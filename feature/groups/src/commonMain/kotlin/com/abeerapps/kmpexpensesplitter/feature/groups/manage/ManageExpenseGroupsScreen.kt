package com.abeerapps.kmpexpensesplitter.feature.groups.manage

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_folder
import com.abeerapps.kmpexpensesplitter.core.ui.CreateExpenseGroupSheet
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_add_expense_group
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_delete
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_delete_expense_group_message
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_delete_message
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_edit
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_empty_expense_groups_message
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_empty_expense_groups_title
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_expense_count
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_expense_groups_title
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_search_expense_groups
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Manage expense groups": search, add, rename (same sheet as Add Expense) and
 * delete (with confirmation; the group's expenses are deleted too).
 */
@Composable
fun ManageExpenseGroupsScreen(
    onBack: () -> Unit,
    viewModel: ManageExpenseGroupsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction
    val folder = painterResource(DsRes.drawable.ic_folder)

    ManageScaffold(
        title = stringResource(Res.string.manage_expense_groups_title),
        onBack = onBack,
        query = query,
        onQueryChange = { onAction(ManageExpenseGroupsAction.SearchChanged(it)) },
        searchPlaceholder = stringResource(Res.string.manage_search_expense_groups),
        addLabel = stringResource(Res.string.manage_add_expense_group),
        onAdd = { onAction(ManageExpenseGroupsAction.AddClicked) },
        message = uiState.message,
        onMessageShown = { onAction(ManageExpenseGroupsAction.MessageShown) },
        body = when {
            uiState.isLoading -> ManageBody.Loading
            uiState.totalCount == 0 -> ManageBody.Empty(
                icon = folder,
                title = stringResource(Res.string.manage_empty_expense_groups_title),
                message = stringResource(Res.string.manage_empty_expense_groups_message),
            )
            uiState.items.isEmpty() -> ManageBody.NoResults(query)
            else -> ManageBody.Items {
                items(uiState.items, key = { it.group.id }) { item ->
                    ManageRow(
                        icon = folder,
                        title = item.group.name,
                        subtitle = pluralStringResource(Res.plurals.manage_expense_count, item.expenseCount, item.expenseCount),
                        note = null,
                        onEdit = { onAction(ManageExpenseGroupsAction.EditClicked(item.group.id)) },
                        editDescription = stringResource(Res.string.manage_edit, item.group.name),
                        onDelete = { onAction(ManageExpenseGroupsAction.DeleteClicked(item.group.id)) },
                        canDelete = true,
                        deleteDescription = stringResource(Res.string.manage_delete, item.group.name),
                    )
                }
            }
        },
    )

    uiState.editor?.let { editor ->
        CreateExpenseGroupSheet(
            name = editor.name,
            canCreate = editor.canSave,
            isCreating = editor.isSaving,
            onNameChange = { onAction(ManageExpenseGroupsAction.NameChanged(it)) },
            onCreate = { onAction(ManageExpenseGroupsAction.SaveClicked) },
            onDismiss = { onAction(ManageExpenseGroupsAction.SheetDismissed) },
            isEditing = editor.editingId != null,
        )
    }
    uiState.pendingDelete?.let { item ->
        DeleteConfirmSheet(
            name = item.group.name,
            message = if (item.expenseCount > 0) {
                pluralStringResource(Res.plurals.manage_delete_expense_group_message, item.expenseCount, item.expenseCount)
            } else {
                stringResource(Res.string.manage_delete_message)
            },
            isDeleting = uiState.isDeleting,
            onConfirm = { onAction(ManageExpenseGroupsAction.DeleteConfirmed) },
            onDismiss = { onAction(ManageExpenseGroupsAction.SheetDismissed) },
        )
    }
}
