package com.abeerapps.kmpexpensesplitter.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppCard
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.EmptyState
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_plus
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_search
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.ui.GroupCard
import com.abeerapps.kmpexpensesplitter.core.ui.format
import com.abeerapps.kmpexpensesplitter.core.ui.resources.ic_empty_groups
import com.abeerapps.kmpexpensesplitter.core.ui.resources.Res as UiRes
import com.abeerapps.kmpexpensesplitter.feature.home.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.home.resources.home_add_expense
import com.abeerapps.kmpexpensesplitter.feature.home.resources.home_empty_message
import com.abeerapps.kmpexpensesplitter.feature.home.resources.home_empty_title
import com.abeerapps.kmpexpensesplitter.feature.home.resources.home_greeting
import com.abeerapps.kmpexpensesplitter.feature.home.resources.home_group_count
import com.abeerapps.kmpexpensesplitter.feature.home.resources.home_no_results_message
import com.abeerapps.kmpexpensesplitter.feature.home.resources.home_no_results_title
import com.abeerapps.kmpexpensesplitter.feature.home.resources.home_recent_groups
import com.abeerapps.kmpexpensesplitter.feature.home.resources.home_search_placeholder
import com.abeerapps.kmpexpensesplitter.feature.home.resources.home_you_are_owed
import com.abeerapps.kmpexpensesplitter.feature.home.resources.home_you_owe
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Test tags for UI tests. */
object HomeTestTags {
    const val SEARCH_FIELD = "home_search"
    const val EMPTY_STATE = "home_empty_state"
    const val ADD_EXPENSE_BUTTON = "home_add_expense"
}

/**
 * Stateful entry point for the Home tab.
 *
 * @param onAddExpense "+" button tapped.
 * @param onGroupClick a group card tapped (only possible once groups exist).
 */
@Composable
fun HomeScreen(
    onAddExpense: () -> Unit,
    onGroupClick: (groupId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onAddExpense = onAddExpense,
        onGroupClick = onGroupClick,
        modifier = modifier,
    )
}

/** Stateless Home UI. Previewable with any [HomeUiState]. */
@Composable
fun HomeContent(
    uiState: HomeUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddExpense: () -> Unit,
    onGroupClick: (groupId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ExpenseSplitterTheme.spacing
    val showsEmptyState = uiState.hasNoGroups || uiState.hasNoSearchResults

    Box(modifier.fillMaxSize()) {
        if (!uiState.isLoading) {
            // One scrolling Column for every state (so the search field keeps focus while
            // results change). Its minimum height is the viewport, so an empty state can
            // take the leftover space and centre itself; taller content simply scrolls.
            // Home lists *recent* groups only, so a non-lazy Column is cheap here.
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(
                            start = spacing.screenHorizontal,
                            end = spacing.screenHorizontal,
                            top = spacing.md,
                            // List: room so the last card scrolls clear of the "+" button.
                            bottom = if (showsEmptyState) spacing.md else 88.dp,
                        ),
                ) {
                    Greeting(uiState.userName)
                    BalanceCards(uiState, Modifier.padding(top = spacing.md))
                    GroupSearchBar(searchQuery, onSearchQueryChange, Modifier.padding(top = spacing.md))
                    RecentGroupsHeader(uiState.totalGroupCount, Modifier.padding(top = spacing.md, bottom = spacing.md))

                    when {
                        uiState.hasNoGroups -> CenteredInRemainingSpace {
                            EmptyState(
                                icon = painterResource(UiRes.drawable.ic_empty_groups),
                                title = stringResource(Res.string.home_empty_title),
                                message = stringResource(Res.string.home_empty_message),
                                modifier = Modifier.testTag(HomeTestTags.EMPTY_STATE),
                            )
                        }
                        uiState.hasNoSearchResults -> CenteredInRemainingSpace {
                            EmptyState(
                                icon = painterResource(DsRes.drawable.ic_search),
                                title = stringResource(Res.string.home_no_results_title),
                                message = stringResource(Res.string.home_no_results_message, searchQuery.trim()),
                            )
                        }
                        else -> uiState.groups.forEach { group ->
                            key(group.id) {
                                GroupCard(
                                    group = group,
                                    onClick = { onGroupClick(group.id) },
                                    modifier = Modifier.padding(bottom = spacing.smd),
                                )
                            }
                        }
                    }
                }
            }
        }

        AddExpenseButton(
            onClick = onAddExpense,
            modifier = Modifier.align(Alignment.BottomEnd).padding(spacing.screenHorizontal),
        )
    }
}

/**
 * Takes all vertical space left in the parent Column and centres [content] in it.
 * Works inside a scrolling Column because that Column has a minimum height.
 */
@Composable
private fun ColumnScope.CenteredInRemainingSpace(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().weight(1f),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Greeting(name: String) {
    Text(
        text = stringResource(Res.string.home_greeting, name),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun BalanceCards(uiState: HomeUiState, modifier: Modifier = Modifier) {
    val colors = ExpenseSplitterTheme.extendedColors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        BalanceCard(
            label = stringResource(Res.string.home_you_are_owed),
            amount = uiState.owedToYou.format(),
            accent = colors.positive,
            modifier = Modifier.weight(1f),
        )
        BalanceCard(
            label = stringResource(Res.string.home_you_owe),
            amount = uiState.youOwe.format(),
            accent = colors.negative,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Card with a faint diagonal gradient of [accent] (10% → 5%), as in the design. */
@Composable
private fun BalanceCard(label: String, amount: String, accent: Color, modifier: Modifier = Modifier) {
    AppCard(
        modifier = modifier,
        tint = Brush.linearGradient(listOf(accent.copy(alpha = 0.10f), accent.copy(alpha = 0.05f))),
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
                color = accent,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GroupSearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)

    AppCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(DsRes.drawable.ic_search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.weight(1f).testTag(HomeTestTags.SEARCH_FIELD),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.home_search_placeholder),
                                style = textStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

@Composable
private fun RecentGroupsHeader(count: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.home_recent_groups),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Text(
            text = pluralStringResource(Res.plurals.home_group_count, count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AddExpenseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        modifier = modifier.size(56.dp).testTag(HomeTestTags.ADD_EXPENSE_BUTTON),
    ) {
        Icon(
            painter = painterResource(DsRes.drawable.ic_plus),
            contentDescription = stringResource(Res.string.home_add_expense),
            modifier = Modifier.size(28.dp),
        )
    }
}

// region Previews

private val PreviewJod = CurrencyCode.of("JOD")!!

private fun previewGroup(id: String, name: String, members: List<String>, total: Long, balance: Long) =
    GroupSummary(id, name, members, Money(total, PreviewJod), Money(balance, PreviewJod), LocalDate(2026, 4, 29))

@Preview
@Composable
private fun HomeEmptyPreview() {
    ExpenseSplitterTheme {
        HomeContent(
            uiState = HomeUiState(isLoading = false, userName = "Ahmad"),
            searchQuery = "",
            onSearchQueryChange = {},
            onAddExpense = {},
            onGroupClick = {},
        )
    }
}

/** The filled state from the Figma design (light). */
@Preview
@Composable
private fun HomeWithGroupsPreview() = HomeWithGroupsSample(darkTheme = false)

/** The filled state from the Figma dark design. */
@Preview
@Composable
private fun HomeWithGroupsDarkPreview() = HomeWithGroupsSample(darkTheme = true)

@Preview
@Composable
private fun HomeEmptyDarkPreview() {
    ExpenseSplitterTheme(darkTheme = true) {
        HomeContent(
            uiState = HomeUiState(isLoading = false, userName = "Ahmad"),
            searchQuery = "",
            onSearchQueryChange = {},
            onAddExpense = {},
            onGroupClick = {},
        )
    }
}

@Composable
private fun HomeWithGroupsSample(darkTheme: Boolean) {
    val groups = listOf(
        previewGroup("1", "Aqaba Trip", listOf("Ahmad", "Sara", "Omar", "Lina"), 1250_50, 85_25),
        previewGroup("2", "Apartment Bills", listOf("Ahmad", "Omar", "Ali"), 680_00, -45_00),
        previewGroup("3", "University Project", listOf("Ahmad", "Sara", "Lina"), 320_75, 12_50),
        previewGroup("4", "Friends Dinner", listOf("Ahmad", "Omar", "Sara", "Lina", "Ali"), 145_00, 0),
    )
    ExpenseSplitterTheme(darkTheme = darkTheme) {
        HomeContent(
            uiState = HomeUiState(
                isLoading = false,
                userName = "Ahmad",
                owedToYou = Money(97_75, PreviewJod),
                youOwe = Money(45_00, PreviewJod),
                groups = groups,
                totalGroupCount = groups.size,
            ),
            searchQuery = "",
            onSearchQueryChange = {},
            onAddExpense = {},
            onGroupClick = {},
        )
    }
}

// endregion
