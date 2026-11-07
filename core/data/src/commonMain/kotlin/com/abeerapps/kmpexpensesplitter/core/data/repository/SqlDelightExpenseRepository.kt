package com.abeerapps.kmpexpensesplitter.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.abeerapps.kmpexpensesplitter.core.data.database.money
import com.abeerapps.kmpexpensesplitter.core.data.db.AppDatabase
import com.abeerapps.kmpexpensesplitter.core.data.db.SelectAllWithShares
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * [ExpenseRepository] stored in SQLite: one row per expense plus one per share,
 * written in one transaction and read with one join. Newest date first; same-day
 * expenses keep most-recently-added first.
 */
internal class SqlDelightExpenseRepository(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ExpenseRepository {
    private val queries = database.expenseEntityQueries

    override fun observeExpenses(): Flow<List<Expense>> =
        queries.selectAllWithShares().asFlow().mapToList(dispatcher).map(::toExpenses)

    override suspend fun addExpense(expense: Expense) {
        withContext(dispatcher) {
            database.transaction {
                with(expense) {
                    queries.insertExpense(
                        id, expenseGroupId, peopleGroupId, title, amount.minorUnits, amount.currency.value,
                        category.name, paidBy, splitType.name, startDate.toString(), endDate.toString(), note,
                    )
                }
                insertShares(expense)
            }
        }
    }

    override suspend fun updateExpense(expense: Expense) {
        withContext(dispatcher) {
            database.transaction {
                with(expense) {
                    queries.updateExpense(
                        expenseGroupId = expenseGroupId,
                        peopleGroupId = peopleGroupId,
                        title = title,
                        amountMinor = amount.minorUnits,
                        currency = amount.currency.value,
                        category = category.name,
                        paidBy = paidBy,
                        splitType = splitType.name,
                        startDate = startDate.toString(),
                        endDate = endDate.toString(),
                        note = note,
                        id = id,
                    )
                }
                queries.deleteShares(expense.id)
                insertShares(expense)
            }
        }
    }

    override suspend fun deleteExpense(expenseId: String) {
        withContext(dispatcher) {
            database.transaction {
                queries.deleteShares(expenseId)
                queries.deleteExpense(expenseId)
            }
        }
    }

    override suspend fun deleteExpensesIn(expenseGroupId: String) {
        withContext(dispatcher) {
            database.transaction {
                queries.deleteSharesInGroup(expenseGroupId)
                queries.deleteExpensesInGroup(expenseGroupId)
            }
        }
    }

    private fun insertShares(expense: Expense) {
        expense.shares.forEachIndexed { position, share ->
            queries.insertShare(expense.id, share.participantId, share.amount.minorUnits, position.toLong())
        }
    }

    /** Rows arrive grouped by expense (already in display order) and in share order. */
    private fun toExpenses(rows: List<SelectAllWithShares>): List<Expense> =
        rows.groupBy { it.id }.values.map { expenseRows ->
            val row = expenseRows.first()
            Expense(
                id = row.id,
                expenseGroupId = row.expense_group_id,
                peopleGroupId = row.people_group_id,
                title = row.title,
                amount = money(row.amount_minor, row.currency),
                category = ExpenseCategory.valueOf(row.category),
                paidBy = row.paid_by,
                splitType = SplitType.valueOf(row.split_type),
                shares = expenseRows.mapNotNull { share ->
                    share.participant_id?.let { ExpenseShare(it, money(requireNotNull(share.share_minor), row.currency)) }
                },
                startDate = LocalDate.parse(row.start_date),
                endDate = LocalDate.parse(row.end_date),
                note = row.note,
            )
        }
}
