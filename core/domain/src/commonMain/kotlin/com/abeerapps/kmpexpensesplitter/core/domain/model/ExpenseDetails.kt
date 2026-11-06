package com.abeerapps.kmpexpensesplitter.core.domain.model

/** One person's part of an expense, with their display name. */
data class PersonShare(val participantId: String, val name: String, val amount: Money)

/**
 * An expense with names resolved from its people group.
 *
 * @property shares in the people group's member order.
 */
data class ExpenseDetails(
    val expense: Expense,
    val payerName: String,
    val shares: List<PersonShare>,
) {
    companion object {
        /** @param peopleGroup the expense's people group (names fall back to "?" if it's gone). */
        fun build(expense: Expense, peopleGroup: PeopleGroup?): ExpenseDetails {
            val members = peopleGroup?.members.orEmpty()
            fun nameOf(id: String) = members.firstOrNull { it.id == id }?.name ?: "?"
            val order = members.mapIndexed { index, member -> member.id to index }.toMap()
            return ExpenseDetails(
                expense = expense,
                payerName = nameOf(expense.paidBy),
                shares = expense.shares
                    .sortedBy { order[it.participantId] ?: Int.MAX_VALUE }
                    .map { PersonShare(it.participantId, nameOf(it.participantId), it.amount) },
            )
        }
    }
}
