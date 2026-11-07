package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.Settlement
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakeExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakeExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakePeopleGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakeSettlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ExpenseGroupUseCasesTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val today = LocalDate(2026, 4, 29)
    private val profiles = object : UserProfileRepository {
        val profile = MutableStateFlow<UserProfile?>(UserProfile("Ahmad", jod))
        override fun observeProfile(): Flow<UserProfile?> = profile
        override suspend fun saveProfile(profile: UserProfile) { this.profile.value = profile }
    }
    private val trips = FakeExpenseGroupRepository()
    private val people = FakePeopleGroupRepository()
    private var nextId = 0
    private val create = CreateExpenseGroupUseCase(trips, { today }, newId = { "t${nextId++}" })

    private val you = Participant(Participant.YOU_ID, "Ahmad")
    private val sara = Participant("s", "Sara")
    private val omar = Participant("o", "Omar")

    @Test
    fun `creating an expense group trims the name and generates an id`() = runTest {
        assertEquals(CreateExpenseGroupResult.Success(ExpenseGroup("t0", "Aqaba Trip", today)), create("  Aqaba Trip "))
        assertEquals(listOf("t0"), trips.groups.value.map { it.id })
    }

    @Test
    fun `names may repeat because the id identifies the group`() = runTest {
        create("Trip")
        create("Trip")
        val ids = trips.groups.value.map { it.id }
        assertEquals(2, ids.size)
        assertNotEquals(ids[0], ids[1])
    }

    @Test
    fun `name must be 1 to 50 characters`() = runTest {
        assertEquals(CreateExpenseGroupResult.Invalid(ExpenseGroupNameError.Blank), create("  "))
        assertEquals(ExpenseGroupNameError.TooLong, create.validate("x".repeat(51)))
        assertNull(create.validate("x".repeat(50)))
    }

    @Test
    fun `storage failure is reported`() = runTest {
        trips.failNext = true
        assertEquals(CreateExpenseGroupResult.StorageError, create("Trip"))
    }

    @Test
    fun `search matches names ignoring case`() {
        val all = listOf(ExpenseGroup("1", "Aqaba Trip", today), ExpenseGroup("2", "Apartment", today))
        assertEquals(listOf("Aqaba Trip"), ObserveExpenseGroupsUseCase.search(all, " aqaba ").map { it.name })
        assertEquals(all, ObserveExpenseGroupsUseCase.search(all, ""))
    }

    @Test
    fun `summaries combine expenses with different people groups`() = runTest {
        trips.groups.value = listOf(ExpenseGroup("trip", "Aqaba Trip", today))
        people.groups.value = listOf(
            PeopleGroup("pg1", "Friends", listOf(you, sara), today),
            PeopleGroup("pg2", "Family", listOf(you, omar), today),
        )
        val expenses = FakeExpenseRepository(
            listOf(
                // Newest first, as the repository emits them.
                // Omar paid 10, all yours: you owe 10.
                expense("e2", "pg2", paidBy = omar.id, total = 10_00, shares = mapOf(you.id to 10_00L)),
                // You paid 30, split with Sara: she owes you 15.
                expense("e1", "pg1", paidBy = you.id, total = 30_00, shares = mapOf(you.id to 15_00L, sara.id to 15_00L)),
                // Another currency: ignored.
                expense("e3", "pg1", paidBy = sara.id, total = 99_00, shares = mapOf(you.id to 99_00L), currency = CurrencyCode.of("USD")!!),
            ),
        )

        val summary = ObserveGroupsUseCase(trips, people, expenses, FakeSettlementRepository(), profiles)().first().single()

        assertEquals(
            GroupSummary("trip", "Aqaba Trip", listOf("Ahmad", "Sara", "Omar"), Money(40_00, jod), Money(5_00, jod), today),
            summary,
        )
    }

    @Test
    fun `settlements move your balance on the summary`() = runTest {
        trips.groups.value = listOf(ExpenseGroup("trip", "Aqaba Trip", today))
        // Omar paid 10, all yours: you owe 10 …
        val expenses = FakeExpenseRepository(listOf(expense("e", "pg2", paidBy = omar.id, total = 10_00, shares = mapOf(you.id to 10_00L))))
        // … until you pay him back.
        val settled = FakeSettlementRepository(listOf(Settlement("s", "trip", you.id, omar.id, Money(10_00, jod), today)))
        people.groups.value = listOf(PeopleGroup("pg2", "Family", listOf(you, omar), today))

        val summary = ObserveGroupsUseCase(trips, people, expenses, settled, profiles)().first().single()
        assertTrue(summary.yourBalance.isZero)
    }

    @Test
    fun `summaries are ordered by latest activity`() = runTest {
        trips.groups.value = listOf(ExpenseGroup("new", "New", today), ExpenseGroup("old", "Old", LocalDate(2026, 1, 1)))
        val expenses = FakeExpenseRepository(
            listOf(
                expense(
                    "e", "pg", paidBy = you.id, total = 1_00, shares = mapOf(you.id to 1_00L), trip = "old",
                    start = LocalDate(2026, 4, 28), end = LocalDate(2026, 4, 30),
                ),
            ),
        )
        val order = ObserveGroupsUseCase(trips, people, expenses, FakeSettlementRepository(), profiles)().first().map { it.id }
        assertEquals(listOf("old", "new"), order)
    }

    private fun expense(
        id: String,
        peopleGroupId: String,
        paidBy: String,
        total: Long,
        shares: Map<String, Long>,
        currency: CurrencyCode = jod,
        trip: String = "trip",
        start: LocalDate = today,
        end: LocalDate = start,
    ) = Expense(
        id = id,
        expenseGroupId = trip,
        peopleGroupId = peopleGroupId,
        title = id,
        amount = Money(total, currency),
        category = ExpenseCategory.Food,
        paidBy = paidBy,
        splitType = SplitType.Exact,
        shares = shares.map { (who, units) -> ExpenseShare(who, Money(units, currency)) },
        startDate = start,
        endDate = end,
        note = null,
    )
}
