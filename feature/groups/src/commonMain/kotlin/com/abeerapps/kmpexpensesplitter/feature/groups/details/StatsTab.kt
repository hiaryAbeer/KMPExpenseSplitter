package com.abeerapps.kmpexpensesplitter.feature.groups.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppCard
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.domain.model.CategoryTotal
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroupDetails
import com.abeerapps.kmpexpensesplitter.core.ui.chartColor
import com.abeerapps.kmpexpensesplitter.core.ui.format
import com.abeerapps.kmpexpensesplitter.core.ui.label
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_stats_average
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_stats_by_category
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_stats_by_member
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_stats_count
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_stats_member_bar
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_stats_slice
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_stats_summary
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_stats_total
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * Stats tab: spending summary cards, spending by category (pie + legend) and
 * spending by member (what each person paid, as bars).
 */
internal fun LazyListScope.stats(details: ExpenseGroupDetails) {
    item(key = "summary-title") { SectionTitle(stringResource(Res.string.details_stats_summary)) }
    item(key = "summary") { SummaryCards(details) }
    item(key = "category-title") { SectionTitle(stringResource(Res.string.details_stats_by_category), Modifier.padding(top = ExpenseSplitterTheme.spacing.sm)) }
    item(key = "category") { CategoryCard(details.categoryTotals) }
    item(key = "member-title") { SectionTitle(stringResource(Res.string.details_stats_by_member), Modifier.padding(top = ExpenseSplitterTheme.spacing.sm)) }
    item(key = "member") { MemberCard(details) }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() },
    )
}

/** "Average per person" and "Total expenses" side by side, "Number of expenses" below. */
@Composable
private fun SummaryCards(details: ExpenseGroupDetails) {
    val spacing = ExpenseSplitterTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.smd)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(spacing.smd)) {
            StatCard(stringResource(Res.string.details_stats_average), details.averagePerPerson.format(), Modifier.weight(1f).fillMaxHeight())
            StatCard(stringResource(Res.string.details_stats_total), details.totalSpent.format(), Modifier.weight(1f).fillMaxHeight())
        }
        StatCard(stringResource(Res.string.details_stats_count), details.expenses.size.toString(), Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = ExpenseSplitterTheme.spacing.xs),
            )
        }
    }
}

/** Pie chart with "Hotel 45%" labels, then a legend row per category with its total. */
@Composable
private fun CategoryCard(totals: List<CategoryTotal>) {
    val labels = totals.map { stringResource(it.category.label) }
    val slices = totals.mapIndexed { i, total ->
        PieSlice(
            label = stringResource(Res.string.details_stats_slice, labels[i], (total.share * 100).roundToInt()),
            fraction = total.share.toFloat(),
            color = total.category.chartColor,
        )
    }
    AppCard(Modifier.fillMaxWidth()) {
        Column {
            PieChart(slices, description = slices.joinToString { it.label })
            Column(
                modifier = Modifier.padding(top = ExpenseSplitterTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ExpenseSplitterTheme.spacing.sm),
            ) {
                totals.forEachIndexed { i, total -> LegendRow(labels[i], total) }
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, total: CategoryTotal) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(total.category.chartColor, CircleShape))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = ExpenseSplitterTheme.spacing.sm),
        )
        Text(text = total.total.format(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** What each member paid, in the order they joined the group's expenses. */
@Composable
private fun MemberCard(details: ExpenseGroupDetails) {
    val description = details.memberSpending.map { stringResource(Res.string.details_stats_member_bar, it.name, it.paid.format()) }
    AppCard(Modifier.fillMaxWidth()) {
        BarChart(
            bars = details.memberSpending.map { BarValue(it.name, it.paid.minorUnits) },
            description = description.joinToString(),
        )
    }
}
