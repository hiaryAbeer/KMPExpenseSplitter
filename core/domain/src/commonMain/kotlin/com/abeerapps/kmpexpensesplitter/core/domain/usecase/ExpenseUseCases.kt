package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitCalculator
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitError
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitInput
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Everything the user entered on "Add Expense", before validation. */
data class ExpenseDraft(
    /** Required: every expense belongs to a group. */
    val groupId: String?,
    val title: String,
    val amount: Money,
    val category: ExpenseCategory,
    val paidBy: String?,
    val split: SplitInput,
    val date: LocalDate,
    val note: String,
    /** Who may appear in [paidBy] and [split]: the selected group's members. */
    val participants: List<Participant>,
)

/** A rule the draft breaks. The UI decides how to show each one. */
sealed interface ExpenseDraftError {
    data object NoGroup : ExpenseDraftError
    data object BlankTitle : ExpenseDraftError
    data object TitleTooLong : ExpenseDraftError
    data object NonPositiveAmount : ExpenseDraftError
    data object NoPayer : ExpenseDraftError
    data object NoteTooLong : ExpenseDraftError
    data object UnknownParticipant : ExpenseDraftError
    data class InvalidSplit(val reason: SplitError) : ExpenseDraftError
}

sealed interface AddExpenseResult {
    data class Success(val expense: Expense) : AddExpenseResult
    data class Invalid(val errors: List<ExpenseDraftError>) : AddExpenseResult
    data object StorageError : AddExpenseResult
}

/**
 * Validates an [ExpenseDraft], computes the shares and stores the expense.
 *
 * [validate] is exposed so the UI can enable "Save" with exactly the same rules
 * that [invoke] enforces.
 */
class AddExpenseUseCase(
    private val repository: ExpenseRepository,
    private val newId: () -> String = ::randomId,
) {

    /** @return every broken rule; empty means [invoke] will accept the draft. */
    fun validate(draft: ExpenseDraft): List<ExpenseDraftError> = buildList {
        if (draft.groupId == null) add(ExpenseDraftError.NoGroup)
        val title = draft.title.trim()
        if (title.isEmpty()) add(ExpenseDraftError.BlankTitle)
        if (title.length > Expense.MAX_TITLE_LENGTH) add(ExpenseDraftError.TitleTooLong)
        if (draft.note.trim().length > Expense.MAX_NOTE_LENGTH) add(ExpenseDraftError.NoteTooLong)

        val knownIds = draft.participants.mapTo(HashSet()) { it.id }
        if (draft.paidBy == null) add(ExpenseDraftError.NoPayer)
        else if (draft.paidBy !in knownIds) add(ExpenseDraftError.UnknownParticipant)
        if (draft.split.participantIds().any { it !in knownIds }) add(ExpenseDraftError.UnknownParticipant)

        if (!draft.amount.isPositive) {
            add(ExpenseDraftError.NonPositiveAmount)
        } else {
            val result = SplitCalculator.split(draft.amount, draft.split)
            if (result is SplitCalculator.Result.Failure) add(ExpenseDraftError.InvalidSplit(result.error))
        }
    }.distinct()

    suspend operator fun invoke(draft: ExpenseDraft): AddExpenseResult {
        val errors = validate(draft)
        if (errors.isNotEmpty()) return AddExpenseResult.Invalid(errors)

        val shares: List<ExpenseShare> =
            (SplitCalculator.split(draft.amount, draft.split) as SplitCalculator.Result.Success).shares
        val expense = Expense(
            id = newId(),
            groupId = requireNotNull(draft.groupId),
            title = draft.title.trim(),
            amount = draft.amount,
            category = draft.category,
            paidBy = requireNotNull(draft.paidBy),
            splitType = draft.split.type,
            shares = shares,
            date = draft.date,
            note = draft.note.trim().ifEmpty { null },
        )
        return try {
            repository.addExpense(expense)
            AddExpenseResult.Success(expense)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            AddExpenseResult.StorageError
        }
    }

    private fun SplitInput.participantIds(): Collection<String> = when (this) {
        is SplitInput.Equal -> participantIds
        is SplitInput.Percentage -> percentages.keys
        is SplitInput.Exact -> amounts.keys
    }

    private companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun randomId(): String = Uuid.random().toString()
    }
}
