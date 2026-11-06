package com.abeerapps.kmpexpensesplitter.core.domain.testutil

import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePeopleGroupRepository(initial: List<PeopleGroup> = emptyList()) : PeopleGroupRepository {
    val groups = MutableStateFlow(initial)
    var failNext = false
    override fun observeGroups(): Flow<List<PeopleGroup>> = groups
    override suspend fun createGroup(group: PeopleGroup) {
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

class FakeExpenseGroupRepository(initial: List<ExpenseGroup> = emptyList()) : ExpenseGroupRepository {
    val groups = MutableStateFlow(initial)
    var failNext = false
    override fun observeExpenseGroups(): Flow<List<ExpenseGroup>> = groups
    override suspend fun createExpenseGroup(group: ExpenseGroup) {
        if (failNext) {
            failNext = false
            error("disk full")
        }
        groups.value = listOf(group) + groups.value
    }
}
