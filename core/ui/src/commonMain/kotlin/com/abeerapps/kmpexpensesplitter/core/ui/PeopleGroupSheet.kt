package com.abeerapps.kmpexpensesplitter.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppBottomSheet
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTextField
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.FieldLabel
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.InitialAvatar
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.PrimaryButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_plus
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_trash
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.MemberNameError
import com.abeerapps.kmpexpensesplitter.core.ui.resources.Res
import com.abeerapps.kmpexpensesplitter.core.ui.resources.edit_people_group_title
import com.abeerapps.kmpexpensesplitter.core.ui.resources.group_save
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_add_member
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_create
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_member_blank
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_member_duplicate
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_member_placeholder
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_member_too_long
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_member_too_many
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_members_label
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_name_label
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_name_placeholder
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_remove_member
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_title
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_group_you
import com.abeerapps.kmpexpensesplitter.core.ui.resources.people_group_member_in_use
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Test tags for UI tests. */
object PeopleGroupSheetTestTags {
    const val SAVE = "people_group_save"
}

/** Callbacks of [PeopleGroupSheet]; screens map them to their own actions. */
class PeopleGroupSheetCallbacks(
    val onNameChange: (String) -> Unit,
    val onMemberInputChange: (String) -> Unit,
    val onAddMember: () -> Unit,
    val onRemoveMember: (index: Int) -> Unit,
    val onRemoveYou: () -> Unit,
    val onSave: () -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * "New people group" / "Edit people group": a name and its members. You are listed
 * first and can be removed like anyone else; people used in expenses show "In expenses"
 * instead of a delete button. "Add"/"Save" is enabled with a name and at least one member.
 */
@Composable
fun PeopleGroupSheet(form: PeopleGroupForm, yourName: String, callbacks: PeopleGroupSheetCallbacks) {
    val spacing = ExpenseSplitterTheme.spacing
    val title = if (form.isEditing) Res.string.edit_people_group_title else Res.string.new_group_title
    AppBottomSheet(stringResource(title), callbacks.onDismiss) {
        Column {
            FieldLabel(stringResource(Res.string.new_group_name_label))
            Spacer(Modifier.height(spacing.sm))
            AppTextField(
                value = form.name,
                onValueChange = callbacks.onNameChange,
                placeholder = stringResource(Res.string.new_group_name_placeholder),
                leadingIcon = null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
        }

        Column {
            FieldLabel(stringResource(Res.string.new_group_members_label))
            Spacer(Modifier.height(spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                AppTextField(
                    value = form.memberInput,
                    onValueChange = callbacks.onMemberInputChange,
                    placeholder = stringResource(Res.string.new_group_member_placeholder),
                    leadingIcon = null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    // "Done" adds the name and keeps the keyboard open for the next one.
                    keyboardActions = KeyboardActions(onDone = { callbacks.onAddMember() }),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = callbacks.onAddMember, role = Role.Button),
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
                    onRemove = callbacks.onRemoveYou.takeIf { form.canRemoveYou },
                )
            }
            form.members.forEachIndexed { index, member ->
                MemberRow(
                    name = member.name,
                    label = member.name,
                    onRemove = { callbacks.onRemoveMember(index) }.takeIf { form.canRemove(member) },
                )
            }
        }

        PrimaryButton(
            text = stringResource(if (form.isEditing) Res.string.group_save else Res.string.new_group_create),
            onClick = callbacks.onSave,
            enabled = form.canSave || form.isSaving,
            isLoading = form.isSaving,
            modifier = Modifier.testTag(PeopleGroupSheetTestTags.SAVE),
        )
    }
}

/** A member: avatar, name, then a delete button, or "In expenses" when [onRemove] is `null`. */
@Composable
private fun MemberRow(name: String, label: String, onRemove: (() -> Unit)?) {
    Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
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
        } else {
            Text(
                text = stringResource(Res.string.people_group_member_in_use),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun MemberNameError.message() = when (this) {
    MemberNameError.Blank -> Res.string.new_group_member_blank
    MemberNameError.TooLong -> Res.string.new_group_member_too_long
    MemberNameError.Duplicate -> Res.string.new_group_member_duplicate
    MemberNameError.TooMany -> Res.string.new_group_member_too_many
}
