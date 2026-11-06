package com.abeerapps.kmpexpensesplitter.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppBottomSheet
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTextField
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.FieldLabel
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.PrimaryButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.ui.resources.Res
import com.abeerapps.kmpexpensesplitter.core.ui.resources.edit_expense_group_title
import com.abeerapps.kmpexpensesplitter.core.ui.resources.group_save
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_expense_group_create
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_expense_group_name_label
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_expense_group_name_placeholder
import com.abeerapps.kmpexpensesplitter.core.ui.resources.new_expense_group_title
import org.jetbrains.compose.resources.stringResource

/**
 * "New expense group" / "Edit expense group": just a name. Shared by Add Expense
 * and "Manage expense groups".
 *
 * @param canCreate whether [name] is valid (`validateExpenseGroupName`).
 * @param isCreating shows progress on the button while saving.
 * @param isEditing renaming an existing group: "Edit expense group" / "Save".
 */
@Composable
fun CreateExpenseGroupSheet(
    name: String,
    canCreate: Boolean,
    isCreating: Boolean,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
    isEditing: Boolean = false,
) {
    val title = if (isEditing) Res.string.edit_expense_group_title else Res.string.new_expense_group_title
    AppBottomSheet(stringResource(title), onDismiss) {
        Column {
            FieldLabel(stringResource(Res.string.new_expense_group_name_label))
            Spacer(Modifier.height(ExpenseSplitterTheme.spacing.sm))
            AppTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = stringResource(Res.string.new_expense_group_name_placeholder),
                leadingIcon = null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canCreate) onCreate() }),
            )
        }
        PrimaryButton(
            text = stringResource(if (isEditing) Res.string.group_save else Res.string.new_expense_group_create),
            onClick = onCreate,
            enabled = canCreate || isCreating,
            isLoading = isCreating,
        )
    }
}
