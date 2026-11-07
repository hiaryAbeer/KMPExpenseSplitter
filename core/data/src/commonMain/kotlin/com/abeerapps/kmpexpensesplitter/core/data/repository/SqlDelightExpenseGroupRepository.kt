package com.abeerapps.kmpexpensesplitter.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.abeerapps.kmpexpensesplitter.core.data.db.AppDatabase
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseGroupRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/** [ExpenseGroupRepository] stored in the app's SQLite database; newest first. */
internal class SqlDelightExpenseGroupRepository(
    database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ExpenseGroupRepository {
    private val queries = database.expenseGroupEntityQueries

    override fun observeExpenseGroups(): Flow<List<ExpenseGroup>> =
        queries.selectAll { id, name, createdOn -> ExpenseGroup(id, name, LocalDate.parse(createdOn)) }
            .asFlow()
            .mapToList(dispatcher)

    override suspend fun createExpenseGroup(group: ExpenseGroup) {
        withContext(dispatcher) {
            queries.insert(group.id, group.name, group.createdOn.toString())
        }
    }

    override suspend fun updateExpenseGroup(group: ExpenseGroup) {
        withContext(dispatcher) {
            queries.update(name = group.name, createdOn = group.createdOn.toString(), id = group.id)
        }
    }

    override suspend fun deleteExpenseGroup(groupId: String) {
        withContext(dispatcher) {
            queries.delete(groupId)
        }
    }
}
