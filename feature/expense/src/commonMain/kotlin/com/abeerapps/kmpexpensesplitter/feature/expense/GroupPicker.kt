package com.abeerapps.kmpexpensesplitter.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppBottomSheet
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppCard
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTextField
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.FieldLabel
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.InitialAvatar
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.PrimaryButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_chevron_down
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_plus
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_trash
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_users
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.domain.model.Group
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.MemberNameError
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_no_group
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.group_add
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.group_member_count
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.group_sheet_empty
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.group_sheet_title
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_add_member
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_create
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_member_blank
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_member_duplicate
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_member_placeholder
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_member_too_long
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_member_too_many
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_members_label
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_name_label
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_name_placeholder
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_remove_member
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_title
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_you
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.split_between_no_group
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Test tags for UI tests. */
object GroupPickerTestTags {
    const val FIELD = "add_expense_group"
    const val ADD_GROUP = "group_sheet_add"
    const val CREATE = "new_group_create"
}

/** "Group" field under Title: "No group" until one is picked; opens the group sheet. */
@Composable
internal fun GroupField(group: Group?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick, role = Role.Button),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GroupIcon(highlighted = group != null)
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
            Column(Modifier.weight(1f)) {
                Text(
                    text = group?.name ?: stringResource(Res.string.add_expense_no_group),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (group != null) colors.onSurface else colors.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (group != null) {
                    Text(
                        text = pluralStringResource(Res.plurals.group_member_count, group.members.size, group.members.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Icon(
                painter = painterResource(DsRes.drawable.ic_chevron_down),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Shown under "Split Between" while no group is selected: the section is disabled. */
@Composable
internal fun SplitBetweenDisabled() {
    AppCard(Modifier.fillMaxWidth().alpha(0.5f)) {
        Text(
            text = stringResource(Res.string.split_between_no_group),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Every group as a radio row, then "Add group" at the bottom. */
@Composable
internal fun GroupSheet(
    groups: List<Group>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onAddGroup: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(stringResource(Res.string.group_sheet_title), onDismiss) {
        if (groups.isEmpty()) {
            Text(
                text = stringResource(Res.string.group_sheet_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(ExpenseSplitterTheme.spacing.sm)) {
                groups.forEach { group ->
                    val isSelected = group.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(group.id) })
                            .padding(vertical = ExpenseSplitterTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ExpenseSplitterTheme.spacing.smd),
                    ) {
                        GroupIcon(highlighted = true)
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = group.members.joinToString { it.name },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = null, // The whole row is the click target.
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
        PrimaryButton(
            text = stringResource(Res.string.group_add),
            onClick = onAddGroup,
            modifier = Modifier.padding(top = ExpenseSplitterTheme.spacing.sm).testTag(GroupPickerTestTags.ADD_GROUP),
        )
    }
}

/**
 * "New group": a name and its members. You are listed first and can be removed
 * like anyone else. "Add" (enabled with a name and at least one member) creates
 * the group; the screen then selects it.
 */
@Composable
internal fun CreateGroupSheet(
    form: NewGroupForm,
    yourName: String,
    onAction: (AddExpenseAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = ExpenseSplitterTheme.spacing
    AppBottomSheet(stringResource(Res.string.new_group_title), onDismiss) {
        Column {
            FieldLabel(stringResource(Res.string.new_group_name_label))
            Spacer(Modifier.padding(top = spacing.sm))
            AppTextField(
                value = form.name,
                onValueChange = { onAction(AddExpenseAction.NewGroupNameChanged(it)) },
                placeholder = stringResource(Res.string.new_group_name_placeholder),
                leadingIcon = null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
        }

        Column {
            FieldLabel(stringResource(Res.string.new_group_members_label))
            Spacer(Modifier.padding(top = spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                AppTextField(
                    value = form.memberInput,
                    onValueChange = { onAction(AddExpenseAction.MemberInputChanged(it)) },
                    placeholder = stringResource(Res.string.new_group_member_placeholder),
                    leadingIcon = null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    // "Done" adds the name and keeps the keyboard open for the next one.
                    keyboardActions = KeyboardActions(onDone = { onAction(AddExpenseAction.AddMemberClicked) }),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = { onAction(AddExpenseAction.AddMemberClicked) }, role = Role.Button),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(DsRes.drawable.ic_plus),
                        contentDescription = stringResource(Res.string.new_group_add_member),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            form.memberError?.let { error ->
                Text(
                    text = stringResource(error.message()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ExpenseSplitterTheme.extendedColors.negative,
                    modifier = Modifier.padding(top = spacing.xs, start = spacing.xs),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            if (form.includeYou) {
                MemberRow(
                    name = yourName,
                    label = stringResource(Res.string.new_group_you, yourName),
                    onRemove = { onAction(AddExpenseAction.YouRemoved) },
                )
            }
            form.members.forEachIndexed { index, name ->
                MemberRow(name = name, label = name, onRemove = { onAction(AddExpenseAction.MemberRemoved(index)) })
            }
        }

        PrimaryButton(
            text = stringResource(Res.string.new_group_create),
            onClick = { onAction(AddExpenseAction.CreateGroupClicked) },
            enabled = form.canCreate || form.isCreating,
            isLoading = form.isCreating,
            modifier = Modifier.testTag(GroupPickerTestTags.CREATE),
        )
    }
}

@Composable
private fun MemberRow(name: String, label: String, onRemove: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        InitialAvatar(name)
        Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onRemove != null) {
            Box(
                modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.large).clickable(onClick = onRemove, role = Role.Button),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(DsRes.drawable.ic_trash),
                    contentDescription = stringResource(Res.string.new_group_remove_member, name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Users icon in a tinted 40dp tile; teal once a group is chosen. */
@Composable
private fun GroupIcon(highlighted: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                if (highlighted) colors.primary.copy(alpha = 0.1f) else colors.surfaceContainerHigh,
                MaterialTheme.shapes.medium,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(DsRes.drawable.ic_users),
            contentDescription = null,
            tint = if (highlighted) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun MemberNameError.message() = when (this) {
    MemberNameError.Blank -> Res.string.new_group_member_blank
    MemberNameError.TooLong -> Res.string.new_group_member_too_long
    MemberNameError.Duplicate -> Res.string.new_group_member_duplicate
    MemberNameError.TooMany -> Res.string.new_group_member_too_many
}
