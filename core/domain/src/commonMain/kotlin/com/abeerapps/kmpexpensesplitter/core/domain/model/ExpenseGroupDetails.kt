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

/** What one person paid for the group's expenses ("Spending by Member"). */
data class MemberSpending(val participantId: String, val name: String, val paid: Money)

/**
 * Everything the Group Details screen shows for one expense group, in the user's
 * default currency (expenses in other currencies are left out, like on Home).
 *
 * @property expenses newest first (by start date).
 * @property balances people with the largest amount to get back first; recorded
 *   [Settlement]s are included, so everyone is at zero once the group is settled.
 * @property transfers payments that would settle the group; empty = [isSettled].
 * @property categoryTotals categories with spending, largest first.
 * @property memberSpending everyone involved, in [memberNames] order, with what they paid.
 * @property averagePerPerson [totalSpent] divided by the number of people involved.
 */
data class ExpenseGroupDetails(
    val group: ExpenseGroup,
    val memberNames: List<String>,
    val totalSpent: Money,
    val expenses: List<ExpenseItem>,
    val balances: List<PersonBalance>,
    val transfers: List<Transfer>,
    val categoryTotals: List<CategoryTotal>,
    val memberSpending: List<MemberSpending>,
    val averagePerPerson: Money,
) {
    /** Nobody owes anything (also true before the first expense). */
    val isSettled: Boolean get() = transfers.isEmpty()

    companion object {
        /** Pure builder, so the numbers are unit-tested without any UI. */
        fun build(
            group: ExpenseGroup,
            peopleGroups: List<PeopleGroup>,
            allExpenses: List<Expense>,
            currency: CurrencyCode,
            allSettlements: List<Settlement> = emptyList(),
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
            fun add(id: String, units: Long) {
                net[id] = (net[id] ?: 0L) + units
            }
            val paid = HashMap<String, Long>()
            expenses.forEach { expense ->
                add(expense.paidBy, expense.amount.minorUnits)
                paid[expense.paidBy] = (paid[expense.paidBy] ?: 0L) + expense.amount.minorUnits
                expense.shares.forEach { share -> add(share.participantId, -share.amount.minorUnits) }
            }
            allSettlements
                .filter { it.expenseGroupId == group.id && it.amount.currency == currency }
                .forEach { settlement ->
                    add(settlement.fromId, settlement.amount.minorUnits)
                    add(settlement.toId, -settlement.amount.minorUnits)
                }

            val total = expenses.sumOf { it.amount.minorUnits }
            val balances = names.map { (id, name) -> PersonBalance(id, name, Money(net[id] ?: 0L, currency)) }
                .sortedByDescending { it.balance.minorUnits }
            return ExpenseGroupDetails(
                group = group,
                memberNames = names.values.toList(),
                totalSpent = Money(total, currency),
                expenses = expenses.map { ExpenseItem(it, nameOf(it, it.paidBy)) },
                balances = balances,
                transfers = SettleUpPlanner.plan(balances),
                memberSpending = names.map { (id, name) -> MemberSpending(id, name, Money(paid[id] ?: 0L, currency)) },
                // Rounded half up to the cent: 1250.50 / 4 = 312.625 → 312.63.
                averagePerPerson = Money(if (names.isEmpty()) 0L else (total + names.size / 2) / names.size, currency),
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
