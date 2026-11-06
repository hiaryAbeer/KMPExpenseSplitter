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
    override suspend fun updateGroup(group: PeopleGroup) {
        groups.value = groups.value.map { if (it.id == group.id) group else it }
    }
    override suspend fun deleteGroup(groupId: String) {
        groups.value = groups.value.filterNot { it.id == groupId }
    }
}

class FakeExpenseRepository(initial: List<Expense> = emptyList()) : ExpenseRepository {
    val expenses = MutableStateFlow(initial)
    override fun observeExpenses(): Flow<List<Expense>> = expenses
    override suspend fun addExpense(expense: Expense) {
        expenses.value = listOf(expense) + expenses.value
    }
    override suspend fun updateExpense(expense: Expense) {
        expenses.value = expenses.value.map { if (it.id == expense.id) expense else it }
    }
    override suspend fun deleteExpense(expenseId: String) {
        expenses.value = expenses.value.filterNot { it.id == expenseId }
    }
    override suspend fun deleteExpensesIn(expenseGroupId: String) {
        expenses.value = expenses.value.filterNot { it.expenseGroupId == expenseGroupId }
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
    override suspend fun updateExpenseGroup(group: ExpenseGroup) {
        groups.value = groups.value.map { if (it.id == group.id) group else it }
    }
    override suspend fun deleteExpenseGroup(groupId: String) {
        groups.value = groups.value.filterNot { it.id == groupId }
    }
}
