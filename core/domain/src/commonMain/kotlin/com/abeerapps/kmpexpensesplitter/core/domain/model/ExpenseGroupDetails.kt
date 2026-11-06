package com.abeerapps.kmpexpensesplitter.core.domain.model

/** An expense with its payer's display name, for lists. */
data class ExpenseItem(val expense: Expense, val payerName: String)

/**
 * A person's position in an expense group: what they paid minus their shares.
 * Positive = gets money back, negative = owes, zero = settled.
 */
data class PersonBalance(val participantId: String, val name: String, val balance: Money)

/** Spending in one category; [share] is its part of the total (0.0–1.0). */
data class CategoryTotal(val category: ExpenseCategory, val total: Money, val share: Double)

/**
 * Everything the Group Details screen shows for one expense group, in the user's
 * default currency (expenses in other currencies are left out, like on Home).
 *
 * @property expenses newest first (by start date).
 * @property balances people with the largest amount to get back first.
 * @property categoryTotals categories with spending, largest first.
 */
data class ExpenseGroupDetails(
    val group: ExpenseGroup,
    val memberNames: List<String>,
    val totalSpent: Money,
    val expenses: List<ExpenseItem>,
    val balances: List<PersonBalance>,
    val categoryTotals: List<CategoryTotal>,
) {
    companion object {
        /** Pure builder, so the numbers are unit-tested without any UI. */
        fun build(
            group: ExpenseGroup,
            peopleGroups: List<PeopleGroup>,
            allExpenses: List<Expense>,
            currency: CurrencyCode,
        ): ExpenseGroupDetails {
            val expenses = allExpenses
                .filter { it.expenseGroupId == group.id && it.amount.currency == currency }
                .sortedByDescending { it.startDate } // Stable: same-day keeps newest-added first.
            val peopleById = peopleGroups.associateBy { it.id }

            fun nameOf(expense: Expense, participantId: String): String =
                peopleById[expense.peopleGroupId]?.members?.firstOrNull { it.id == participantId }?.name ?: "?"

            // Everyone involved, each once, in order of first appearance (oldest expense first).
            val names = LinkedHashMap<String, String>()
            expenses.asReversed().forEach { expense ->
                peopleById[expense.peopleGroupId]?.members?.forEach { if (it.id !in names) names[it.id] = it.name }
            }

            val net = HashMap<String, Long>()
            expenses.forEach { expense ->
                net[expense.paidBy] = (net[expense.paidBy] ?: 0L) + expense.amount.minorUnits
                expense.shares.forEach { share ->
                    net[share.participantId] = (net[share.participantId] ?: 0L) - share.amount.minorUnits
                }
            }

            val total = expenses.sumOf { it.amount.minorUnits }
            return ExpenseGroupDetails(
                group = group,
                memberNames = names.values.toList(),
                totalSpent = Money(total, currency),
                expenses = expenses.map { ExpenseItem(it, nameOf(it, it.paidBy)) },
                balances = names.map { (id, name) -> PersonBalance(id, name, Money(net[id] ?: 0L, currency)) }
                    .sortedByDescending { it.balance.minorUnits },
                categoryTotals = expenses.groupBy { it.category }
                    .map { (category, items) ->
                        val sum = items.sumOf { it.amount.minorUnits }
                        CategoryTotal(category, Money(sum, currency), if (total == 0L) 0.0 else sum.toDouble() / total)
                    }
                    .sortedByDescending { it.total.minorUnits },
            )
        }
    }
}
