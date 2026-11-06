package com.abeerapps.kmpexpensesplitter.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.DecimalInput
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitInput
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.AddExpenseResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.AddExpenseUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreateExpenseGroupResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreateExpenseGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreatePeopleGroupResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreatePeopleGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ExpenseDraft
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObservePeopleGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveUserProfileUseCase
import com.abeerapps.kmpexpensesplitter.core.ui.PeopleGroupForm
import com.abeerapps.kmpexpensesplitter.core.ui.PeopleGroupFormEditor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presentation logic for "Add Expense", including picking and creating the group.
 * Shared by Android and iOS.
 *
 * The whole form is one [MutableStateFlow] updated synchronously on each action,
 * so text fields never lag behind typing. After every change the form is
 * re-validated with [AddExpenseUseCase.validate] (the same rules used on save),
 * which drives the Save button and the split hint.
 */
class AddExpenseViewModel(
    observeExpenseGroups: ObserveExpenseGroupsUseCase,
    private val createExpenseGroup: CreateExpenseGroupUseCase,
    observeGroupList: ObservePeopleGroupsUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    private val addExpense: AddExpenseUseCase,
    private val createGroup: CreatePeopleGroupUseCase,
    dateProvider: DateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        dateProvider.today().let { today -> AddExpenseUiState(startDate = today, endDate = today, today = today) },
    )
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddExpenseEvent>(Channel.BUFFERED)
    val events: Flow<AddExpenseEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeExpenseGroups().collect { trips ->
                updateForm { state ->
                    val firstLoad = !state.expenseGroupsLoaded
                    val next = state.copy(expenseGroups = trips, expenseGroupsLoaded = true)
                    when {
                        // Like people groups: preselect the first (newest) expense group.
                        firstLoad && next.expenseGroupId == null -> next.copy(expenseGroupId = trips.firstOrNull()?.id)
                        // The selected one was removed (e.g. data cleared).
                        next.expenseGroupId != null && next.expenseGroup == null -> next.copy(expenseGroupId = null)
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
                        when {
                            // First load with existing groups: preselect the first (newest) one.
                            state.isLoading && next.groupId == null && groups.isNotEmpty() -> next.withGroup(groups.first())
                            // The selected group was removed (e.g. data cleared): back to "No group".
                            next.groupId != null && next.group == null -> next.withGroup(null)
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
            AddExpenseAction.AddExpenseGroupClicked -> _uiState.update { state ->
                // Start from the search text: "searched, not found, add it" in one step.
                val name = state.expenseGroupQuery.trim().take(ExpenseGroup.MAX_NAME_LENGTH)
                state.copy(
                    openSheet = AddExpenseSheet.CreateExpenseGroup,
                    newExpenseGroup = NewExpenseGroupForm(name = name, canCreate = createExpenseGroup.validate(name) == null),
                )
            }
            is AddExpenseAction.NewExpenseGroupNameChanged ->
                if (action.value.length <= ExpenseGroup.MAX_NAME_LENGTH) {
                    _uiState.update {
                        it.copy(newExpenseGroup = it.newExpenseGroup.copy(name = action.value, canCreate = createExpenseGroup.validate(action.value) == null))
                    }
                }
            AddExpenseAction.CreateExpenseGroupClicked -> createNewExpenseGroup()
            AddExpenseAction.GroupClicked -> _uiState.update { it.copy(openSheet = AddExpenseSheet.PeopleGroup) }
            is AddExpenseAction.GroupSelected -> updateForm { state ->
                val group = state.groups.firstOrNull { it.id == action.groupId }
                (if (group == null || group.id == state.groupId) state else state.withGroup(group)).copy(openSheet = null)
            }
            AddExpenseAction.AddGroupClicked ->
                _uiState.update { it.copy(openSheet = AddExpenseSheet.CreateGroup, newGroup = PeopleGroupForm()) }
            is AddExpenseAction.NewGroupNameChanged -> updateNewGroup { form, you -> PeopleGroupFormEditor.nameChanged(form, action.value, you) }
            is AddExpenseAction.MemberInputChanged ->
                updateNewGroup { form, you -> PeopleGroupFormEditor.memberInputChanged(form, action.value, you) }
            AddExpenseAction.AddMemberClicked -> updateNewGroup { form, you -> PeopleGroupFormEditor.addMember(form, you) }
            is AddExpenseAction.MemberRemoved ->
                updateNewGroup { form, you -> PeopleGroupFormEditor.removeMember(form, action.index, you) }
            AddExpenseAction.YouRemoved -> updateNewGroup { form, you -> PeopleGroupFormEditor.removeYou(form, you) }
            AddExpenseAction.CreateGroupClicked -> createNewGroup()
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

    private fun createNewExpenseGroup() {
        val form = _uiState.value.newExpenseGroup
        if (!form.canCreate || form.isCreating) return
        _uiState.update { it.copy(newExpenseGroup = form.copy(isCreating = true)) }
        viewModelScope.launch {
            when (val result = createExpenseGroup(form.name)) {
                is CreateExpenseGroupResult.Success -> updateForm { current ->
                    val created = result.group
                    val trips = if (current.expenseGroups.any { it.id == created.id }) current.expenseGroups
                    else listOf(created) + current.expenseGroups
                    current.copy(
                        expenseGroups = trips,
                        expenseGroupId = created.id,
                        openSheet = null,
                        newExpenseGroup = NewExpenseGroupForm(),
                    )
                }
                is CreateExpenseGroupResult.Invalid, CreateExpenseGroupResult.StorageError -> _uiState.update {
                    it.copy(
                        newExpenseGroup = it.newExpenseGroup.copy(isCreating = false),
                        message = AddExpenseMessage.CreateGroupFailed,
                    )
                }
            }
        }
    }

    // region People group creation (rules shared with "Manage people groups" via PeopleGroupFormEditor)

    private fun createNewGroup() {
        val state = _uiState.value
        val form = state.newGroup
        if (!form.canSave || form.isSaving) return
        val yourName = state.you?.name
        val members = PeopleGroupFormEditor.candidateMembers(form, yourName).map { it.name }
        val includeYou = PeopleGroupFormEditor.candidateIncludesYou(form, yourName)
        _uiState.update { it.copy(newGroup = form.copy(isSaving = true)) }
        viewModelScope.launch {
            when (val result = createGroup(form.name, members, includeYou)) {
                is CreatePeopleGroupResult.Success -> updateForm { current ->
                    val created = result.group
                    val groups = if (current.groups.any { it.id == created.id }) current.groups else listOf(created) + current.groups
                    current.copy(groups = groups, openSheet = null, newGroup = PeopleGroupForm()).withGroup(created)
                }
                is CreatePeopleGroupResult.Invalid, CreatePeopleGroupResult.NoProfile, CreatePeopleGroupResult.StorageError ->
                    _uiState.update {
                        it.copy(newGroup = it.newGroup.copy(isSaving = false), message = AddExpenseMessage.CreateGroupFailed)
                    }
            }
        }
    }

    private fun updateNewGroup(transform: (PeopleGroupForm, yourName: String?) -> PeopleGroupForm) {
        _uiState.update { state -> state.copy(newGroup = transform(state.newGroup, state.you?.name)) }
    }

    // endregion

    private fun onShareChanged(participantId: String, value: String) {
        val type = _uiState.value.splitType
        if (type == SplitType.Equal) return
        val clean = DecimalInput.sanitize(value) ?: return
        updateForm { state ->
            val forType = state.shareInputs[type].orEmpty() + (participantId to clean)
            state.copy(shareInputs = state.shareInputs + (type to forType))
        }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = addExpense(state.toDraft())) {
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
}
