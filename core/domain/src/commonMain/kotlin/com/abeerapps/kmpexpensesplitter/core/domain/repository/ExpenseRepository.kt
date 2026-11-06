package com.abeerapps.kmpexpensesplitter.core.domain.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import kotlinx.coroutines.flow.Flow

/** Source of truth for recorded expenses. Implemented in `:core:data`. */
interface ExpenseRepository {

    /** Emits every expense, newest date first; re-emits on change. */
    fun observeExpenses(): Flow<List<Expense>>

    /** Stores [expense]. Throws on storage failure. */
    suspend fun addExpense(expense: Expense)

    /** Replaces the expense with [expense]'s id. Throws on storage failure. */
    suspend fun updateExpense(expense: Expense)

    /** Removes the expense with [expenseId], if present. Throws on storage failure. */
    suspend fun deleteExpense(expenseId: String)

    /** Removes every expense of the expense group [expenseGroupId]. Throws on storage failure. */
    suspend fun deleteExpensesIn(expenseGroupId: String)
}
