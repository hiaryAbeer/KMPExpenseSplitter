package com.abeerapps.kmpexpensesplitter.feature.groups.details

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroupDetails
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseGroupDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

enum class DetailsTab { Expenses, Balances, Stats }

/**
 * @property details `null` while loading or once the group no longer exists ([isMissing]).
 */
@Immutable
data class GroupDetailsUiState(
    val isLoading: Boolean = true,
    val details: ExpenseGroupDetails? = null,
    val tab: DetailsTab = DetailsTab.Expenses,
    val today: LocalDate? = null,
) {
    /** The group was deleted (or never existed): the screen closes itself. */
    val isMissing: Boolean get() = !isLoading && details == null
}

/** Group Details: one expense group's expenses, balances and stats. Shared by Android and iOS. */
class GroupDetailsViewModel(
    observeDetails: ObserveExpenseGroupDetailsUseCase,
    dateProvider: DateProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val groupId: String? = savedStateHandle[ARG_GROUP_ID]
    private val tab = MutableStateFlow(DetailsTab.Expenses)
    private val today = dateProvider.today()

    val uiState: StateFlow<GroupDetailsUiState> =
        combine(groupId?.let { observeDetails(it) } ?: flowOf(null), tab) { details, tab ->
            GroupDetailsUiState(isLoading = false, details = details, tab = tab, today = today)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupDetailsUiState())

    fun onTabSelected(selected: DetailsTab) {
        tab.value = selected
    }

    companion object {
        /** Navigation argument: the expense group id (the route's `groupId`). */
        const val ARG_GROUP_ID = "groupId"
    }
}
