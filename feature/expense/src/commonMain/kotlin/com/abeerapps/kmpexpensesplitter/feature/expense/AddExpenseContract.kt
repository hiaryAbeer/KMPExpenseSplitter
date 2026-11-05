package com.abeerapps.kmpexpensesplitter.feature.expense

import androidx.compose.runtime.Immutable
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.Group
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitError
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ExpenseDraftError
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.MemberNameError
import kotlinx.datetime.LocalDate

/** Bottom sheets / dialogs the screen can show. */
enum class AddExpenseSheet { Group, CreateGroup, PaidBy, Date }

/** One-off messages shown in a snackbar. */
enum class AddExpenseMessage { SaveFailed, CreateGroupFailed }

/**
 * The "Add group" form inside the group sheet.
 *
 * @property includeYou whether you are in the group (on by default; removable like anyone else).
 * @property members other people's names.
 * @property memberInput the name being typed, added with the "+" button.
 */
@Immutable
data class NewGroupForm(
    val name: String = "",
    val includeYou: Boolean = true,
    val memberInput: String = "",
    val members: List<String> = emptyList(),
    val memberError: MemberNameError? = null,
    val canCreate: Boolean = false,
    val isCreating: Boolean = false,
)

/**
 * Everything "Add Expense" renders. Text fields hold exactly what the user typed;
 * parsing and validation happen in the ViewModel via the domain use cases.
 *
 * @property you the device owner; the payer while no group is selected.
 * @property groupId the selected group (required to save); `null` = "No group".
 * @property selected ids of the group members ticked under "Split Between".
 * @property shareInputs typed percentage/amount per member, kept per split type
 *   so switching between Percentage and Exact doesn't lose either.
 * @property errors broken rules from `AddExpenseUseCase.validate`; empty = can save.
 */
@Immutable
data class AddExpenseUiState(
    val date: LocalDate,
    val today: LocalDate,
    val isLoading: Boolean = true,
    val currency: CurrencyCode = SupportedCurrencies.default.code,
    val you: Participant? = null,
    val groups: List<Group> = emptyList(),
    val groupId: String? = null,
    val title: String = "",
    val amount: String = "",
    val category: ExpenseCategory = ExpenseCategory.Food,
    val paidBy: String? = null,
    val splitType: SplitType = SplitType.Equal,
    val selected: Set<String> = emptySet(),
    val shareInputs: Map<SplitType, Map<String, String>> = emptyMap(),
    val note: String = "",
    val newGroup: NewGroupForm = NewGroupForm(),
    val openSheet: AddExpenseSheet? = null,
    val isSaving: Boolean = false,
    val errors: List<ExpenseDraftError> = emptyList(),
    val message: AddExpenseMessage? = null,
) {
    val group: Group? get() = groups.firstOrNull { it.id == groupId }

    /** People listed under "Split Between": the selected group's members (none without a group). */
    val participants: List<Participant> get() = group?.members.orEmpty()

    /** Who can be picked as payer: the group's members, or just you before a group is chosen. */
    val payerOptions: List<Participant> get() = group?.members ?: listOfNotNull(you)

    val payer: Participant? get() = payerOptions.firstOrNull { it.id == paidBy }

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
    data class DateSelected(val date: LocalDate) : AddExpenseAction
    data class NoteChanged(val value: String) : AddExpenseAction
    data object SheetDismissed : AddExpenseAction
    data object SaveClicked : AddExpenseAction
    data object MessageShown : AddExpenseAction
}

sealed interface AddExpenseEvent {
    /** The expense was stored: leave the screen. */
    data object Saved : AddExpenseEvent
}
