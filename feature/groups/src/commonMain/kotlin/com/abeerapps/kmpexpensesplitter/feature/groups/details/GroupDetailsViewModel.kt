package com.abeerapps.kmpexpensesplitter.feature.groups.details

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroupDetails
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseGroupDetailsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.SettleUpResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.SettleUpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

enum class DetailsTab { Expenses, Balances, Stats }

/**
 * @property details `null` while loading or once the group no longer exists ([isMissing]).
 * @property showSettleUp the Settle Up sheet is open.
 * @property settleFailed storing the settlements failed; shown once as a snackbar.
 */
@Immutable
data class GroupDetailsUiState(
    val isLoading: Boolean = true,
    val details: ExpenseGroupDetails? = null,
    val tab: DetailsTab = DetailsTab.Expenses,
    val today: LocalDate? = null,
    val showSettleUp: Boolean = false,
    val isSettling: Boolean = false,
    val settleFailed: Boolean = false,
) {
    /** The group was deleted (or never existed): the screen closes itself. */
    val isMissing: Boolean get() = !isLoading && details == null

    /** "Add expense" belongs to the Expenses and Stats tabs; Balances ends with Settle Up instead. */
    val showAddExpense: Boolean get() = details != null && tab == DetailsTab.Expenses

    /** Settle Up is offered only while someone still owes money. */
    val canSettleUp: Boolean get() = details?.isSettled == false
}

sealed interface GroupDetailsAction {
    data class TabSelected(val tab: DetailsTab) : GroupDetailsAction
    data object SettleUpClicked : GroupDetailsAction
    data object SettleUpDismissed : GroupDetailsAction
    data object MarkSettledClicked : GroupDetailsAction
    data object MessageShown : GroupDetailsAction
}

/** Group Details: one expense group's expenses, balances, settle up and stats. Shared by Android and iOS. */
class GroupDetailsViewModel(
    observeDetails: ObserveExpenseGroupDetailsUseCase,
    private val settleUp: SettleUpUseCase,
    dateProvider: DateProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val groupId: String? = savedStateHandle[ARG_GROUP_ID]
    private val today = dateProvider.today()

    /** Screen-owned state; the details themselves come from the repositories. */
    private val local = MutableStateFlow(GroupDetailsUiState(isLoading = false, today = today))

    private val details = groupId?.let { observeDetails(it) } ?: flowOf(null)

    val uiState: StateFlow<GroupDetailsUiState> =
        combine(details, local) { details, local -> local.copy(details = details) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupDetailsUiState())

    fun onAction(action: GroupDetailsAction) {
        when (action) {
            is GroupDetailsAction.TabSelected -> local.update { it.copy(tab = action.tab) }
            GroupDetailsAction.SettleUpClicked ->
                if (uiState.value.canSettleUp) local.update { it.copy(showSettleUp = true) }
            GroupDetailsAction.SettleUpDismissed ->
                local.update { if (it.isSettling) it else it.copy(showSettleUp = false) }
            GroupDetailsAction.MarkSettledClicked -> markSettled()
            GroupDetailsAction.MessageShown -> local.update { it.copy(settleFailed = false) }
        }
    }

    private fun markSettled() {
        val details = uiState.value.details ?: return
        if (local.value.isSettling) return
        local.update { it.copy(isSettling = true) }
        viewModelScope.launch {
            val result = settleUp(details.group.id, details.transfers)
            local.update {
                when (result) {
                    // Balances drop to zero, so the Settle Up button disappears with the sheet.
                    SettleUpResult.Settled -> it.copy(isSettling = false, showSettleUp = false)
                    SettleUpResult.StorageError -> it.copy(isSettling = false, settleFailed = true)
                }
            }
        }
    }

    companion object {
        /** Navigation argument: the expense group id (the route's `groupId`). */
        const val ARG_GROUP_ID = "groupId"
    }
}
