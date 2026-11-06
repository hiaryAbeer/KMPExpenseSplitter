package com.abeerapps.kmpexpensesplitter.feature.expense.details

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseDetails
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.DeleteExpenseUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.DeleteResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @property details `null` while loading, or once the expense no longer exists ([isMissing]).
 * @property confirmingDelete the delete confirmation sheet is open.
 */
@Immutable
data class ExpenseDetailsUiState(
    val isLoading: Boolean = true,
    val details: ExpenseDetails? = null,
    val confirmingDelete: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteFailed: Boolean = false,
) {
    /** The expense is gone (deleted here or elsewhere): the screen closes itself. */
    val isMissing: Boolean get() = !isLoading && details == null
}

sealed interface ExpenseDetailsAction {
    data object DeleteClicked : ExpenseDetailsAction
    data object DeleteConfirmed : ExpenseDetailsAction
    data object SheetDismissed : ExpenseDetailsAction
    data object MessageShown : ExpenseDetailsAction
}

/** Expense Details: one expense with its payer and shares; edit or delete it. */
class ExpenseDetailsViewModel(
    observeDetails: ObserveExpenseDetailsUseCase,
    private val deleteExpense: DeleteExpenseUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val expenseId: String? = savedStateHandle[ARG_EXPENSE_ID]

    private data class Local(
        val confirmingDelete: Boolean = false,
        val isDeleting: Boolean = false,
        val deleteFailed: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<ExpenseDetailsUiState> =
        combine(expenseId?.let { observeDetails(it) } ?: flowOf(null), local) { details, local ->
            ExpenseDetailsUiState(
                isLoading = false,
                details = details,
                confirmingDelete = local.confirmingDelete,
                isDeleting = local.isDeleting,
                deleteFailed = local.deleteFailed,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseDetailsUiState())

    fun onAction(action: ExpenseDetailsAction) {
        when (action) {
            ExpenseDetailsAction.DeleteClicked -> local.update { it.copy(confirmingDelete = true) }
            ExpenseDetailsAction.DeleteConfirmed -> delete()
            ExpenseDetailsAction.SheetDismissed -> local.update { it.copy(confirmingDelete = false) }
            ExpenseDetailsAction.MessageShown -> local.update { it.copy(deleteFailed = false) }
        }
    }

    private fun delete() {
        val id = expenseId ?: return
        if (local.value.isDeleting) return
        local.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            val result = deleteExpense(id)
            // On success the observed expense becomes null, so the screen closes
            // itself through `isMissing` (one path for "deleted here" and "deleted elsewhere").
            local.update {
                if (result == DeleteResult.Deleted) it.copy(confirmingDelete = false, isDeleting = false)
                else it.copy(isDeleting = false, deleteFailed = true)
            }
        }
    }

    companion object {
        /** Navigation argument: the expense id (the route's `expenseId`). */
        const val ARG_EXPENSE_ID = "expenseId"
    }
}
