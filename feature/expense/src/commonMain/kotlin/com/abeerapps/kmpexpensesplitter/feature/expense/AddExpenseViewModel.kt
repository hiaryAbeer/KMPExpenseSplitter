package com.abeerapps.kmpexpensesplitter.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.DecimalInput
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.Group
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitInput
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.AddExpenseResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.AddExpenseUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreateGroupResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreateGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ExpenseDraft
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveGroupListUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveUserProfileUseCase
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
    observeGroupList: ObserveGroupListUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    private val addExpense: AddExpenseUseCase,
    private val createGroup: CreateGroupUseCase,
    dateProvider: DateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        dateProvider.today().let { today -> AddExpenseUiState(date = today, today = today) },
    )
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddExpenseEvent>(Channel.BUFFERED)
    val events: Flow<AddExpenseEvent> = _events.receiveAsFlow()

    init {
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
            AddExpenseAction.GroupClicked -> _uiState.update { it.copy(openSheet = AddExpenseSheet.Group) }
            is AddExpenseAction.GroupSelected -> updateForm { state ->
                val group = state.groups.firstOrNull { it.id == action.groupId }
                (if (group == null || group.id == state.groupId) state else state.withGroup(group)).copy(openSheet = null)
            }
            AddExpenseAction.AddGroupClicked ->
                _uiState.update { it.copy(openSheet = AddExpenseSheet.CreateGroup, newGroup = NewGroupForm()) }
            is AddExpenseAction.NewGroupNameChanged ->
                if (action.value.length <= Group.MAX_NAME_LENGTH) updateNewGroup { it.copy(name = action.value) }
            is AddExpenseAction.MemberInputChanged ->
                if (action.value.length <= Group.MAX_MEMBER_NAME_LENGTH) {
                    updateNewGroup { it.copy(memberInput = action.value, memberError = null) }
                }
            AddExpenseAction.AddMemberClicked -> addTypedMember()
            is AddExpenseAction.MemberRemoved ->
                updateNewGroup { form -> form.copy(members = form.members.filterIndexed { i, _ -> i != action.index }) }
            AddExpenseAction.YouRemoved -> updateNewGroup { it.copy(includeYou = false) }
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
            is AddExpenseAction.DateSelected -> updateForm { state ->
                // The picker blocks future dates; clamp anyway so the rule lives here.
                state.copy(date = minOf(action.date, state.today), openSheet = null)
            }
            is AddExpenseAction.NoteChanged ->
                if (action.value.length <= Expense.MAX_NOTE_LENGTH) updateForm { it.copy(note = action.value) }
            AddExpenseAction.SheetDismissed -> _uiState.update { it.copy(openSheet = null) }
            AddExpenseAction.SaveClicked -> save()
            AddExpenseAction.MessageShown -> _uiState.update { it.copy(message = null) }
        }
    }

    // region Group creation

    private fun addTypedMember() {
        val state = _uiState.value
        val form = state.newGroup
        if (!form.includeYou && state.isYourName(form.memberInput)) {
            // Typing your own name after removing yourself adds *you* back, not a namesake.
            updateNewGroup { it.copy(includeYou = true, memberInput = "", memberError = null) }
            return
        }
        val error = createGroup.validateMemberName(form.memberInput, form.members, state.yourNameInNewGroup())
        updateNewGroup {
            if (error != null) it.copy(memberError = error)
            else it.copy(members = it.members + it.memberInput.trim(), memberInput = "", memberError = null)
        }
    }

    private fun AddExpenseUiState.isYourName(name: String): Boolean =
        you != null && name.trim().equals(you.name.trim(), ignoreCase = true)

    /** Your name while you're in the new group, else `null` (as the domain rules expect). */
    private fun AddExpenseUiState.yourNameInNewGroup(): String? = you?.name?.takeIf { newGroup.includeYou }

    /** Whether you'll be in the group: the toggle, or your own name still typed in the input. */
    private fun AddExpenseUiState.candidateIncludesYou(): Boolean =
        newGroup.includeYou || isYourName(newGroup.memberInput)

    /** The other members to create the group with: the list plus a valid name still in the input. */
    private fun AddExpenseUiState.candidateMembers(): List<String> {
        val form = newGroup
        val pending = form.memberInput.trim()
        val pendingIsValid = pending.isNotEmpty() && !(!form.includeYou && isYourName(pending)) &&
            createGroup.validateMemberName(pending, form.members, yourNameInNewGroup()) == null
        return if (pendingIsValid) form.members + pending else form.members
    }

    private fun createNewGroup() {
        val state = _uiState.value
        if (!state.newGroup.canCreate || state.newGroup.isCreating) return
        val members = state.candidateMembers()
        val includeYou = state.candidateIncludesYou()
        updateNewGroup { it.copy(isCreating = true) }
        viewModelScope.launch {
            when (val result = createGroup(state.newGroup.name, members, includeYou)) {
                is CreateGroupResult.Success -> updateForm { current ->
                    val created = result.group
                    val groups = if (current.groups.any { it.id == created.id }) current.groups else listOf(created) + current.groups
                    current.copy(groups = groups, openSheet = null, newGroup = NewGroupForm()).withGroup(created)
                }
                is CreateGroupResult.Invalid, CreateGroupResult.NoProfile, CreateGroupResult.StorageError ->
                    _uiState.update {
                        it.copy(newGroup = it.newGroup.copy(isCreating = false), message = AddExpenseMessage.CreateGroupFailed)
                    }
            }
        }
    }

    private fun updateNewGroup(transform: (NewGroupForm) -> NewGroupForm) {
        _uiState.update { state ->
            val form = transform(state.newGroup)
            val withForm = state.copy(newGroup = form)
            val yourName = state.you?.name?.takeIf { withForm.candidateIncludesYou() }
            val canCreate = createGroup.validate(form.name, withForm.candidateMembers(), yourName).isEmpty()
            withForm.copy(newGroup = form.copy(canCreate = canCreate))
        }
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
    private fun AddExpenseUiState.withGroup(group: Group?): AddExpenseUiState =
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
            groupId = groupId,
            title = title,
            amount = Money(DecimalInput.toHundredths(amount) ?: 0L, currency),
            category = category,
            paidBy = paidBy,
            split = when (splitType) {
                SplitType.Equal -> SplitInput.Equal(sharing)
                SplitType.Percentage -> SplitInput.Percentage(typed(SplitType.Percentage))
                SplitType.Exact -> SplitInput.Exact(typed(SplitType.Exact))
            },
            date = date,
            note = note,
            participants = payerOptions,
        )
    }
}
