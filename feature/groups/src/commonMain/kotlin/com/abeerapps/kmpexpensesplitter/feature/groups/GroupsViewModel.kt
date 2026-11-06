package com.abeerapps.kmpexpensesplitter.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupFilter
import com.abeerapps.kmpexpensesplitter.core.domain.model.filterGroups
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveGroupsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Presentation logic for the Groups tab. Shared by Android and iOS.
 *
 * Combines the expense group summaries with the search text and the selected
 * filter (rules in the domain: `filterGroups`). The search text is its own flow,
 * updated synchronously, so typing never lags behind the state pipeline.
 */
class GroupsViewModel(
    observeGroups: ObserveGroupsUseCase,
    private val dateProvider: DateProvider,
) : ViewModel() {

    private data class Local(
        val filter: GroupFilter = GroupFilter.All,
    )

    private val local = MutableStateFlow(Local())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<GroupsUiState> = combine(observeGroups(), _searchQuery, local) { groups, query, local ->
        GroupsUiState(
            isLoading = false,
            groups = groups.filterGroups(local.filter, query, dateProvider.today()),
            totalCount = groups.size,
            filter = local.filter,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GroupsUiState(),
    )

    fun onAction(action: GroupsAction) {
        when (action) {
            is GroupsAction.SearchChanged -> _searchQuery.value = action.query
            is GroupsAction.FilterSelected -> local.update { it.copy(filter = action.filter) }
        }
    }
}
