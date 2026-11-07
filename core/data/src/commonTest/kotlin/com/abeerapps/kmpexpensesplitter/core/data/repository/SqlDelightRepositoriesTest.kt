package com.abeerapps.kmpexpensesplitter.core.data.repository

import com.abeerapps.kmpexpensesplitter.core.data.testutil.testDatabase
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.Settlement
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Runs the repositories against a real (in-memory) SQLite database. */
class SqlDelightRepositoriesTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val day1 = LocalDate(2026, 4, 24)
    private val day2 = LocalDate(2026, 4, 25)
    private val you = Participant(Participant.YOU_ID, "Ahmad")
    private val sara = Participant("s", "Sara")
    private val omar = Participant("o", "Omar")

    private val database = testDatabase()
    private fun TestScope.dispatcher() = StandardTestDispatcher(testScheduler)

    private fun expense(id: String, date: LocalDate, trip: String = "t1", note: String? = null) = Expense(
        id = id, expenseGroupId = trip, peopleGroupId = "pg", title = "Expense $id", amount = Money(30_00, jod),
        category = ExpenseCategory.Hotel, paidBy = you.id, splitType = SplitType.Exact,
        shares = listOf(ExpenseShare(sara.id, Money(20_00, jod)), ExpenseShare(you.id, Money(10_00, jod))),
        startDate = date, endDate = date, note = note,
    )

    @Test
    fun `expense groups round trip newest first`() = runTest {
        val repo = SqlDelightExpenseGroupRepository(database, dispatcher())
        repo.createExpenseGroup(ExpenseGroup("t1", "Aqaba", day1))
        repo.createExpenseGroup(ExpenseGroup("t2", "Petra", day2))
        repo.updateExpenseGroup(ExpenseGroup("t1", "Aqaba Trip", day1))
        assertEquals(listOf(ExpenseGroup("t2", "Petra", day2), ExpenseGroup("t1", "Aqaba Trip", day1)), repo.observeExpenseGroups().first())

        repo.deleteExpenseGroup("t2")
        assertEquals(listOf("t1"), repo.observeExpenseGroups().first().map { it.id })
    }

    @Test
    fun `people groups keep their members in order across updates`() = runTest {
        val repo = SqlDelightPeopleGroupRepository(database, dispatcher())
        repo.createGroup(PeopleGroup("pg1", "Friends", listOf(you, sara, omar), day1))
        repo.createGroup(PeopleGroup("pg2", "Family", listOf(omar), day2))
        assertEquals(
            listOf(PeopleGroup("pg2", "Family", listOf(omar), day2), PeopleGroup("pg1", "Friends", listOf(you, sara, omar), day1)),
            repo.observeGroups().first(),
        )

        repo.updateGroup(PeopleGroup("pg1", "Close friends", listOf(omar, you), day1))
        assertEquals(PeopleGroup("pg1", "Close friends", listOf(omar, you), day1), repo.observeGroups().first().last())

        repo.deleteGroup("pg1")
        assertEquals(listOf("pg2"), repo.observeGroups().first().map { it.id })
        assertEquals(0L, database.peopleGroupEntityQueries.selectAllWithMembers().executeAsList().count { it.id == "pg1" }.toLong())
    }

    @Test
    fun `expenses round trip with shares and keep the display order`() = runTest {
        val repo = SqlDelightExpenseRepository(database, dispatcher())
        repo.addExpense(expense("a", day1, note = "Sea view"))
        repo.addExpense(expense("b", day2))
        repo.addExpense(expense("c", day1)) // Same day as "a" but added later: shown before it.

        val stored = repo.observeExpenses().first()
        assertEquals(listOf("b", "c", "a"), stored.map { it.id })
        assertEquals(expense("a", day1, note = "Sea view"), stored.last()) // Every field survives, shares in order.

        val edited = expense("a", day2).copy(
            title = "Dinner", category = ExpenseCategory.Food, splitType = SplitType.Equal,
            shares = listOf(ExpenseShare(omar.id, Money(30_00, jod))),
        )
        repo.updateExpense(edited)
        assertEquals(edited, repo.observeExpenses().first().single { it.id == "a" })

        repo.deleteExpense("b")
        assertEquals(listOf("c", "a").toSet(), repo.observeExpenses().first().map { it.id }.toSet())
    }

    @Test
    fun `deleting a trip's expenses removes their shares too`() = runTest {
        val repo = SqlDelightExpenseRepository(database, dispatcher())
        repo.addExpense(expense("a", day1, trip = "t1"))
        repo.addExpense(expense("b", day1, trip = "t2"))

        repo.deleteExpensesIn("t1")

        assertEquals(listOf("b"), repo.observeExpenses().first().map { it.id })
        val shareRows = database.expenseEntityQueries.selectAllWithShares().executeAsList()
        assertTrue(shareRows.all { it.id == "b" })
    }

    @Test
    fun `settlements are stored together and deleted per trip`() = runTest {
        val repo = SqlDelightSettlementRepository(database, dispatcher())
        val s1 = Settlement("s1", "t1", sara.id, you.id, Money(20_00, jod), day2)
        val s2 = Settlement("s2", "t2", omar.id, you.id, Money(5_50, jod), day2)
        repo.addSettlements(listOf(s1, s2))
        assertEquals(listOf(s1, s2), repo.observeSettlements().first())

        repo.deleteSettlementsIn("t1")
        assertEquals(listOf(s2), repo.observeSettlements().first())
    }

    @Test
    fun `data survives reopening the database`() = runTest {
        // Same driver = same database file; a new AppDatabase is what an app restart creates.
        val driver = com.abeerapps.kmpexpensesplitter.core.data.testutil.inMemorySqlDriver()
        SqlDelightExpenseGroupRepository(testDatabase(driver), dispatcher()).createExpenseGroup(ExpenseGroup("t1", "Aqaba", day1))

        val reopened = SqlDelightExpenseGroupRepository(testDatabase(driver), dispatcher())
        assertEquals(listOf("Aqaba"), reopened.observeExpenseGroups().first().map { it.name })
    }
}
