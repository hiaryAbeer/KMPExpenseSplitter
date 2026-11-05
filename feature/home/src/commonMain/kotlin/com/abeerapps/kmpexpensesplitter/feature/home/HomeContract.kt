package com.abeerapps.kmpexpensesplitter.feature.home

import androidx.compose.runtime.Immutable
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies

/** Everything the Home tab renders (the search text lives in its own flow). */
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val owedToYou: Money = Money.zero(SupportedCurrencies.default.code),
    val youOwe: Money = Money.zero(SupportedCurrencies.default.code),
    /** Groups matching the search, most recent first. */
    val groups: List<GroupSummary> = emptyList(),
    /** All of the user's groups, ignoring search ("4 groups"). */
    val totalGroupCount: Int = 0,
) {
    /** The user has no groups at all → "No groups yet". */
    val hasNoGroups: Boolean get() = !isLoading && totalGroupCount == 0

    /** Groups exist but none match the search → "No groups found". */
    val hasNoSearchResults: Boolean get() = !isLoading && totalGroupCount > 0 && groups.isEmpty()
}
