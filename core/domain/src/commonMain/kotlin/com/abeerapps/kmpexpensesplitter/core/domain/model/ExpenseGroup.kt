package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlinx.datetime.LocalDate

/**
 * A collection of related expenses, e.g. one trip ("Aqaba Trip"). This is what
 * Home lists under "Recent Groups". Each expense in it can be split within a
 * different [PeopleGroup].
 *
 * @property id unique, generated identity; never shown to the user (names may repeat).
 * @property createdOn orders expense groups that have no expenses yet.
 */
data class ExpenseGroup(
    val id: String,
    val name: String,
    val createdOn: LocalDate,
) {
    companion object {
        const val MAX_NAME_LENGTH = 50
    }
}

/**
 * Builds the Home/Settings view of expense groups from their expenses.
 * Pure function: easy to test and the single place balances are derived.
 */
object GroupSummaries {

    /**
     * @param peopleGroups used to name the people involved in each expense group.
     * @param currency only expenses in this currency are counted, so totals never mix
     *   currencies (until currency conversion exists).
     * @param settlements recorded payments; they move your balance like expenses do.
     * @return summaries, most recently active first (latest expense end date, else creation date).
     */
    fun build(
        expenseGroups: List<ExpenseGroup>,
        peopleGroups: List<PeopleGroup>,
        expenses: List<Expense>,
        currency: CurrencyCode,
        settlements: List<Settlement> = emptyList(),
    ): List<GroupSummary> {
        val peopleById = peopleGroups.associateBy { it.id }
        val byGroup = expenses.filter { it.amount.currency == currency }.groupBy { it.expenseGroupId }
        val settledByGroup = settlements.filter { it.amount.currency == currency }.groupBy { it.expenseGroupId }
        return expenseGroups
            .map { group ->
                val groupExpenses = byGroup[group.id].orEmpty()
                val lastActivity = maxOf(group.createdOn, groupExpenses.maxOfOrNull { it.endDate } ?: group.createdOn)
                lastActivity to GroupSummary(
                    id = group.id,
                    name = group.name,
                    // Everyone in the people groups used by its expenses, each once, oldest expense first.
                    memberNames = groupExpenses.asReversed()
                        .flatMap { peopleById[it.peopleGroupId]?.members.orEmpty() }
                        .distinctBy { it.id }
                        .map { it.name },
                    totalSpent = Money(groupExpenses.sumOf { it.amount.minorUnits }, currency),
                    yourBalance = Money(
                        groupExpenses.sumOf { it.yourBalanceChange() } +
                            settledByGroup[group.id].orEmpty().sumOf { it.yourBalanceChange() },
                        currency,
                    ),
                    lastActivity = lastActivity,
                )
            }
            .sortedByDescending { it.first } // Stable: same-day groups keep repository order.
            .map { it.second }
    }

    /** What you paid minus your own share: positive = others owe you. */
    private fun Expense.yourBalanceChange(): Long {
        val paid = if (paidBy == Participant.YOU_ID) amount.minorUnits else 0L
        val owed = shares.filter { it.participantId == Participant.YOU_ID }.sumOf { it.amount.minorUnits }
        return paid - owed
    }

    /** Paying moves your balance up (you owe less); receiving moves it down. */
    private fun Settlement.yourBalanceChange(): Long = when (Participant.YOU_ID) {
        fromId -> amount.minorUnits
        toId -> -amount.minorUnits
        else -> 0L
    }
}
