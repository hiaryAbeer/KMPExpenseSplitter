package com.abeerapps.kmpexpensesplitter.feature.expense.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppBottomSheet
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppCard
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTopBar
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.DestructiveButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.FieldLabel
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.InitialAvatar
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_edit
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_trash
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseDetails
import com.abeerapps.kmpexpensesplitter.core.ui.format
import com.abeerapps.kmpexpensesplitter.core.ui.icon
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_back
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_label_paid_by
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_label_split_between
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.expense_details_delete
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.expense_details_delete_failed
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.expense_details_delete_message
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.expense_details_delete_title
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.expense_details_edit
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.expense_details_title
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Test tags for UI tests. */
object ExpenseDetailsTestTags {
    const val EDIT = "expense_details_edit"
    const val DELETE = "expense_details_delete"
}

/**
 * Expense Details: header (category, title, date, amount), who paid, each person's
 * share, then Edit (opens the expense form filled in) and Delete (asks first).
 *
 * @param onEdit receives the expense id.
 */
@Composable
fun ExpenseDetailsScreen(
    onBack: () -> Unit,
    onEdit: (expenseId: String) -> Unit,
    viewModel: ExpenseDetailsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onAction = viewModel::onAction
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailed = stringResource(Res.string.expense_details_delete_failed)

    // Deleted (here or elsewhere): nothing left to show.
    LaunchedEffect(uiState.isMissing) { if (uiState.isMissing) onBack() }
    LaunchedEffect(uiState.deleteFailed) {
        if (uiState.deleteFailed) {
            snackbarHostState.showSnackbar(deleteFailed)
            onAction(ExpenseDetailsAction.MessageShown)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                title = stringResource(Res.string.expense_details_title),
                onBack = onBack,
                backDescription = stringResource(Res.string.add_expense_back),
            )
            uiState.details?.let { details ->
                Content(
                    details = details,
                    onEdit = { onEdit(details.expense.id) },
                    onDelete = { onAction(ExpenseDetailsAction.DeleteClicked) },
                )
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }

    val details = uiState.details
    if (uiState.confirmingDelete && details != null) {
        AppBottomSheet(stringResource(Res.string.expense_details_delete_title, details.expense.title), { onAction(ExpenseDetailsAction.SheetDismissed) }) {
            Text(
                text = stringResource(Res.string.expense_details_delete_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DestructiveButton(
                text = stringResource(Res.string.expense_details_delete),
                onClick = { onAction(ExpenseDetailsAction.DeleteConfirmed) },
                isLoading = uiState.isDeleting,
            )
        }
    }
}

@Composable
private fun Content(details: ExpenseDetails, onEdit: () -> Unit, onDelete: () -> Unit) {
    val spacing = ExpenseSplitterTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        HeaderCard(details)

        Column {
            FieldLabel(stringResource(Res.string.add_expense_label_paid_by))
            Spacer(Modifier.height(spacing.smd))
            PersonCard(name = details.payerName, amount = null)
        }

        Column {
            FieldLabel(stringResource(Res.string.add_expense_label_split_between))
            Spacer(Modifier.height(spacing.smd))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                details.shares.forEach { share -> PersonCard(name = share.name, amount = share.amount.format()) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.smd)) {
            ActionButton(
                icon = painterResource(DsRes.drawable.ic_edit),
                text = stringResource(Res.string.expense_details_edit),
                onClick = onEdit,
                modifier = Modifier.weight(1f).testTag(ExpenseDetailsTestTags.EDIT),
            )
            ActionButton(
                icon = painterResource(DsRes.drawable.ic_trash),
                text = stringResource(Res.string.expense_details_delete),
                onClick = onDelete,
                modifier = Modifier.weight(1f).testTag(ExpenseDetailsTestTags.DELETE),
            )
        }
    }
}

/** Category tile, title and date, then the amount centred under a divider. */
@Composable
private fun HeaderCard(details: ExpenseDetails) {
    val colors = MaterialTheme.colorScheme
    val spacing = ExpenseSplitterTheme.spacing
    val expense = details.expense
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        tint = Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.10f), colors.primary.copy(alpha = 0.05f))),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).background(colors.primary.copy(alpha = 0.2f), MaterialTheme.shapes.large),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(expense.category.icon), null, tint = colors.primary, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatDates(expense.startDate, expense.endDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = spacing.xs),
                    )
                }
            }
            HorizontalDivider(
                color = colors.outlineVariant.copy(alpha = 0.5f),
                thickness = ExpenseSplitterTheme.borders.hairline,
                modifier = Modifier.padding(top = spacing.md),
            )
            Text(
                text = expense.amount.format(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    letterSpacing = 0.4.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = spacing.md),
            )
        }
    }
}

/** Avatar and name, with the person's share on the right when [amount] is given. */
@Composable
private fun PersonCard(name: String, amount: String?) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(name, size = 40.dp)
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (amount != null) {
                Text(amount, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The design's outlined Edit / Delete buttons: icon + label. */
@Composable
private fun ActionButton(icon: Painter, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(ExpenseSplitterTheme.spacing.sm))
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** "Saturday, April 25, 2026", or "April 25 – April 27, 2026" for a range. English names for now. */
private fun formatDates(start: LocalDate, end: LocalDate): String {
    fun monthDay(date: LocalDate) = "${MonthNames.ENGLISH_FULL.names[date.month.ordinal]} ${date.day}"
    if (start != end) {
        val from = if (start.year == end.year) monthDay(start) else "${monthDay(start)}, ${start.year}"
        return "$from – ${monthDay(end)}, ${end.year}"
    }
    return "${DayOfWeekNames.ENGLISH_FULL.names[start.dayOfWeek.ordinal]}, ${monthDay(start)}, ${start.year}"
}
