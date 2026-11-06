package com.abeerapps.kmpexpensesplitter.feature.groups.manage

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.peopleGroupUsage
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreatePeopleGroupResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreatePeopleGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.DeletePeopleGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.DeleteResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpensesUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObservePeopleGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveUserProfileUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.UpdatePeopleGroupResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.UpdatePeopleGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.ui.PeopleGroupForm
import com.abeerapps.kmpexpensesplitter.core.ui.PeopleGroupFormEditor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A people group with its usage.
 *
 * @property usedIds people referenced by its expenses (they can't be removed).
 * @property isUsed any expense uses the group, so it can't be deleted.
 */
@Immutable
data class ManagedPeopleGroup(val group: PeopleGroup, val usedIds: Set<String>, val isUsed: Boolean)

@Immutable
data class ManagePeopleGroupsUiState(
    val isLoading: Boolean = true,
    val yourName: String = "",
    val items: List<ManagedPeopleGroup> = emptyList(),
    val totalCount: Int = 0,
    val form: PeopleGroupForm? = null,
    val pendingDelete: ManagedPeopleGroup? = null,
    val isDeleting: Boolean = false,
    val message: ManageMessage? = null,
)

sealed interface ManagePeopleGroupsAction {
    data class SearchChanged(val query: String) : ManagePeopleGroupsAction
    data object AddClicked : ManagePeopleGroupsAction
    data class EditClicked(val groupId: String) : ManagePeopleGroupsAction
    data class NameChanged(val value: String) : ManagePeopleGroupsAction
    data class MemberInputChanged(val value: String) : ManagePeopleGroupsAction
    data object AddMemberClicked : ManagePeopleGroupsAction
    data class MemberRemoved(val index: Int) : ManagePeopleGroupsAction
    data object YouRemoved : ManagePeopleGroupsAction
    data object SaveClicked : ManagePeopleGroupsAction
    data class DeleteClicked(val groupId: String) : ManagePeopleGroupsAction
    data object DeleteConfirmed : ManagePeopleGroupsAction
    data object SheetDismissed : ManagePeopleGroupsAction
    data object MessageShown : ManagePeopleGroupsAction
}

/**
 * "Manage people groups": search (by group or member name), add and edit with the
 * same sheet and rules as Add Expense, and delete groups no expense uses.
 */
class ManagePeopleGroupsViewModel(
    observePeopleGroups: ObservePeopleGroupsUseCase,
    observeExpenses: ObserveExpensesUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    private val createPeopleGroup: CreatePeopleGroupUseCase,
    private val updatePeopleGroup: UpdatePeopleGroupUseCase,
    private val deletePeopleGroup: DeletePeopleGroupUseCase,
) : ViewModel() {

    private data class Local(
        val form: PeopleGroupForm? = null,
        val pendingDeleteId: String? = null,
        val isDeleting: Boolean = false,
        val message: ManageMessage? = null,
    )

    private val local = MutableStateFlow(Local())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allGroups: List<ManagedPeopleGroup> = emptyList()
    private var yourName: String? = null

    val uiState: StateFlow<ManagePeopleGroupsUiState> = combine(
        combine(observePeopleGroups(), observeExpenses(), observeUserProfile(), ::Triple),
        _searchQuery,
        local,
    ) { (groups, expenses, profile), query, local ->
        val usage = expenses.peopleGroupUsage()
        val managed = groups.map { group ->
            val used = usage[group.id]
            ManagedPeopleGroup(group, used.orEmpty(), isUsed = used != null)
        }
        allGroups = managed
        yourName = profile?.name
        val q = query.trim()
        ManagePeopleGroupsUiState(
            isLoading = false,
            yourName = profile?.name.orEmpty(),
            items = if (q.isEmpty()) managed else managed.filter { it.matches(q) },
            totalCount = managed.size,
            form = local.form,
            pendingDelete = managed.firstOrNull { it.group.id == local.pendingDeleteId },
            isDeleting = local.isDeleting,
            message = local.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManagePeopleGroupsUiState())

    fun onAction(action: ManagePeopleGroupsAction) {
        when (action) {
            is ManagePeopleGroupsAction.SearchChanged -> _searchQuery.value = action.query
            ManagePeopleGroupsAction.AddClicked -> local.update { it.copy(form = PeopleGroupForm()) }
            is ManagePeopleGroupsAction.EditClicked -> {
                val item = allGroups.firstOrNull { it.group.id == action.groupId } ?: return
                local.update { it.copy(form = PeopleGroupForm.forEdit(item.group, item.usedIds, yourName)) }
            }
            is ManagePeopleGroupsAction.NameChanged -> updateForm { PeopleGroupFormEditor.nameChanged(it, action.value, yourName) }
            is ManagePeopleGroupsAction.MemberInputChanged ->
                updateForm { PeopleGroupFormEditor.memberInputChanged(it, action.value, yourName) }
            ManagePeopleGroupsAction.AddMemberClicked -> updateForm { PeopleGroupFormEditor.addMember(it, yourName) }
            is ManagePeopleGroupsAction.MemberRemoved -> updateForm { PeopleGroupFormEditor.removeMember(it, action.index, yourName) }
            ManagePeopleGroupsAction.YouRemoved -> updateForm { PeopleGroupFormEditor.removeYou(it, yourName) }
            ManagePeopleGroupsAction.SaveClicked -> save()
            is ManagePeopleGroupsAction.DeleteClicked -> {
                // Delete is only offered for unused groups; guard anyway.
                if (allGroups.any { it.group.id == action.groupId && !it.isUsed }) {
                    local.update { it.copy(pendingDeleteId = action.groupId) }
                }
            }
            ManagePeopleGroupsAction.DeleteConfirmed -> delete()
            ManagePeopleGroupsAction.SheetDismissed -> local.update { it.copy(form = null, pendingDeleteId = null) }
            ManagePeopleGroupsAction.MessageShown -> local.update { it.copy(message = null) }
        }
    }

    private fun updateForm(transform: (PeopleGroupForm) -> PeopleGroupForm) {
        local.update { state -> state.copy(form = state.form?.let(transform)) }
    }

    private fun save() {
        val form = local.value.form ?: return
        if (!form.canSave || form.isSaving) return
        val members = PeopleGroupFormEditor.candidateMembers(form, yourName)
        val includeYou = PeopleGroupFormEditor.candidateIncludesYou(form, yourName)
        updateForm { it.copy(isSaving = true) }
        viewModelScope.launch {
            val editingId = form.editingId
            val message = if (editingId == null) {
                when (createPeopleGroup(form.name, members.map { it.name }, includeYou)) {
                    is CreatePeopleGroupResult.Success -> null
                    else -> ManageMessage.SaveFailed
                }
            } else {
                when (updatePeopleGroup(editingId, form.name, members, includeYou)) {
                    is UpdatePeopleGroupResult.Success -> null
                    UpdatePeopleGroupResult.MemberInUse -> ManageMessage.MemberInUse
                    else -> ManageMessage.SaveFailed
                }
            }
            local.update {
                if (message == null) it.copy(form = null)
                else it.copy(form = it.form?.copy(isSaving = false), message = message)
            }
        }
    }

    private fun delete() {
        val id = local.value.pendingDeleteId ?: return
        if (local.value.isDeleting) return
        local.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            val result = deletePeopleGroup(id)
            local.update {
                it.copy(
                    pendingDeleteId = null,
                    isDeleting = false,
                    message = if (result == DeleteResult.Deleted) null else ManageMessage.DeleteFailed,
                )
            }
        }
    }

    /** Search matches the group name or any member name, ignoring case. */
    private fun ManagedPeopleGroup.matches(q: String): Boolean =
        group.name.contains(q, ignoreCase = true) || group.members.any { it.name.contains(q, ignoreCase = true) }
}
