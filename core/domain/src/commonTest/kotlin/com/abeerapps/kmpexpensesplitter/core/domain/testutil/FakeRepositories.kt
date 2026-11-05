package com.abeerapps.kmpexpensesplitter.core.domain.testutil

import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.Group
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeGroupRepository(initial: List<Group> = emptyList()) : GroupRepository {
    val groups = MutableStateFlow(initial)
    var failNext = false
    override fun observeGroups(): Flow<List<Group>> = groups
    override suspend fun createGroup(group: Group) {
        if (failNext) {
            failNext = false
            error("disk full")
        }
        groups.value = listOf(group) + groups.value
    }
}

class FakeExpenseRepository(initial: List<Expense> = emptyList()) : ExpenseRepository {
    val expenses = MutableStateFlow(initial)
    override fun observeExpenses(): Flow<List<Expense>> = expenses
    override suspend fun addExpense(expense: Expense) {
        expenses.value = listOf(expense) + expenses.value
    }
}
