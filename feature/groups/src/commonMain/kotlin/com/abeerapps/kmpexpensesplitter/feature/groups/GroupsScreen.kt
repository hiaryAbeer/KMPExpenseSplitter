package com.abeerapps.kmpexpensesplitter.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTextField
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.EmptyState
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_folder
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_plus
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_search
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_users
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupFilter
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.ui.GroupCard
import com.abeerapps.kmpexpensesplitter.core.ui.resources.ic_empty_groups
import com.abeerapps.kmpexpensesplitter.core.ui.resources.Res as UiRes
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_add_expense
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_empty_message
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_empty_title
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_manage_expense_groups
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_manage_people_groups
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_filter_all
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_filter_recent
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_filter_settled
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_filter_you_are_owed
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_filter_you_owe
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_no_results_filter
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_no_results_query
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_no_results_title
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_search_placeholder
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.groups_title
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Test tags for UI tests. */
object GroupsTestTags {
    const val SEARCH_FIELD = "groups_search"
    const val EMPTY_STATE = "groups_empty_state"
    const val ADD_EXPENSE = "groups_add_expense"
}

/**
 * Stateful entry point for the Groups tab.
 *
 * @param onGroupClick a group card tapped.
 * @param onAddExpense "+" button tapped (same action as on Home).
 * @param onManageExpenseGroups folder icon in the header.
 * @param onManagePeopleGroups people icon in the header.
 */
@Composable
fun GroupsScreen(
    onGroupClick: (groupId: String) -> Unit,
    onAddExpense: () -> Unit,
    onManageExpenseGroups: () -> Unit,
    onManagePeopleGroups: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    GroupsContent(
        uiState = uiState,
        searchQuery = searchQuery,
        onAction = viewModel::onAction,
        onGroupClick = onGroupClick,
        onAddExpense = onAddExpense,
        onManageExpenseGroups = onManageExpenseGroups,
        onManagePeopleGroups = onManagePeopleGroups,
        modifier = modifier,
    )
}

/** Stateless Groups UI. Previewable with any [GroupsUiState]. */
@Composable
fun GroupsContent(
    uiState: GroupsUiState,
    searchQuery: String,
    onAction: (GroupsAction) -> Unit,
    onGroupClick: (groupId: String) -> Unit,
    onAddExpense: () -> Unit,
    onManageExpenseGroups: () -> Unit,
    onManagePeopleGroups: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ExpenseSplitterTheme.spacing

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            Header(
                onManageExpenseGroups = onManageExpenseGroups,
                onManagePeopleGroups = onManagePeopleGroups,
                modifier = Modifier.padding(start = spacing.screenHorizontal, end = spacing.sm, top = spacing.md),
            )
            SearchField(
                query = searchQuery,
                onQueryChange = { onAction(GroupsAction.SearchChanged(it)) },
                modifier = Modifier.padding(horizontal = spacing.screenHorizontal).padding(top = spacing.md),
            )
            FilterChips(
                selected = uiState.filter,
                onSelect = { onAction(GroupsAction.FilterSelected(it)) },
                modifier = Modifier.padding(top = spacing.md),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = spacing.smd),
            )

            when {
                uiState.isLoading -> Unit
                uiState.hasNoGroups -> CenteredEmptyState {
                    EmptyState(
                        icon = painterResource(UiRes.drawable.ic_empty_groups),
                        title = stringResource(Res.string.groups_empty_title),
                        message = stringResource(Res.string.groups_empty_message),
                        modifier = Modifier.testTag(GroupsTestTags.EMPTY_STATE),
                    )
                }
                uiState.hasNoResults -> CenteredEmptyState {
                    EmptyState(
                        icon = painterResource(DsRes.drawable.ic_search),
                        title = stringResource(Res.string.groups_no_results_title),
                        message = if (searchQuery.isNotBlank()) {
                            stringResource(Res.string.groups_no_results_query, searchQuery.trim())
                        } else {
                            stringResource(Res.string.groups_no_results_filter, stringResource(uiState.filter.label))
                        },
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Bottom room so the last card scrolls clear of the "+" button.
                    contentPadding = PaddingValues(start = spacing.screenHorizontal, end = spacing.screenHorizontal, top = spacing.md, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.smd),
                ) {
                    items(uiState.groups, key = { it.id }) { group ->
                        GroupCard(group = group, onClick = { onGroupClick(group.id) })
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

/** "Groups" title with "manage expense groups" and "manage people groups" icon buttons. */
@Composable
private fun Header(onManageExpenseGroups: () -> Unit, onManagePeopleGroups: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.groups_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        HeaderIconButton(
            painterResource(DsRes.drawable.ic_folder),
            stringResource(Res.string.groups_manage_expense_groups),
            onManageExpenseGroups,
        )
        HeaderIconButton(
            painterResource(DsRes.drawable.ic_users),
            stringResource(Res.string.groups_manage_people_groups),
            onManagePeopleGroups,
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.painter.Painter,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    AppTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(Res.string.groups_search_placeholder),
        leadingIcon = painterResource(DsRes.drawable.ic_search),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier.testTag(GroupsTestTags.SEARCH_FIELD),
    )
}

/** Horizontally scrolling pill chips; exactly one is selected. */
@Composable
private fun FilterChips(selected: GroupFilter, onSelect: (GroupFilter) -> Unit, modifier: Modifier = Modifier) {
    val spacing = ExpenseSplitterTheme.spacing
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
            .padding(horizontal = spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        GroupFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.primary else colors.surfaceContainerHigh)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(filter) })
                    .padding(horizontal = spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(filter.label),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Centres an empty state in the space below the header; scrolls if the screen is short. */
@Composable
private fun CenteredEmptyState(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ExpenseSplitterTheme.spacing.md),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun AddExpenseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
        modifier = modifier.size(56.dp).testTag(GroupsTestTags.ADD_EXPENSE),
    ) {
        Icon(
            painter = painterResource(DsRes.drawable.ic_plus),
            contentDescription = stringResource(Res.string.groups_add_expense),
            modifier = Modifier.size(28.dp),
        )
    }
}

private val GroupFilter.label: StringResource
    get() = when (this) {
        GroupFilter.All -> Res.string.groups_filter_all
        GroupFilter.YouOwe -> Res.string.groups_filter_you_owe
        GroupFilter.YouAreOwed -> Res.string.groups_filter_you_are_owed
        GroupFilter.Settled -> Res.string.groups_filter_settled
        GroupFilter.RecentlyActive -> Res.string.groups_filter_recent
    }

// region Previews

@Preview
@Composable
private fun GroupsEmptyPreview() {
    ExpenseSplitterTheme {
        GroupsContent(GroupsUiState(isLoading = false), "", {}, {}, {}, {}, {})
    }
}

@Preview
@Composable
private fun GroupsWithContentDarkPreview() {
    val jod = CurrencyCode.of("JOD")!!
    val day = LocalDate(2026, 4, 29)
    val groups = listOf(
        GroupSummary("1", "Aqaba Trip", listOf("Ahmad", "Sara", "Omar", "Lina"), Money(1250_50, jod), Money(85_25, jod), day),
        GroupSummary("2", "Apartment Bills", listOf("Ahmad", "Omar", "Ali"), Money(680_00, jod), Money(-45_00, jod), day),
    )
    ExpenseSplitterTheme(darkTheme = true) {
        GroupsContent(GroupsUiState(isLoading = false, groups = groups, totalCount = 2), "", {}, {}, {}, {}, {})
    }
}

// endregion
