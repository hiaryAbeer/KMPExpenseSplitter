package com.abeerapps.kmpexpensesplitter.feature.groups.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppBottomSheet
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppCard
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTextField
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTopBar
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.DestructiveButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.EmptyState
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.PrimaryButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_edit
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_search
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_trash
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_back
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_delete_confirm
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_delete_failed
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_delete_title
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_member_in_use
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_no_results
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_save_failed
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Content of a manage screen: loading, empty, no search results, or the list. */
internal sealed interface ManageBody {
    data object Loading : ManageBody
    data class Empty(val icon: Painter, val title: String, val message: String) : ManageBody
    data class NoResults(val query: String) : ManageBody
    data class Items(val content: LazyListScope.() -> Unit) : ManageBody
}

/**
 * Shared layout of the two manage screens: top bar, search field, list (or empty
 * state) and the "Add …" button pinned at the bottom.
 */
@Composable
internal fun ManageScaffold(
    title: String,
    onBack: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    addLabel: String,
    onAdd: () -> Unit,
    message: ManageMessage?,
    onMessageShown: () -> Unit,
    body: ManageBody,
) {
    val spacing = ExpenseSplitterTheme.spacing
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = message?.let {
        stringResource(
            when (it) {
                ManageMessage.SaveFailed -> Res.string.manage_save_failed
                ManageMessage.DeleteFailed -> Res.string.manage_delete_failed
                ManageMessage.MemberInUse -> Res.string.manage_member_in_use
            },
        )
    }
    LaunchedEffect(message) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onMessageShown()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = title, onBack = onBack, backDescription = stringResource(Res.string.manage_back))
            AppTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = searchPlaceholder,
                leadingIcon = painterResource(DsRes.drawable.ic_search),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal).padding(top = spacing.md),
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (body) {
                    ManageBody.Loading -> Unit
                    is ManageBody.Empty -> Centered { EmptyState(icon = body.icon, title = body.title, message = body.message) }
                    is ManageBody.NoResults -> Centered {
                        Text(
                            text = stringResource(Res.string.manage_no_results, body.query.trim()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is ManageBody.Items -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.smd),
                        content = body.content,
                    )
                }
            }
            PrimaryButton(
                text = addLabel,
                onClick = onAdd,
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
            )
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ExpenseSplitterTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/**
 * One group in a manage list: icon tile, name, details, then edit and delete.
 * When [canDelete] is false the delete button is dimmed and does nothing, and
 * [deleteDescription] explains why to screen readers.
 */
@Composable
internal fun ManageRow(
    icon: Painter,
    title: String,
    subtitle: String,
    note: String?,
    onEdit: () -> Unit,
    editDescription: String,
    onDelete: () -> Unit,
    canDelete: Boolean,
    deleteDescription: String,
) {
    val colors = MaterialTheme.colorScheme
    AppCard(Modifier.fillMaxWidth(), contentPadding = ExpenseSplitterTheme.spacing.smd) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(colors.primary.copy(alpha = 0.1f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (note != null) {
                    Text(note, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
            }
            RowIconButton(painterResource(DsRes.drawable.ic_edit), editDescription, onEdit, enabled = true)
            RowIconButton(painterResource(DsRes.drawable.ic_trash), deleteDescription, onDelete, enabled = canDelete)
        }
    }
}

@Composable
private fun RowIconButton(icon: Painter, description: String, onClick: () -> Unit, enabled: Boolean) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button, onClickLabel = description)
            .alpha(if (enabled) 1f else 0.3f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/** "Delete “name”?" with a warning and a red Delete button (dismiss the sheet to cancel). */
@Composable
internal fun DeleteConfirmSheet(
    name: String,
    message: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(stringResource(Res.string.manage_delete_title, name), onDismiss) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DestructiveButton(text = stringResource(Res.string.manage_delete_confirm), onClick = onConfirm, isLoading = isDeleting)
    }
}
