package com.abeerapps.kmpexpensesplitter.feature.groups.manage

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_users
import com.abeerapps.kmpexpensesplitter.core.ui.PeopleGroupSheet
import com.abeerapps.kmpexpensesplitter.core.ui.PeopleGroupSheetCallbacks
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_add_people_group
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_delete
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_delete_disabled
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_delete_message
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_edit
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_empty_people_groups_message
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_empty_people_groups_title
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_people_groups_title
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_search_people_groups
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_used_in_expenses
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Manage people groups": search, add and edit (same sheet as Add Expense), and
 * delete, which is enabled only for groups no expense uses.
 */
@Composable
fun ManagePeopleGroupsScreen(
    onBack: () -> Unit,
    viewModel: ManagePeopleGroupsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction
    val users = painterResource(DsRes.drawable.ic_users)
    val usedNote = stringResource(Res.string.manage_used_in_expenses)

    ManageScaffold(
        title = stringResource(Res.string.manage_people_groups_title),
        onBack = onBack,
        query = query,
        onQueryChange = { onAction(ManagePeopleGroupsAction.SearchChanged(it)) },
        searchPlaceholder = stringResource(Res.string.manage_search_people_groups),
        addLabel = stringResource(Res.string.manage_add_people_group),
        onAdd = { onAction(ManagePeopleGroupsAction.AddClicked) },
        message = uiState.message,
        onMessageShown = { onAction(ManagePeopleGroupsAction.MessageShown) },
        body = when {
            uiState.isLoading -> ManageBody.Loading
            uiState.totalCount == 0 -> ManageBody.Empty(
                icon = users,
                title = stringResource(Res.string.manage_empty_people_groups_title),
                message = stringResource(Res.string.manage_empty_people_groups_message),
            )
            uiState.items.isEmpty() -> ManageBody.NoResults(query)
            else -> ManageBody.Items {
                items(uiState.items, key = { it.group.id }) { item ->
                    ManageRow(
                        icon = users,
                        title = item.group.name,
                        subtitle = item.group.members.joinToString { it.name },
                        note = usedNote.takeIf { item.isUsed },
                        onEdit = { onAction(ManagePeopleGroupsAction.EditClicked(item.group.id)) },
                        editDescription = stringResource(Res.string.manage_edit, item.group.name),
                        onDelete = { onAction(ManagePeopleGroupsAction.DeleteClicked(item.group.id)) },
                        canDelete = !item.isUsed,
                        deleteDescription = stringResource(
                            if (item.isUsed) Res.string.manage_delete_disabled else Res.string.manage_delete,
                            item.group.name,
                        ),
                    )
                }
            }
        },
    )

    uiState.form?.let { form ->
        PeopleGroupSheet(
            form = form,
            yourName = uiState.yourName,
            callbacks = PeopleGroupSheetCallbacks(
                onNameChange = { onAction(ManagePeopleGroupsAction.NameChanged(it)) },
                onMemberInputChange = { onAction(ManagePeopleGroupsAction.MemberInputChanged(it)) },
                onAddMember = { onAction(ManagePeopleGroupsAction.AddMemberClicked) },
                onRemoveMember = { onAction(ManagePeopleGroupsAction.MemberRemoved(it)) },
                onRemoveYou = { onAction(ManagePeopleGroupsAction.YouRemoved) },
                onSave = { onAction(ManagePeopleGroupsAction.SaveClicked) },
                onDismiss = { onAction(ManagePeopleGroupsAction.SheetDismissed) },
            ),
        )
    }
    uiState.pendingDelete?.let { item ->
        DeleteConfirmSheet(
            name = item.group.name,
            message = stringResource(Res.string.manage_delete_message),
            isDeleting = uiState.isDeleting,
            onConfirm = { onAction(ManagePeopleGroupsAction.DeleteConfirmed) },
            onDismiss = { onAction(ManagePeopleGroupsAction.SheetDismissed) },
        )
    }
}
