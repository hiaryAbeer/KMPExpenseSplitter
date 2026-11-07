package com.abeerapps.kmpexpensesplitter.feature.groups

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupFilter
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveGroupsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModelTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val today = LocalDate(2026, 4, 29)
    private val you = Participant(Participant.YOU_ID, "Ahmad")
    private val sara = Participant("s", "Sara")

    private val trips = FakeTrips()
    private val people = FakePeople(listOf(PeopleGroup("pg", "Friends", listOf(you, sara), today)))
    private val expenses = FakeExpenses()
    private val profiles = object : UserProfileRepository {
        override fun observeProfile(): Flow<UserProfile?> = MutableStateFlow(UserProfile("Ahmad", jod))
        override suspend fun saveProfile(profile: UserProfile) = Unit
    }
    private val dateProvider = DateProvider { today }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = GroupsViewModel(
        observeGroups = ObserveGroupsUseCase(trips, people, expenses, FakeSettlementRepository(), profiles),
        dateProvider = dateProvider,
    )

    /** Adds an expense group whose single expense (with Sara) leaves you with [balance]. */
    private fun trip(id: String, name: String, balance: Long) {
        trips.groups.value = trips.groups.value + ExpenseGroup(id, name, today)
        if (balance == 0L) return
        val (payer, debtor) = if (balance > 0) you.id to sara.id else sara.id to you.id
        val amount = Money(kotlin.math.abs(balance), jod)
        expenses.items.value += Expense(
            id = "e$id", expenseGroupId = id, peopleGroupId = "pg", title = "x", amount = amount,
            category = ExpenseCategory.Food, paidBy = payer, splitType = SplitType.Exact,
            shares = listOf(ExpenseShare(debtor, amount)), startDate = today, endDate = today, note = null,
        )
    }

    private suspend fun ReceiveTurbine<GroupsUiState>.awaitLoaded(): GroupsUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }

    @Test
    fun `no expense groups shows the empty state`() = runTest {
        viewModel().uiState.test {
            val state = awaitLoaded()
            assertTrue(state.hasNoGroups)
            assertFalse(state.hasNoResults)
            assertEquals(GroupFilter.All, state.filter)
        }
    }

    @Test
    fun `lists every expense group not just three`() = runTest {
        repeat(5) { trip("t$it", "Trip $it", 0) }
        viewModel().uiState.test {
            assertEquals(5, awaitLoaded().groups.size)
        }
    }

    @Test
    fun `search matches the expense group name`() = runTest {
        trip("1", "Aqaba Trip", 0)
        trip("2", "Apartment Bills", 0)
        val vm = viewModel()
        vm.uiState.test {
            awaitLoaded()
            vm.onAction(GroupsAction.SearchChanged("aqaba"))
            assertEquals(listOf("Aqaba Trip"), awaitItem().groups.map { it.name })
            vm.onAction(GroupsAction.SearchChanged("zzz"))
            val none = awaitItem()
            assertTrue(none.hasNoResults)
            assertEquals(2, none.totalCount)
        }
    }

    @Test
    fun `filters by your balance`() = runTest {
        trip("1", "Aqaba Trip", 85_25)
        trip("2", "Apartment Bills", -45_00)
        trip("3", "Dinner", 0)
        val vm = viewModel()
        vm.uiState.test {
            awaitLoaded()
            vm.onAction(GroupsAction.FilterSelected(GroupFilter.YouOwe))
            assertEquals(listOf("Apartment Bills"), awaitItem().groups.map { it.name })
            vm.onAction(GroupsAction.FilterSelected(GroupFilter.YouAreOwed))
            assertEquals(listOf("Aqaba Trip"), awaitItem().groups.map { it.name })
            vm.onAction(GroupsAction.FilterSelected(GroupFilter.Settled))
            assertEquals(listOf("Dinner"), awaitItem().groups.map { it.name })
        }
    }

    private class FakeTrips : ExpenseGroupRepository {
        override suspend fun updateExpenseGroup(group: ExpenseGroup) = Unit
        override suspend fun deleteExpenseGroup(groupId: String) = Unit
        val groups = MutableStateFlow<List<ExpenseGroup>>(emptyList())
        override fun observeExpenseGroups(): Flow<List<ExpenseGroup>> = groups
        override suspend fun createExpenseGroup(group: ExpenseGroup) { groups.value = listOf(group) + groups.value }
    }

    private class FakePeople(initial: List<PeopleGroup>) : PeopleGroupRepository {
        override suspend fun updateGroup(group: PeopleGroup) = Unit
        override suspend fun deleteGroup(groupId: String) = Unit
        val groups = MutableStateFlow(initial)
        override fun observeGroups(): Flow<List<PeopleGroup>> = groups
        override suspend fun createGroup(group: PeopleGroup) { groups.value = listOf(group) + groups.value }
    }

    private class FakeExpenses : ExpenseRepository {
        override suspend fun updateExpense(expense: Expense) = Unit
        override suspend fun deleteExpense(expenseId: String) = Unit
        override suspend fun deleteExpensesIn(expenseGroupId: String) = Unit
        val items = MutableStateFlow<List<Expense>>(emptyList())
        override fun observeExpenses(): Flow<List<Expense>> = items
        override suspend fun addExpense(expense: Expense) { items.value += expense }
    }
}
