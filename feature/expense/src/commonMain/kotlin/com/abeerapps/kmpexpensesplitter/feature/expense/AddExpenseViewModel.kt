package com.abeerapps.kmpexpensesplitter.feature.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.DecimalInput
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitInput
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.AddExpenseResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.AddExpenseUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ExpenseDraft
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpensesUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObservePeopleGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presentation logic for "Add Expense", including picking the expense group and people group.
 * Groups are managed on the Manage screens; the form follows their changes live.
 * Shared by Android and iOS.
 *
 * The whole form is one [MutableStateFlow] updated synchronously on each action,
 * so text fields never lag behind typing. After every change the form is
 * re-validated with [AddExpenseUseCase.validate] (the same rules used on save),
 * which drives the Save button and the split hint.
 */
class AddExpenseViewModel(
    observeExpenseGroups: ObserveExpenseGroupsUseCase,
    observeGroupList: ObservePeopleGroupsUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    private val addExpense: AddExpenseUseCase,
    private val observeExpenses: ObserveExpensesUseCase,
    dateProvider: DateProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Set when opened from a group's details: that expense group is preselected. */
    private val requestedExpenseGroupId: String? = savedStateHandle[ARG_EXPENSE_GROUP_ID]

    /** Set when opened from "Edit" on Expense Details: the form is filled with that expense. */
    private val editExpenseId: String? = savedStateHandle[ARG_EXPENSE_ID]

    private val _uiState = MutableStateFlow(
        dateProvider.today().let { today -> AddExpenseUiState(startDate = today, endDate = today, today = today) },
    )
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddExpenseEvent>(Channel.BUFFERED)
    val events: Flow<AddExpenseEvent> = _events.receiveAsFlow()

    init {
        editExpenseId?.let(::prefillFrom)
        viewModelScope.launch {
            observeExpenseGroups().collect { trips ->
                updateForm { state ->
                    val firstLoad = !state.expenseGroupsLoaded
                    val next = state.copy(expenseGroups = trips, expenseGroupsLoaded = true)
                    when {
                        // Preselect the requested expense group, else the first (newest), like people groups.
                        firstLoad && next.expenseGroupId == null -> next.copy(
                            expenseGroupId = trips.firstOrNull { it.id == requestedExpenseGroupId }?.id ?: trips.firstOrNull()?.id,
                        )
                        // The selected one was deleted (Manage screen, data cleared): fall back to the newest.
                        next.expenseGroupId != null && next.expenseGroup == null -> next.copy(expenseGroupId = trips.firstOrNull()?.id)
                        // None yet, and one was just added on the Manage screen: select it.
                        next.expenseGroupId == null && trips.isNotEmpty() -> next.copy(expenseGroupId = trips.first().id)
                        else -> next
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(observeGroupList(), observeUserProfile()) { groups, profile -> groups to profile }
                .collect { (groups, profile) ->
                    updateForm { state ->
                        val next = state.copy(
                            isLoading = false,
                            currency = profile?.defaultCurrency ?: SupportedCurrencies.default.code,
                            you = profile?.let { Participant(Participant.YOU_ID, it.name) },
                            groups = groups,
                        )
                        val updated = next.group
                        when {
                            // No group yet (first load, or one was just added on the Manage screen): pick the newest.
                            next.groupId == null && groups.isNotEmpty() -> next.withGroup(groups.first())
                            // The selected group was deleted (Manage screen, data cleared): fall back to the newest.
                            next.groupId != null && updated == null -> next.withGroup(groups.firstOrNull())
                            // Its members were edited on the Manage screen: everyone in it shares again.
                            updated != null && updated.members != state.group?.members -> next.withGroup(updated)
                            else -> next.withValidPayer()
                        }
                    }
                }
        }
    }

    fun onAction(action: AddExpenseAction) {
        when (action) {
            is AddExpenseAction.TitleChanged ->
                if (action.value.length <= Expense.MAX_TITLE_LENGTH) updateForm { it.copy(title = action.value) }
            AddExpenseAction.ExpenseGroupClicked ->
                _uiState.update { it.copy(openSheet = AddExpenseSheet.ExpenseGroup, expenseGroupQuery = "") }
            is AddExpenseAction.ExpenseGroupQueryChanged -> _uiState.update { it.copy(expenseGroupQuery = action.value) }
            is AddExpenseAction.ExpenseGroupSelected -> updateForm { state ->
                val exists = state.expenseGroups.any { it.id == action.expenseGroupId }
                state.copy(expenseGroupId = if (exists) action.expenseGroupId else state.expenseGroupId, openSheet = null)
            }
            AddExpenseAction.ManageExpenseGroupsClicked -> openManageScreen(AddExpenseEvent.OpenManageExpenseGroups)
            AddExpenseAction.GroupClicked -> _uiState.update { it.copy(openSheet = AddExpenseSheet.PeopleGroup) }
            is AddExpenseAction.GroupSelected -> updateForm { state ->
                val group = state.groups.firstOrNull { it.id == action.groupId }
                (if (group == null || group.id == state.groupId) state else state.withGroup(group)).copy(openSheet = null)
            }
            AddExpenseAction.ManagePeopleGroupsClicked -> openManageScreen(AddExpenseEvent.OpenManagePeopleGroups)
            is AddExpenseAction.AmountChanged ->
                DecimalInput.sanitize(action.value)?.let { amount -> updateForm { it.copy(amount = amount) } }
            is AddExpenseAction.CategorySelected -> updateForm { it.copy(category = action.category) }
            AddExpenseAction.PaidByClicked -> _uiState.update { it.copy(openSheet = AddExpenseSheet.PaidBy) }
            is AddExpenseAction.PayerSelected ->
                updateForm { it.copy(paidBy = action.participantId, openSheet = null) }
            is AddExpenseAction.SplitTypeSelected -> updateForm { it.copy(splitType = action.type) }
            is AddExpenseAction.ParticipantToggled -> updateForm { state ->
                val id = action.participantId
                if (state.group == null) state // "Split Between" is disabled without a group.
                else state.copy(selected = if (id in state.selected) state.selected - id else state.selected + id)
            }
            is AddExpenseAction.ShareChanged -> onShareChanged(action.participantId, action.value)
            AddExpenseAction.DateClicked -> _uiState.update { it.copy(openSheet = AddExpenseSheet.Date) }
            is AddExpenseAction.DateRangeSelected -> updateForm { state ->
                // One tap = a one-day expense. The picker blocks future dates; clamp anyway
                // so the rule lives here, and keep the range ordered.
                val start = minOf(action.start, state.today)
                val end = minOf(action.end ?: start, state.today).coerceAtLeast(start)
                state.copy(startDate = start, endDate = end, openSheet = null)
            }
            is AddExpenseAction.NoteChanged ->
                if (action.value.length <= Expense.MAX_NOTE_LENGTH) updateForm { it.copy(note = action.value) }
            AddExpenseAction.SheetDismissed -> _uiState.update { it.copy(openSheet = null) }
            AddExpenseAction.SaveClicked -> save()
            AddExpenseAction.MessageShown -> _uiState.update { it.copy(message = null) }
        }
    }

    /** Closes the sheet and leaves for a Manage screen; the form keeps everything typed so far. */
    private fun openManageScreen(event: AddExpenseEvent) {
        _uiState.update { it.copy(openSheet = null) }
        viewModelScope.launch { _events.send(event) }
    }

    private fun onShareChanged(participantId: String, value: String) {
        val type = _uiState.value.splitType
        if (type == SplitType.Equal) return
        val clean = DecimalInput.sanitize(value) ?: return
        updateForm { state ->
            val forType = state.shareInputs[type].orEmpty() + (participantId to clean)
            state.copy(shareInputs = state.shareInputs + (type to forType))
        }
    }

    /**
     * Edit mode: once groups are loaded (so the first-load defaults have run), fills
     * every field from the stored expense. Percentages aren't stored, so they are
     * derived from the shares and adjusted to total exactly 100%.
     */
    private fun prefillFrom(expenseId: String) {
        viewModelScope.launch {
            _uiState.first { !it.isLoading && it.expenseGroupsLoaded }
            val expense = observeExpenses().first().firstOrNull { it.id == expenseId } ?: return@launch
            updateForm { state ->
                val group = state.groups.firstOrNull { it.id == expense.peopleGroupId }
                val withGroup = state.withGroup(group)
                val shareIds = expense.shares.mapTo(LinkedHashSet()) { it.participantId }
                val inputs: Map<String, String> = when (expense.splitType) {
                    SplitType.Equal -> emptyMap()
                    SplitType.Exact -> expense.shares.associate { it.participantId to it.amount.minorUnits.toInput() }
                    SplitType.Percentage -> percentagesOf(expense).mapValues { (_, hundredths) -> hundredths.toInput() }
                }
                withGroup.copy(
                    editingId = expense.id,
                    title = expense.title,
                    amount = expense.amount.minorUnits.toInput(),
                    category = expense.category,
                    expenseGroupId = expense.expenseGroupId,
                    startDate = expense.startDate,
                    endDate = expense.endDate,
                    note = expense.note.orEmpty(),
                    paidBy = expense.paidBy,
                    splitType = expense.splitType,
                    selected = shareIds.filterTo(LinkedHashSet()) { id -> withGroup.participants.any { it.id == id } },
                    shareInputs = if (inputs.isEmpty()) emptyMap() else mapOf(expense.splitType to inputs),
                )
            }
        }
    }

    /** Share of each person in hundredths of a percent, summing to exactly 10_000. */
    private fun percentagesOf(expense: Expense): Map<String, Long> {
        val total = expense.amount.minorUnits
        if (total == 0L) return emptyMap()
        val raw = expense.shares.associate { it.participantId to (it.amount.minorUnits * 10_000 + total / 2) / total }
        val diff = 10_000 - raw.values.sum()
        val last = expense.shares.last().participantId
        return raw + (last to raw.getValue(last) + diff)
    }

    /** Hundredths as the text a user would type: 1250 → "12.50". */
    private fun Long.toInput(): String = "${this / 100}.${(this % 100).toString().padStart(2, '0')}"

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = addExpense(state.toDraft(), editingId = state.editingId)) {
                is AddExpenseResult.Success -> _events.send(AddExpenseEvent.Saved)
                is AddExpenseResult.Invalid -> _uiState.update { it.copy(isSaving = false, errors = result.errors) }
                AddExpenseResult.StorageError ->
                    _uiState.update { it.copy(isSaving = false, message = AddExpenseMessage.SaveFailed) }
            }
        }
    }

    /** Applies [transform] and re-validates, atomically. */
    private inline fun updateForm(crossinline transform: (AddExpenseUiState) -> AddExpenseUiState) {
        _uiState.update { state ->
            val next = transform(state)
            next.copy(errors = if (next.isLoading) emptyList() else addExpense.validate(next.toDraft()))
        }
    }

    /** Switches to [group]: everyone in it shares, you pay (if a member), typed shares reset. */
    private fun AddExpenseUiState.withGroup(group: PeopleGroup?): AddExpenseUiState =
        copy(
            groupId = group?.id,
            groups = if (group == null || groups.any { it.id == group.id }) groups else listOf(group) + groups,
            selected = group?.members?.mapTo(LinkedHashSet()) { it.id }.orEmpty(),
            shareInputs = emptyMap(),
            paidBy = null,
        ).withValidPayer()

    /** Keeps the payer if still possible, else defaults to you (or the first member). */
    private fun AddExpenseUiState.withValidPayer(): AddExpenseUiState {
        val options = payerOptions
        if (options.any { it.id == paidBy }) return this
        val default = options.firstOrNull { it.id == Participant.YOU_ID } ?: options.firstOrNull()
        return copy(paidBy = default?.id)
    }

    private fun AddExpenseUiState.toDraft(): ExpenseDraft {
        // Member order, not tap order, so rounding is stable.
        val sharing = participants.map { it.id }.filter { it in selected }
        fun typed(type: SplitType) =
            sharing.associateWith { id -> DecimalInput.toHundredths(shareInputs[type]?.get(id).orEmpty()) ?: 0L }

        return ExpenseDraft(
            expenseGroupId = expenseGroupId,
            peopleGroupId = groupId,
            title = title,
            amount = Money(DecimalInput.toHundredths(amount) ?: 0L, currency),
            category = category,
            paidBy = paidBy,
            split = when (splitType) {
                SplitType.Equal -> SplitInput.Equal(sharing)
                SplitType.Percentage -> SplitInput.Percentage(typed(SplitType.Percentage))
                SplitType.Exact -> SplitInput.Exact(typed(SplitType.Exact))
            },
            startDate = startDate,
            endDate = endDate,
            note = note,
            participants = payerOptions,
        )
    }

    companion object {
        /** Navigation argument: the expense group to preselect (the route's `expenseGroupId`). */
        const val ARG_EXPENSE_GROUP_ID = "expenseGroupId"

        /** Navigation argument: the expense to edit (the route's `expenseId`). */
        const val ARG_EXPENSE_ID = "expenseId"
    }
}
