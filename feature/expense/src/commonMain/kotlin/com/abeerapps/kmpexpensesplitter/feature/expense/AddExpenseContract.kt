package com.abeerapps.kmpexpensesplitter.feature.expense

import androidx.compose.runtime.Immutable
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitError
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ExpenseDraftError
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseGroupsUseCase
import kotlinx.datetime.LocalDate

/** Bottom sheets / dialogs the screen can show. */
enum class AddExpenseSheet { ExpenseGroup, Date, PeopleGroup, PaidBy }

/** One-off messages shown in a snackbar. */
enum class AddExpenseMessage { SaveFailed }

/**
 * Everything "Add Expense" renders. Text fields hold exactly what the user typed;
 * parsing and validation happen in the ViewModel via the domain use cases.
 *
 * @property expenseGroupId the selected expense group (trip); required to save.
 * @property expenseGroupQuery search text in the expense group sheet.
 * @property startDate first day of the expense; [endDate] equals it for a one-day expense.
 * @property you the device owner; the payer while no people group is selected.
 * @property groupId the selected group (required to save); `null` = "No group".
 *   Groups are created, edited and deleted on the Manage screens; the form follows those changes.
 * @property selected ids of the group members ticked under "Split Between".
 * @property shareInputs typed percentage/amount per member, kept per split type
 *   so switching between Percentage and Exact doesn't lose either.
 * @property errors broken rules from `AddExpenseUseCase.validate`; empty = can save.
 */
@Immutable
data class AddExpenseUiState(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val today: LocalDate,
    val isLoading: Boolean = true,
    /** The expense being edited ("Edit Expense"), or `null` when adding a new one. */
    val editingId: String? = null,
    val currency: CurrencyCode = SupportedCurrencies.default.code,
    val expenseGroups: List<ExpenseGroup> = emptyList(),
    /** Whether expense groups have been read once (drives the first-load preselection). */
    val expenseGroupsLoaded: Boolean = false,
    val expenseGroupId: String? = null,
    val expenseGroupQuery: String = "",
    val you: Participant? = null,
    val groups: List<PeopleGroup> = emptyList(),
    val groupId: String? = null,
    val title: String = "",
    val amount: String = "",
    val category: ExpenseCategory = ExpenseCategory.Food,
    val paidBy: String? = null,
    val splitType: SplitType = SplitType.Equal,
    val selected: Set<String> = emptySet(),
    val shareInputs: Map<SplitType, Map<String, String>> = emptyMap(),
    val note: String = "",
    val openSheet: AddExpenseSheet? = null,
    val isSaving: Boolean = false,
    val errors: List<ExpenseDraftError> = emptyList(),
    val message: AddExpenseMessage? = null,
) {
    val expenseGroup: ExpenseGroup? get() = expenseGroups.firstOrNull { it.id == expenseGroupId }

    /** Expense groups matching the sheet's search text. */
    val filteredExpenseGroups: List<ExpenseGroup>
        get() = ObserveExpenseGroupsUseCase.search(expenseGroups, expenseGroupQuery)

    val group: PeopleGroup? get() = groups.firstOrNull { it.id == groupId }

    /** People listed under "Split Between": the selected group's members (none without a group). */
    val participants: List<Participant> get() = group?.members.orEmpty()

    /** Who can be picked as payer: the group's members, or just you before a group is chosen. */
    val payerOptions: List<Participant> get() = group?.members ?: listOfNotNull(you)

    val payer: Participant? get() = payerOptions.firstOrNull { it.id == paidBy }

    val isEditing: Boolean get() = editingId != null

    val canSave: Boolean get() = !isLoading && !isSaving && errors.isEmpty()

    fun shareInput(participantId: String): String = shareInputs[splitType]?.get(participantId).orEmpty()

    private val splitError: SplitError?
        get() = errors.firstNotNullOfOrNull { (it as? ExpenseDraftError.InvalidSplit)?.reason }

    /**
     * The split hint turns red when the split can't be applied, but only once the
     * user has started on it (typed a share, or unticked everyone), not on a fresh
     * form and not while "Split Between" is disabled (no group).
     */
    val isSplitHintError: Boolean
        get() {
            if (group == null) return false
            val error = splitError ?: return false
            return when (splitType) {
                SplitType.Equal -> error == SplitError.NoParticipants
                else -> shareInputs[splitType].orEmpty().any { (id, text) -> id in selected && text.isNotEmpty() }
            }
        }
}

sealed interface AddExpenseAction {
    data class TitleChanged(val value: String) : AddExpenseAction
    data object ExpenseGroupClicked : AddExpenseAction
    data class ExpenseGroupQueryChanged(val value: String) : AddExpenseAction
    data class ExpenseGroupSelected(val expenseGroupId: String) : AddExpenseAction
    /** "Manage expense groups" at the bottom of the expense group sheet. */
    data object ManageExpenseGroupsClicked : AddExpenseAction
    data object GroupClicked : AddExpenseAction
    data class GroupSelected(val groupId: String) : AddExpenseAction
    /** "Manage people groups" at the bottom of the people group sheet. */
    data object ManagePeopleGroupsClicked : AddExpenseAction
    data class AmountChanged(val value: String) : AddExpenseAction
    data class CategorySelected(val category: ExpenseCategory) : AddExpenseAction
    data object PaidByClicked : AddExpenseAction
    data class PayerSelected(val participantId: String) : AddExpenseAction
    data class SplitTypeSelected(val type: SplitType) : AddExpenseAction
    data class ParticipantToggled(val participantId: String) : AddExpenseAction
    data class ShareChanged(val participantId: String, val value: String) : AddExpenseAction
    data object DateClicked : AddExpenseAction
    /** From the calendar sheet; [end] is `null` after a single tap (a one-day expense). */
    data class DateRangeSelected(val start: LocalDate, val end: LocalDate?) : AddExpenseAction
    data class NoteChanged(val value: String) : AddExpenseAction
    data object SheetDismissed : AddExpenseAction
    data object SaveClicked : AddExpenseAction
    data object MessageShown : AddExpenseAction
}

sealed interface AddExpenseEvent {
    /** The expense was stored: leave the screen. */
    data object Saved : AddExpenseEvent

    /** Open "Manage expense groups" (the same screen as the Groups tab's button). */
    data object OpenManageExpenseGroups : AddExpenseEvent

    /** Open "Manage people groups" (the same screen as the Groups tab's button). */
    data object OpenManagePeopleGroups : AddExpenseEvent
}
