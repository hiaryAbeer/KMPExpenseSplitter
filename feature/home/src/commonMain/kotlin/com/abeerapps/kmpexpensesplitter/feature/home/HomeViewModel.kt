package com.abeerapps.kmpexpensesplitter.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CalculateBalanceSummaryUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Presentation logic for the Home tab. Shared by Android and iOS.
 *
 * Combines the profile (greeting + currency), the groups and the search text
 * into one [HomeUiState]. The search text is exposed separately ([searchQuery])
 * and updated synchronously, so typing never lags behind the state pipeline.
 */
class HomeViewModel(
    observeUserProfile: ObserveUserProfileUseCase,
    observeGroups: ObserveGroupsUseCase,
    private val calculateBalanceSummary: CalculateBalanceSummaryUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        observeUserProfile(),
        observeGroups(),
        _searchQuery,
    ) { profile, groups, query ->
        val currency = profile?.defaultCurrency ?: SupportedCurrencies.default.code
        val summary = calculateBalanceSummary(groups, currency)
        HomeUiState(
            isLoading = false,
            userName = profile?.name.orEmpty(),
            owedToYou = summary.owedToYou,
            youOwe = summary.youOwe,
            groups = groups.filterByName(query).take(MAX_RECENT_GROUPS),
            totalGroupCount = groups.size,
        )
    }.stateIn(
        scope = viewModelScope,
        // Keep the state for 5s across configuration changes, then stop collecting.
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private fun List<GroupSummary>.filterByName(query: String): List<GroupSummary> {
        val q = query.trim()
        return if (q.isEmpty()) this else filter { it.name.contains(q, ignoreCase = true) }
    }

    companion object {
        /** Home shows only the latest few; the Groups tab lists them all. */
        const val MAX_RECENT_GROUPS = 3
    }
}
