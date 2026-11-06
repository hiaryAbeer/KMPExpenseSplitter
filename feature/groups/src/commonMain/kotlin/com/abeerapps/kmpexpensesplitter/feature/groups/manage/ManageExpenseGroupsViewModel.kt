package com.abeerapps.kmpexpensesplitter.feature.groups.manage

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.countByExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreateExpenseGroupResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreateExpenseGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.DeleteExpenseGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.DeleteResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpensesUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.RenameExpenseGroupResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.RenameExpenseGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.validateExpenseGroupName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** An expense group with how many expenses it holds. */
@Immutable
data class ManagedExpenseGroup(val group: ExpenseGroup, val expenseCount: Int)

/** The add/rename sheet; [editingId] is `null` when adding. */
@Immutable
data class ExpenseGroupEditor(
    val editingId: String? = null,
    val name: String = "",
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
)

enum class ManageMessage { SaveFailed, DeleteFailed, MemberInUse }

/**
 * @property items expense groups matching the search, newest first.
 * @property pendingDelete the group awaiting delete confirmation.
 */
@Immutable
data class ManageExpenseGroupsUiState(
    val isLoading: Boolean = true,
    val items: List<ManagedExpenseGroup> = emptyList(),
    val totalCount: Int = 0,
    val editor: ExpenseGroupEditor? = null,
    val pendingDelete: ManagedExpenseGroup? = null,
    val isDeleting: Boolean = false,
    val message: ManageMessage? = null,
)

sealed interface ManageExpenseGroupsAction {
    data class SearchChanged(val query: String) : ManageExpenseGroupsAction
    data object AddClicked : ManageExpenseGroupsAction
    data class EditClicked(val groupId: String) : ManageExpenseGroupsAction
    data class NameChanged(val name: String) : ManageExpenseGroupsAction
    data object SaveClicked : ManageExpenseGroupsAction
    data class DeleteClicked(val groupId: String) : ManageExpenseGroupsAction
    data object DeleteConfirmed : ManageExpenseGroupsAction
    data object SheetDismissed : ManageExpenseGroupsAction
    data object MessageShown : ManageExpenseGroupsAction
}

/**
 * "Manage expense groups": search, add, rename and delete (with their expenses,
 * after confirmation). Shared by Android and iOS.
 */
class ManageExpenseGroupsViewModel(
    observeExpenseGroups: ObserveExpenseGroupsUseCase,
    observeExpenses: ObserveExpensesUseCase,
    private val createExpenseGroup: CreateExpenseGroupUseCase,
    private val renameExpenseGroup: RenameExpenseGroupUseCase,
    private val deleteExpenseGroup: DeleteExpenseGroupUseCase,
) : ViewModel() {

    private data class Local(
        val editor: ExpenseGroupEditor? = null,
        val pendingDeleteId: String? = null,
        val isDeleting: Boolean = false,
        val message: ManageMessage? = null,
    )

    private val local = MutableStateFlow(Local())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allGroups: List<ManagedExpenseGroup> = emptyList()

    val uiState: StateFlow<ManageExpenseGroupsUiState> = combine(
        observeExpenseGroups(),
        observeExpenses(),
        _searchQuery,
        local,
    ) { groups, expenses, query, local ->
        val counts = expenses.countByExpenseGroup()
        val managed = groups.map { ManagedExpenseGroup(it, counts[it.id] ?: 0) }
        allGroups = managed
        val matching = ObserveExpenseGroupsUseCase.search(groups, query).mapTo(HashSet()) { it.id }
        ManageExpenseGroupsUiState(
            isLoading = false,
            items = managed.filter { it.group.id in matching },
            totalCount = managed.size,
            editor = local.editor,
            pendingDelete = managed.firstOrNull { it.group.id == local.pendingDeleteId },
            isDeleting = local.isDeleting,
            message = local.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageExpenseGroupsUiState())

    fun onAction(action: ManageExpenseGroupsAction) {
        when (action) {
            is ManageExpenseGroupsAction.SearchChanged -> _searchQuery.value = action.query
            ManageExpenseGroupsAction.AddClicked -> local.update { it.copy(editor = ExpenseGroupEditor()) }
            is ManageExpenseGroupsAction.EditClicked -> {
                val group = allGroups.firstOrNull { it.group.id == action.groupId }?.group ?: return
                local.update { it.copy(editor = ExpenseGroupEditor(group.id, group.name, canSave = true)) }
            }
            is ManageExpenseGroupsAction.NameChanged ->
                if (action.name.length <= ExpenseGroup.MAX_NAME_LENGTH) {
                    local.update { state ->
                        state.copy(editor = state.editor?.copy(name = action.name, canSave = validateExpenseGroupName(action.name) == null))
                    }
                }
            ManageExpenseGroupsAction.SaveClicked -> save()
            is ManageExpenseGroupsAction.DeleteClicked -> local.update { it.copy(pendingDeleteId = action.groupId) }
            ManageExpenseGroupsAction.DeleteConfirmed -> delete()
            ManageExpenseGroupsAction.SheetDismissed -> local.update { it.copy(editor = null, pendingDeleteId = null) }
            ManageExpenseGroupsAction.MessageShown -> local.update { it.copy(message = null) }
        }
    }

    private fun save() {
        val editor = local.value.editor ?: return
        if (!editor.canSave || editor.isSaving) return
        local.update { it.copy(editor = editor.copy(isSaving = true)) }
        viewModelScope.launch {
            val ok = if (editor.editingId == null) {
                createExpenseGroup(editor.name) is CreateExpenseGroupResult.Success
            } else {
                renameExpenseGroup(editor.editingId, editor.name) is RenameExpenseGroupResult.Success
            }
            local.update {
                if (ok) it.copy(editor = null)
                else it.copy(editor = it.editor?.copy(isSaving = false), message = ManageMessage.SaveFailed)
            }
        }
    }

    private fun delete() {
        val id = local.value.pendingDeleteId ?: return
        if (local.value.isDeleting) return
        local.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            val result = deleteExpenseGroup(id)
            local.update {
                it.copy(
                    pendingDeleteId = null,
                    isDeleting = false,
                    message = if (result == DeleteResult.Deleted) null else ManageMessage.DeleteFailed,
                )
            }
        }
    }
}
