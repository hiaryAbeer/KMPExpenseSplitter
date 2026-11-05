package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlinx.datetime.LocalDate

/** What an expense was for. Order = order of the tiles on "Add Expense". */
enum class ExpenseCategory { Food, Transport, Hotel, Bills, Shopping, Entertainment }

/** How an expense's total is divided between the people sharing it. */
enum class SplitType {
    /** Everyone selected pays the same (remainder cents go to the first people). */
    Equal,

    /** Each person gets a percentage; percentages must total 100%. */
    Percentage,

    /** Each person gets an exact amount; amounts must total the expense. */
    Exact,
}

/**
 * Someone who can pay for or share an expense.
 *
 * @property id stable identity used in [Expense.paidBy] and [ExpenseShare]; names can change.
 */
data class Participant(val id: String, val name: String) {
    companion object {
        /** Id of the device owner (the registered profile). */
        const val YOU_ID = "you"
    }
}

/** The part of an expense one participant is responsible for. */
data class ExpenseShare(val participantId: String, val amount: Money)

/**
 * A recorded expense.
 *
 * Invariant (enforced by `AddExpenseUseCase`): [shares] are positive, in
 * [amount]'s currency, and add up to exactly [amount].
 *
 * @property groupId the [Group] the expense belongs to.
 * @property paidBy id of the [Participant] who paid.
 * @property note optional free text; `null` when empty.
 */
data class Expense(
    val id: String,
    val groupId: String,
    val title: String,
    val amount: Money,
    val category: ExpenseCategory,
    val paidBy: String,
    val splitType: SplitType,
    val shares: List<ExpenseShare>,
    val date: LocalDate,
    val note: String?,
) {
    companion object {
        const val MAX_TITLE_LENGTH = 100
        const val MAX_NOTE_LENGTH = 300
    }
}
