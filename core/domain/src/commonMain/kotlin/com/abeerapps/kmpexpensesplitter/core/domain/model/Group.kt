package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlinx.datetime.LocalDate

/**
 * A set of people who share expenses.
 *
 * @property members everyone in the group. The device owner, when in it, has id
 *   [Participant.YOU_ID]; their stored name is replaced by the current profile
 *   name when read (see `ObserveGroupListUseCase`).
 * @property createdOn used to order groups that have no expenses yet.
 */
data class Group(
    val id: String,
    val name: String,
    val members: List<Participant>,
    val createdOn: LocalDate,
) {
    companion object {
        const val MAX_NAME_LENGTH = 50
        const val MAX_MEMBER_NAME_LENGTH = 50

        /** Other people you can add (you are added automatically). */
        const val MAX_OTHER_MEMBERS = 20
    }
}

/**
 * Builds the Home/Settings view of groups from groups and their expenses.
 * Pure function: easy to test and the single place balances are derived.
 */
object GroupSummaries {

    /**
     * @param currency only expenses in this currency are counted, so totals never mix
     *   currencies (until currency conversion exists).
     * @return summaries, most recently active first (latest expense date, else creation date).
     */
    fun build(groups: List<Group>, expenses: List<Expense>, currency: CurrencyCode): List<GroupSummary> {
        val byGroup = expenses.filter { it.amount.currency == currency }.groupBy { it.groupId }
        return groups
            .map { group ->
                val groupExpenses = byGroup[group.id].orEmpty()
                val lastActivity = maxOf(group.createdOn, groupExpenses.maxOfOrNull { it.date } ?: group.createdOn)
                lastActivity to GroupSummary(
                    id = group.id,
                    name = group.name,
                    memberNames = group.members.map { it.name },
                    totalSpent = Money(groupExpenses.sumOf { it.amount.minorUnits }, currency),
                    yourBalance = Money(groupExpenses.sumOf { it.yourBalanceChange() }, currency),
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
}
