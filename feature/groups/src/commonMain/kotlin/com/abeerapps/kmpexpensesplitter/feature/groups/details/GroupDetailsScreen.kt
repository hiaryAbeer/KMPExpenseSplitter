package com.abeerapps.kmpexpensesplitter.feature.groups.details

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppCard
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTopBar
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.EmptyState
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.InitialAvatar
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_calendar
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_folder
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_plus
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.domain.model.CategoryTotal
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroupDetails
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseItem
import com.abeerapps.kmpexpensesplitter.core.domain.model.PersonBalance
import com.abeerapps.kmpexpensesplitter.core.ui.AvatarStack
import com.abeerapps.kmpexpensesplitter.core.ui.format
import com.abeerapps.kmpexpensesplitter.core.ui.icon
import com.abeerapps.kmpexpensesplitter.core.ui.label
import com.abeerapps.kmpexpensesplitter.core.ui.resources.ic_empty_groups
import com.abeerapps.kmpexpensesplitter.core.ui.resources.Res as UiRes
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_add_expense
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_empty_message
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_empty_title
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_gets_back
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_owes
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_paid_by
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_settled
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_share_percent
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_tab_balances
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_tab_expenses
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_tab_stats
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_title
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_total_spent
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.manage_back
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/** Test tags for UI tests. */
object GroupDetailsTestTags {
    const val ADD_EXPENSE = "details_add_expense"
}

/**
 * Group Details: header card (members, total), then Expenses / Balances / Stats tabs.
 * The "+" button adds an expense with this expense group preselected.
 *
 * @param onExpenseClick an expense card tapped (opens Expense Details).
 * @param onAddExpense receives this expense group's id.
 */
@Composable
fun GroupDetailsScreen(
    onBack: () -> Unit,
    onExpenseClick: (expenseId: String) -> Unit,
    onAddExpense: (expenseGroupId: String) -> Unit,
    viewModel: GroupDetailsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Deleted elsewhere (e.g. from "Manage expense groups"): nothing to show.
    LaunchedEffect(uiState.isMissing) { if (uiState.isMissing) onBack() }

    val spacing = ExpenseSplitterTheme.spacing
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                title = stringResource(Res.string.details_title),
                onBack = onBack,
                backDescription = stringResource(Res.string.manage_back),
            )
            val details = uiState.details ?: return@Column
            HeaderCard(details, Modifier.padding(horizontal = spacing.screenHorizontal))
            Tabs(selected = uiState.tab, onSelect = viewModel::onTabSelected, modifier = Modifier.padding(top = spacing.md))
            when (uiState.tab) {
                DetailsTab.Expenses ->
                    if (details.expenses.isEmpty()) NoExpenses()
                    else DetailsList { expensesByDate(details.expenses, uiState.today, onExpenseClick) }
                DetailsTab.Balances ->
                    if (details.expenses.isEmpty()) NoExpenses()
                    else DetailsList { items(details.balances, key = { it.participantId }) { BalanceRow(it) } }
                DetailsTab.Stats ->
                    if (details.expenses.isEmpty()) NoExpenses()
                    else DetailsList { items(details.categoryTotals, key = { it.category.name }) { CategoryRow(it) } }
            }
        }

        uiState.details?.let { details ->
            FloatingActionButton(
                onClick = { onAddExpense(details.group.id) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(spacing.screenHorizontal)
                    .size(56.dp)
                    .testTag(GroupDetailsTestTags.ADD_EXPENSE),
            ) {
                Icon(
                    painter = painterResource(DsRes.drawable.ic_plus),
                    contentDescription = stringResource(Res.string.details_add_expense),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/** Name, member avatars and total spent on the design's soft teal gradient. */
@Composable
private fun HeaderCard(details: ExpenseGroupDetails, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val spacing = ExpenseSplitterTheme.spacing
    AppCard(
        modifier = modifier.fillMaxWidth(),
        tint = Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.10f), colors.primary.copy(alpha = 0.05f))),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(colors.primary.copy(alpha = 0.1f), MaterialTheme.shapes.large),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(DsRes.drawable.ic_folder), null, tint = colors.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = details.group.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (details.memberNames.isNotEmpty()) {
                        AvatarStack(details.memberNames, Modifier.padding(top = spacing.xs))
                    }
                }
            }
            HorizontalDivider(
                color = colors.outlineVariant.copy(alpha = 0.5f),
                thickness = ExpenseSplitterTheme.borders.hairline,
                modifier = Modifier.padding(top = spacing.smd),
            )
            Row(Modifier.fillMaxWidth().padding(top = spacing.smd), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.details_total_spent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = details.totalSpent.format(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                    color = colors.onSurface,
                )
            }
        }
    }
}

/** Expenses / Balances / Stats with the teal underline indicator, as designed. */
@Composable
private fun Tabs(selected: DetailsTab, onSelect: (DetailsTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    SecondaryTabRow(
        selectedTabIndex = selected.ordinal,
        modifier = modifier,
        containerColor = colors.background,
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(selected.ordinal),
                height = 2.dp,
                color = colors.primary,
            )
        },
        divider = { HorizontalDivider(color = colors.outlineVariant, thickness = ExpenseSplitterTheme.borders.hairline) },
    ) {
        DetailsTab.entries.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                selectedContentColor = colors.primary,
                unselectedContentColor = colors.onSurfaceVariant,
                text = { Text(stringResource(tab.label), style = MaterialTheme.typography.titleMedium) },
            )
        }
    }
}

@Composable
private fun DetailsList(content: LazyListScope.() -> Unit) {
    val spacing = ExpenseSplitterTheme.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Bottom room so the last card scrolls clear of the "+" button.
        contentPadding = PaddingValues(start = spacing.screenHorizontal, end = spacing.screenHorizontal, top = spacing.md, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        content = content,
    )
}

/** Date headers ("Sat, Apr 25") each followed by that day's expenses. */
private fun LazyListScope.expensesByDate(expenses: List<ExpenseItem>, today: LocalDate?, onClick: (String) -> Unit) {
    expenses.groupBy { it.expense.startDate }.forEach { (date, dayExpenses) ->
        item(key = "date-$date") { DateHeader(date, today) }
        items(dayExpenses, key = { it.expense.id }) { ExpenseRow(it, onClick = { onClick(it.expense.id) }) }
    }
}

@Composable
private fun DateHeader(date: LocalDate, today: LocalDate?) {
    val dayName = DayOfWeekNames.ENGLISH_ABBREVIATED.names[date.dayOfWeek.ordinal]
    val month = MonthNames.ENGLISH_ABBREVIATED.names[date.month.ordinal]
    val year = if (today != null && date.year != today.year) ", ${date.year}" else ""
    Row(
        modifier = Modifier.padding(top = ExpenseSplitterTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ExpenseSplitterTheme.spacing.sm),
    ) {
        Icon(
            painter = painterResource(DsRes.drawable.ic_calendar),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "$dayName, $month ${date.day}$year",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Category icon, title and "Paid by …", amount on the right. */
@Composable
private fun ExpenseRow(item: ExpenseItem, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppCard(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick, role = Role.Button),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(item.expense.category.icon)
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.expense.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.details_paid_by, item.payerName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = ExpenseSplitterTheme.spacing.xs),
                )
            }
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.sm))
            Text(
                text = item.expense.amount.format(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                color = colors.onSurface,
            )
        }
    }
}

/** A person's balance: green "Gets back", red "Owes", or "Settled". */
@Composable
private fun BalanceRow(person: PersonBalance) {
    val extended = ExpenseSplitterTheme.extendedColors
    val balance = person.balance
    val (text, color) = when {
        balance.isPositive -> stringResource(Res.string.details_gets_back, balance.format()) to extended.positive
        balance.isNegative -> stringResource(Res.string.details_owes, (-balance).format()) to extended.negative
        else -> stringResource(Res.string.details_settled) to MaterialTheme.colorScheme.onSurfaceVariant
    }
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(person.name, size = 40.dp)
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(text = text, style = MaterialTheme.typography.titleMedium, color = color)
        }
    }
}

/** A category's total, its share of all spending and a bar showing that share. */
@Composable
private fun CategoryRow(total: CategoryTotal) {
    val colors = MaterialTheme.colorScheme
    AppCard(Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(total.category.icon)
                Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(total.category.label), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Text(
                        text = stringResource(Res.string.details_share_percent, (total.share * 100).roundToInt()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                Text(
                    text = total.total.format(),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
                    color = colors.onSurface,
                )
            }
            LinearProgressIndicator(
                progress = { total.share.toFloat() },
                color = colors.primary,
                trackColor = colors.primary.copy(alpha = 0.1f),
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().padding(top = ExpenseSplitterTheme.spacing.smd).height(6.dp),
            )
        }
    }
}

/** Category icon in the design's 40dp round teal tile. */
@Composable
private fun CategoryIcon(icon: org.jetbrains.compose.resources.DrawableResource) {
    Box(
        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun NoExpenses() {
    Box(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ExpenseSplitterTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            icon = painterResource(UiRes.drawable.ic_empty_groups),
            title = stringResource(Res.string.details_empty_title),
            message = stringResource(Res.string.details_empty_message),
        )
    }
}

private val DetailsTab.label
    get() = when (this) {
        DetailsTab.Expenses -> Res.string.details_tab_expenses
        DetailsTab.Balances -> Res.string.details_tab_balances
        DetailsTab.Stats -> Res.string.details_tab_stats
    }
