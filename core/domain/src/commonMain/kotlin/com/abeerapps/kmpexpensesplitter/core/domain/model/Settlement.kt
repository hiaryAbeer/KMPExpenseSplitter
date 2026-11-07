package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlinx.datetime.LocalDate

/**
 * A recorded payment that settles debt inside an expense group: [fromId] paid
 * [toId] [amount]. It moves balances like an expense does: the payer's balance
 * goes up by [amount] and the receiver's goes down by it.
 */
data class Settlement(
    val id: String,
    val expenseGroupId: String,
    val fromId: String,
    val toId: String,
    val amount: Money,
    val date: LocalDate,
)

/** A payment still needed to settle up: [fromName] pays [toName] [amount]. */
data class Transfer(
    val fromId: String,
    val fromName: String,
    val toId: String,
    val toName: String,
    val amount: Money,
)

/** Business rule: which payments clear every balance in an expense group. */
object SettleUpPlanner {

    /**
     * Greedy plan that matches the largest debt with the largest credit until all
     * balances are zero. It needs at most `people - 1` payments, and the result
     * is the same for the same balances.
     *
     * @param balances must sum to zero, as [ExpenseGroupDetails.balances] always do.
     */
    fun plan(balances: List<PersonBalance>): List<Transfer> {
        val currency = balances.firstOrNull()?.balance?.currency ?: return emptyList()
        // Index keeps ties in the given order, so the plan is stable.
        val creditors = balances.withIndex().filter { it.value.balance.isPositive }
            .map { Due(it.value, it.value.balance.minorUnits, it.index) }.toMutableList()
        val debtors = balances.withIndex().filter { it.value.balance.isNegative }
            .map { Due(it.value, -it.value.balance.minorUnits, it.index) }.toMutableList()

        val transfers = mutableListOf<Transfer>()
        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val creditor = creditors.largest()
            val debtor = debtors.largest()
            val amount = minOf(creditor.left, debtor.left)
            transfers += Transfer(
                fromId = debtor.person.participantId,
                fromName = debtor.person.name,
                toId = creditor.person.participantId,
                toName = creditor.person.name,
                amount = Money(amount, currency),
            )
            creditor.left -= amount
            debtor.left -= amount
            if (creditor.left == 0L) creditors.remove(creditor)
            if (debtor.left == 0L) debtors.remove(debtor)
        }
        return transfers
    }

    private class Due(val person: PersonBalance, var left: Long, val order: Int)

    private fun List<Due>.largest(): Due =
        maxWith(compareBy<Due> { it.left }.thenByDescending { it.order })
}
