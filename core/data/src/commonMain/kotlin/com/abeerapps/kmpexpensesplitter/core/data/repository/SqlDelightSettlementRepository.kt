package com.abeerapps.kmpexpensesplitter.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.abeerapps.kmpexpensesplitter.core.data.database.money
import com.abeerapps.kmpexpensesplitter.core.data.db.AppDatabase
import com.abeerapps.kmpexpensesplitter.core.domain.model.Settlement
import com.abeerapps.kmpexpensesplitter.core.domain.repository.SettlementRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/** [SettlementRepository] stored in SQLite, oldest first. */
internal class SqlDelightSettlementRepository(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : SettlementRepository {
    private val queries = database.settlementEntityQueries

    override fun observeSettlements(): Flow<List<Settlement>> =
        queries.selectAll { id, expenseGroupId, fromId, toId, amountMinor, currency, date ->
            Settlement(id, expenseGroupId, fromId, toId, money(amountMinor, currency), LocalDate.parse(date))
        }.asFlow().mapToList(dispatcher)

    /** All in one transaction: a settle up is recorded completely or not at all. */
    override suspend fun addSettlements(settlements: List<Settlement>) {
        withContext(dispatcher) {
            database.transaction {
                settlements.forEach {
                    queries.insert(it.id, it.expenseGroupId, it.fromId, it.toId, it.amount.minorUnits, it.amount.currency.value, it.date.toString())
                }
            }
        }
    }

    override suspend fun deleteSettlementsIn(expenseGroupId: String) {
        withContext(dispatcher) {
            queries.deleteInGroup(expenseGroupId)
        }
    }
}
