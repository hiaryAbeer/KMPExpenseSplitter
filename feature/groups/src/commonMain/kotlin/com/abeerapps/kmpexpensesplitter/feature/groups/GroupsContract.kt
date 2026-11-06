package com.abeerapps.kmpexpensesplitter.feature.groups

import androidx.compose.runtime.Immutable
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupFilter
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary

/**
 * Everything the Groups tab renders (the search text lives in its own flow).
 *
 * @property groups expense groups matching [filter] and the search, most recently active first.
 * @property totalCount all expense groups, ignoring filter and search.
 */
@Immutable
data class GroupsUiState(
    val isLoading: Boolean = true,
    val groups: List<GroupSummary> = emptyList(),
    val totalCount: Int = 0,
    val filter: GroupFilter = GroupFilter.All,
) {
    /** No expense groups at all → "No groups yet" (groups are created from Add Expense). */
    val hasNoGroups: Boolean get() = !isLoading && totalCount == 0

    /** Groups exist but none match the filter/search → "No groups found". */
    val hasNoResults: Boolean get() = !isLoading && totalCount > 0 && groups.isEmpty()
}

sealed interface GroupsAction {
    data class SearchChanged(val query: String) : GroupsAction
    data class FilterSelected(val filter: GroupFilter) : GroupsAction
}
