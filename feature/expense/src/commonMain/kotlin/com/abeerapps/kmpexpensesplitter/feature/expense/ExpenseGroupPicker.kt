package com.abeerapps.kmpexpensesplitter.feature.expense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.PrimaryButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_chevron_down
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_search
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_no_expense_group
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.expense_group_add
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.expense_group_empty
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.expense_group_no_results
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.expense_group_search_placeholder
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.expense_group_sheet_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Test tags for UI tests. */
object ExpenseGroupPickerTestTags {
    const val FIELD = "add_expense_expense_group"
    const val SEARCH = "expense_group_search"
    const val ADD = "expense_group_add"
}

/** "Expense Group" field under Title: a prompt until one is picked; opens the expense group sheet. */
@Composable
internal fun ExpenseGroupField(expenseGroup: ExpenseGroup?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick, role = Role.Button)
            .testTag(ExpenseGroupPickerTestTags.FIELD),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = expenseGroup?.name ?: stringResource(Res.string.add_expense_no_expense_group),
                style = MaterialTheme.typography.bodyLarge,
                color = if (expenseGroup != null) colors.onSurface else colors.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(DsRes.drawable.ic_chevron_down),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Search, the matching expense groups as radio rows, and "Add expense group" at the bottom. */
@Composable
internal fun ExpenseGroupSheet(
    query: String,
    allCount: Int,
    results: List<ExpenseGroup>,
    selectedId: String?,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(stringResource(Res.string.expense_group_sheet_title), onDismiss) {
        if (allCount > 0) {
            AppTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(Res.string.expense_group_search_placeholder),
                leadingIcon = painterResource(DsRes.drawable.ic_search),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.testTag(ExpenseGroupPickerTestTags.SEARCH),
            )
        }
        when {
            allCount == 0 -> HintText(stringResource(Res.string.expense_group_empty))
            results.isEmpty() -> HintText(stringResource(Res.string.expense_group_no_results, query.trim()))
            else -> Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(ExpenseSplitterTheme.spacing.xs)) {
                results.forEach { group ->
                    val isSelected = group.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(group.id) })
                            .padding(vertical = ExpenseSplitterTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
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
            text = stringResource(Res.string.expense_group_add),
            onClick = onAdd,
            modifier = Modifier.padding(top = ExpenseSplitterTheme.spacing.sm).testTag(ExpenseGroupPickerTestTags.ADD),
        )
    }
}

@Composable
private fun HintText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
