package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseDetails
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitCalculator
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitError
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitInput
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Everything the user entered on "Add Expense", before validation. */
data class ExpenseDraft(
    /** Required: the expense group (trip) it belongs to. */
    val expenseGroupId: String?,
    /** Required: the people group it is split within. */
    val peopleGroupId: String?,
    val title: String,
    val amount: Money,
    val category: ExpenseCategory,
    val paidBy: String?,
    val split: SplitInput,
    val startDate: LocalDate,
    /** Equal to [startDate] for a one-day expense; never before it. */
    val endDate: LocalDate,
    val note: String,
    /** Who may appear in [paidBy] and [split]: the selected people group's members. */
    val participants: List<Participant>,
)

/** A rule the draft breaks. The UI decides how to show each one. */
sealed interface ExpenseDraftError {
    data object NoExpenseGroup : ExpenseDraftError
    data object NoPeopleGroup : ExpenseDraftError
    data object EndBeforeStart : ExpenseDraftError
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
 * Validates an [ExpenseDraft], computes the shares and stores the expense: a new
 * one, or an existing one when editing (same id, so it stays in its place).
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
        if (draft.expenseGroupId == null) add(ExpenseDraftError.NoExpenseGroup)
        if (draft.peopleGroupId == null) add(ExpenseDraftError.NoPeopleGroup)
        if (draft.endDate < draft.startDate) add(ExpenseDraftError.EndBeforeStart)
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

    /** @param editingId the expense being edited, or `null` to add a new one. */
    suspend operator fun invoke(draft: ExpenseDraft, editingId: String? = null): AddExpenseResult {
        val errors = validate(draft)
        if (errors.isNotEmpty()) return AddExpenseResult.Invalid(errors)

        val shares: List<ExpenseShare> =
            (SplitCalculator.split(draft.amount, draft.split) as SplitCalculator.Result.Success).shares
        val expense = Expense(
            id = editingId ?: newId(),
            expenseGroupId = requireNotNull(draft.expenseGroupId),
            peopleGroupId = requireNotNull(draft.peopleGroupId),
            title = draft.title.trim(),
            amount = draft.amount,
            category = draft.category,
            paidBy = requireNotNull(draft.paidBy),
            splitType = draft.split.type,
            shares = shares,
            startDate = draft.startDate,
            endDate = draft.endDate,
            note = draft.note.trim().ifEmpty { null },
        )
        return try {
            if (editingId == null) repository.addExpense(expense) else repository.updateExpense(expense)
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

/** Deletes one expense (the UI asks for confirmation first). */
class DeleteExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(expenseId: String): DeleteResult = try {
        repository.deleteExpense(expenseId)
        DeleteResult.Deleted
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        DeleteResult.StorageError
    }
}

/**
 * Emits one expense with the names of its payer and of everyone sharing it, or
 * `null` once it no longer exists (e.g. it was deleted).
 */
class ObserveExpenseDetailsUseCase(
    private val expenses: ExpenseRepository,
    private val peopleGroups: PeopleGroupRepository,
    private val profiles: UserProfileRepository,
) {
    operator fun invoke(expenseId: String): Flow<ExpenseDetails?> =
        combine(expenses.observeExpenses(), peopleGroups.observeGroups(), profiles.observeProfile()) { all, groups, profile ->
            all.firstOrNull { it.id == expenseId }?.let { expense ->
                ExpenseDetails.build(expense, groups.withYourName(profile?.name).firstOrNull { it.id == expense.peopleGroupId })
            }
        }
}
