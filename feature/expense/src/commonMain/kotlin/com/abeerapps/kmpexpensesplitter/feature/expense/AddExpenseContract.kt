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
import com.abeerapps.kmpexpensesplitter.core.ui.PeopleGroupForm
import kotlinx.datetime.LocalDate

/** Bottom sheets / dialogs the screen can show. */
enum class AddExpenseSheet { ExpenseGroup, CreateExpenseGroup, Date, PeopleGroup, CreateGroup, PaidBy }

/** One-off messages shown in a snackbar. */
enum class AddExpenseMessage { SaveFailed, CreateGroupFailed }

/**
 * The "Add expense group" form: just a name.
 */
@Immutable
data class NewExpenseGroupForm(
    val name: String = "",
    val canCreate: Boolean = false,
    val isCreating: Boolean = false,
)


/**
 * Everything "Add Expense" renders. Text fields hold exactly what the user typed;
 * parsing and validation happen in the ViewModel via the domain use cases.
 *
 * @property expenseGroupId the selected expense group (trip); required to save.
 * @property expenseGroupQuery search text in the expense group sheet.
 * @property startDate first day of the expense; [endDate] equals it for a one-day expense.
 * @property you the device owner; the payer while no people group is selected.
 * @property groupId the selected group (required to save); `null` = "No group".
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
    val newExpenseGroup: NewExpenseGroupForm = NewExpenseGroupForm(),
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
    /** The "Add group" form inside the people group sheet (shared with "Manage people groups"). */
    val newGroup: PeopleGroupForm = PeopleGroupForm(),
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
    data object AddExpenseGroupClicked : AddExpenseAction
    data class NewExpenseGroupNameChanged(val value: String) : AddExpenseAction
    data object CreateExpenseGroupClicked : AddExpenseAction
    data object GroupClicked : AddExpenseAction
    data class GroupSelected(val groupId: String) : AddExpenseAction
    data object AddGroupClicked : AddExpenseAction
    data class NewGroupNameChanged(val value: String) : AddExpenseAction
    data class MemberInputChanged(val value: String) : AddExpenseAction
    data object AddMemberClicked : AddExpenseAction
    data class MemberRemoved(val index: Int) : AddExpenseAction
    data object YouRemoved : AddExpenseAction
    data object CreateGroupClicked : AddExpenseAction
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
}
