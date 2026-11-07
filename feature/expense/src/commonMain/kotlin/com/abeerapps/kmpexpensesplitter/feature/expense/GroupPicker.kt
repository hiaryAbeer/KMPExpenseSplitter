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
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.MemberNameError
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_no_group
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.group_manage
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.group_member_count
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.group_sheet_empty
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.group_sheet_title
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.split_between_no_group
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Test tags for UI tests. */
object GroupPickerTestTags {
    const val FIELD = "add_expense_group"
    const val MANAGE = "group_sheet_manage"
}

/** "People group" field: a prompt until one is picked; opens the people-group sheet. */
@Composable
internal fun GroupField(group: PeopleGroup?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick, role = Role.Button),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
//            GroupIcon(highlighted = group != null)
//            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
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

/**
 * Every group as a radio row, then "Manage people groups" at the bottom, which opens the
 * same Manage screen as the Groups tab (add, edit, delete).
 */
@Composable
internal fun GroupSheet(
    groups: List<PeopleGroup>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onManage: () -> Unit,
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
            text = stringResource(Res.string.group_manage),
            onClick = onManage,
            modifier = Modifier.padding(top = ExpenseSplitterTheme.spacing.sm).testTag(GroupPickerTestTags.MANAGE),
        )
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
