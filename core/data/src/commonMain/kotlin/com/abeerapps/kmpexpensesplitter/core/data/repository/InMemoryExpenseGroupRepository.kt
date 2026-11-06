package com.abeerapps.kmpexpensesplitter.core.data.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Temporary [ExpenseGroupRepository], like the other in-memory stores: expense
 * groups live for the app session only. Replace with database storage together
 * with people groups and expenses.
 */
internal class InMemoryExpenseGroupRepository : ExpenseGroupRepository {
    private val groups = MutableStateFlow<List<ExpenseGroup>>(emptyList())

    override fun observeExpenseGroups(): Flow<List<ExpenseGroup>> = groups.asStateFlow()

    override suspend fun createExpenseGroup(group: ExpenseGroup) {
        groups.update { listOf(group) + it } // Newest first.
    }

    /** Used by "Clear local data". */
    fun clear() {
        groups.value = emptyList()
    }
}
