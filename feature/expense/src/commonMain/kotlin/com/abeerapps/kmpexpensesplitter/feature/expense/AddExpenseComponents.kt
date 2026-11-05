package com.abeerapps.kmpexpensesplitter.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppBottomSheet
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppCard
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.InitialAvatar
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_calendar
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_check
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_chevron_down
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_amount_placeholder
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_paid_by_sheet_title
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.category_bills
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.category_entertainment
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.category_food
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.category_hotel
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.category_shopping
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.category_transport
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.date_picker_cancel
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.date_picker_confirm
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.date_today
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.date_yesterday
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.ic_category_bills
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.ic_category_entertainment
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.ic_category_food
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.ic_category_hotel
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.ic_category_shopping
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.ic_category_transport
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.split_equal
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.split_equal_hint
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.split_exact
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.split_exact_hint
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.split_percent_placeholder
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.split_percent_sign
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.split_percentage
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.split_percentage_hint
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.split_share_description
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Large teal amount ("0.00") with the currency code on the right, as designed. */
@Composable
internal fun AmountField(
    value: String,
    currency: CurrencyCode,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val amountStyle = MaterialTheme.typography.headlineSmall.copy(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.37.sp,
        fontWeight = FontWeight.Medium,
        color = primary,
    )
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = amountStyle,
            cursorBrush = SolidColor(primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.add_expense_amount_placeholder),
                            style = amountStyle,
                            color = primary.copy(alpha = 0.5f),
                        )
                    }
                    inner()
                }
            },
        )
        Text(
            text = currency.value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The six category tiles, 3 per row; exactly one is selected. */
@Composable
internal fun CategoryGrid(selected: ExpenseCategory, onSelect: (ExpenseCategory) -> Unit) {
    val gap = ExpenseSplitterTheme.spacing.smd
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(gap)) {
        ExpenseCategory.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { category ->
                    CategoryTile(
                        category = category,
                        isSelected = category == selected,
                        onClick = { onSelect(category) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: ExpenseCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val content = if (isSelected) colors.primary else colors.onSurfaceVariant
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.surfaceContainerHigh)
            .then(if (isSelected) Modifier.border(1.5.dp, colors.primary, shape) else Modifier)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = ExpenseSplitterTheme.spacing.xs, vertical = ExpenseSplitterTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ExpenseSplitterTheme.spacing.sm),
    ) {
        Icon(
            painter = painterResource(category.icon),
            contentDescription = null, // The label below names it.
            tint = content,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(category.label),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Shows who paid (avatar + name); opens the payer sheet on tap. */
@Composable
internal fun PaidByCard(payer: Participant?, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick, role = Role.Button),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (payer != null) {
                InitialAvatar(payer.name)
                Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
            }
            Text(
                text = payer?.name.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(DsRes.drawable.ic_chevron_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Equal / Percentage / Exact: three equal-width chips, the selected one filled. */
@Composable
internal fun SplitTypeSelector(selected: SplitType, onSelect: (SplitType) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ExpenseSplitterTheme.spacing.sm),
    ) {
        SplitType.entries.forEach { type ->
            val isSelected = type == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(if (isSelected) colors.primary else colors.surfaceContainerHigh)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(type) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(type.label),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** One-line rule under the split chips; red when the entered split doesn't add up. */
@Composable
internal fun SplitHint(type: SplitType, isError: Boolean) {
    Text(
        text = stringResource(type.hint),
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
        color = if (isError) ExpenseSplitterTheme.extendedColors.negative else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = ExpenseSplitterTheme.spacing.xs, top = ExpenseSplitterTheme.spacing.xs),
    )
}

/**
 * A person under "Split Between": checkbox, avatar, name and, for Percentage /
 * Exact, their share. Tapping the row (outside the share field) toggles them.
 */
@Composable
internal fun ParticipantRow(
    participant: Participant,
    isSelected: Boolean,
    splitType: SplitType,
    shareInput: String,
    currency: CurrencyCode,
    onToggle: () -> Unit,
    onShareChange: (String) -> Unit,
) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .toggleable(value = isSelected, role = Role.Checkbox, onValueChange = { onToggle() }),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckMark(checked = isSelected)
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
            InitialAvatar(participant.name)
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
            Text(
                text = participant.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            when (splitType) {
                SplitType.Equal -> Unit
                SplitType.Percentage -> ShareInput(
                    value = shareInput,
                    placeholder = stringResource(Res.string.split_percent_placeholder),
                    suffix = stringResource(Res.string.split_percent_sign),
                    width = 64.dp,
                    enabled = isSelected,
                    description = stringResource(Res.string.split_share_description, participant.name),
                    onValueChange = onShareChange,
                )
                SplitType.Exact -> ShareInput(
                    value = shareInput,
                    placeholder = stringResource(Res.string.add_expense_amount_placeholder),
                    suffix = currency.value,
                    width = 96.dp,
                    enabled = isSelected,
                    description = stringResource(Res.string.split_share_description, participant.name),
                    onValueChange = onShareChange,
                )
            }
        }
    }
}

/** The design's 20dp rounded checkbox: teal with a white tick when checked. */
@Composable
private fun CheckMark(checked: Boolean) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(shape)
            .background(if (checked) colors.primary else colors.surface)
            .border(1.5.dp, if (checked) colors.primary else colors.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(DsRes.drawable.ic_check),
                contentDescription = null, // The row's toggleable semantics carry the state.
                tint = colors.onPrimary,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/** Small pill input for a person's percentage or amount, followed by its unit. */
@Composable
private fun ShareInput(
    value: String,
    placeholder: String,
    suffix: String,
    width: Dp,
    enabled: Boolean,
    description: String,
    onValueChange: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface, textAlign = TextAlign.End)
    Row(
        modifier = Modifier.alpha(if (enabled) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ExpenseSplitterTheme.spacing.xs),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .width(width)
                .semantics { contentDescription = description },
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(colors.surfaceContainerLow)
                        .border(ExpenseSplitterTheme.borders.hairline, colors.outlineVariant, shape)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = style, color = colors.onSurface.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth())
                    }
                    inner()
                }
            },
        )
        Text(suffix, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}

/** "Today, April 29, 2026" card; opens the date picker on tap. */
@Composable
internal fun DateCard(date: LocalDate, today: LocalDate, onClick: () -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick, role = Role.Button),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(DsRes.drawable.ic_calendar),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
            Text(
                text = formatExpenseDate(date, today),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun PaidBySheet(
    participants: List<Participant>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(stringResource(Res.string.add_expense_paid_by_sheet_title), onDismiss) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(ExpenseSplitterTheme.spacing.sm)) {
            participants.forEach { person ->
                val isSelected = person.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(person.id) })
                        .padding(vertical = ExpenseSplitterTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ExpenseSplitterTheme.spacing.smd),
                ) {
                    InitialAvatar(person.name)
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
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
}

/** Material date picker limited to today and earlier (expenses can't be in the future). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseDatePicker(
    date: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayMillis = today.toUtcMillis()
    val selectableDates = remember(todayMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            override fun isSelectableYear(year: Int) = year <= today.year
        }
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date.toUtcMillis(),
        selectableDates = selectableDates,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onSelect(LocalDate.fromEpochDays(it / MILLIS_PER_DAY)) } ?: onDismiss()
            }) { Text(stringResource(Res.string.date_picker_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.date_picker_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

/** "Today, April 29, 2026" / "Yesterday, …" / "April 12, 2026". English month names for now. */
@Composable
private fun formatExpenseDate(date: LocalDate, today: LocalDate): String {
    val full = "${MonthNames.ENGLISH_FULL.names[date.month.ordinal]} ${date.day}, ${date.year}"
    return when (date) {
        today -> stringResource(Res.string.date_today, full)
        today.minus(DatePeriod(days = 1)) -> stringResource(Res.string.date_yesterday, full)
        else -> full
    }
}

/** The date picker works in UTC midnight milliseconds. */
private const val MILLIS_PER_DAY = 86_400_000L

private fun LocalDate.toUtcMillis(): Long = toEpochDays() * MILLIS_PER_DAY

private val ExpenseCategory.icon: DrawableResource
    get() = when (this) {
        ExpenseCategory.Food -> Res.drawable.ic_category_food
        ExpenseCategory.Transport -> Res.drawable.ic_category_transport
        ExpenseCategory.Hotel -> Res.drawable.ic_category_hotel
        ExpenseCategory.Bills -> Res.drawable.ic_category_bills
        ExpenseCategory.Shopping -> Res.drawable.ic_category_shopping
        ExpenseCategory.Entertainment -> Res.drawable.ic_category_entertainment
    }

private val ExpenseCategory.label: StringResource
    get() = when (this) {
        ExpenseCategory.Food -> Res.string.category_food
        ExpenseCategory.Transport -> Res.string.category_transport
        ExpenseCategory.Hotel -> Res.string.category_hotel
        ExpenseCategory.Bills -> Res.string.category_bills
        ExpenseCategory.Shopping -> Res.string.category_shopping
        ExpenseCategory.Entertainment -> Res.string.category_entertainment
    }

private val SplitType.label: StringResource
    get() = when (this) {
        SplitType.Equal -> Res.string.split_equal
        SplitType.Percentage -> Res.string.split_percentage
        SplitType.Exact -> Res.string.split_exact
    }

private val SplitType.hint: StringResource
    get() = when (this) {
        SplitType.Equal -> Res.string.split_equal_hint
        SplitType.Percentage -> Res.string.split_percentage_hint
        SplitType.Exact -> Res.string.split_exact_hint
    }
