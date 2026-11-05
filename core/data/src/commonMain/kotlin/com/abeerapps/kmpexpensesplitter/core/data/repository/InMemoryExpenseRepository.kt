package com.abeerapps.kmpexpensesplitter.core.data.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Temporary [ExpenseRepository], like [InMemoryGroupRepository]: expenses live
 * for the app session only. Replace the binding in `dataModule` with a database
 * implementation together with groups; nothing above the data layer changes.
 */
internal class InMemoryExpenseRepository : ExpenseRepository {
    private val expenses = MutableStateFlow<List<Expense>>(emptyList())

    override fun observeExpenses(): Flow<List<Expense>> = expenses.asStateFlow()

    override suspend fun addExpense(expense: Expense) {
        // Newest date first; same-day expenses keep most-recently-added first.
        expenses.update { current -> (listOf(expense) + current).sortedByDescending { it.date } }
    }

    /** Used by "Clear local data". */
    fun clear() {
        expenses.value = emptyList()
    }
}
