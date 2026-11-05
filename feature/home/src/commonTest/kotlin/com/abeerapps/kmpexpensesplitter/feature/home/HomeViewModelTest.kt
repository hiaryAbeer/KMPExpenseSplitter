package com.abeerapps.kmpexpensesplitter.feature.home

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.Group
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.GroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CalculateBalanceSummaryUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveUserProfileUseCase
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
class HomeViewModelTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val profiles = FakeProfiles(UserProfile("Ahmad", jod))
    private val groups = FakeGroups()
    private val expenses = FakeExpenses()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = HomeViewModel(
        ObserveUserProfileUseCase(profiles),
        ObserveGroupsUseCase(groups, expenses, profiles),
        CalculateBalanceSummaryUseCase(),
    )

    private val you = Participant(Participant.YOU_ID, "Ahmad")
    private val sara = Participant("s", "Sara")

    /** A group with you and Sara whose single expense leaves you with [balance]. */
    private fun group(id: String, name: String, balance: Long): Group {
        if (balance != 0L) {
            val (payer, debtor) = if (balance > 0) you.id to sara.id else sara.id to you.id
            val amount = Money(kotlin.math.abs(balance), jod)
            expenses.expenses.value += Expense(
                id = "e$id", groupId = id, title = "x", amount = amount, category = ExpenseCategory.Food,
                paidBy = payer, splitType = SplitType.Exact, shares = listOf(ExpenseShare(debtor, amount)),
                date = LocalDate(2026, 4, 29), note = null,
            )
        }
        return Group(id, name, listOf(you, sara), LocalDate(2026, 4, 1))
    }

    /** Skips the initial loading state (it may be conflated away entirely). */
    private suspend fun ReceiveTurbine<HomeUiState>.awaitLoaded(): HomeUiState {
        var state = awaitItem()
        while (state.isLoading) state = awaitItem()
        return state
    }

    @Test
    fun `initial state is loading`() {
        assertTrue(HomeUiState().isLoading)
        assertFalse(HomeUiState().hasNoGroups) // No empty-state flash while loading.
    }

    @Test
    fun `shows greeting and empty state with zero balances`() = runTest {
        viewModel().uiState.test {
            val state = awaitLoaded()
            assertFalse(state.isLoading)
            assertEquals("Ahmad", state.userName)
            assertEquals(Money.zero(jod), state.owedToYou)
            assertEquals(Money.zero(jod), state.youOwe)
            assertEquals(0, state.totalGroupCount)
            assertTrue(state.hasNoGroups)
            assertFalse(state.hasNoSearchResults)
        }
    }

    @Test
    fun `balances use the user's default currency`() = runTest {
        val usd = CurrencyCode.of("USD")!!
        profiles.profile.value = UserProfile("Ahmad", usd)

        viewModel().uiState.test {
            assertEquals(usd, awaitLoaded().owedToYou.currency)
        }
    }

    @Test
    fun `groups populate the list and totals`() = runTest {
        groups.groups.value = listOf(group("1", "Aqaba Trip", 85_25), group("2", "Apartment Bills", -45_00))

        viewModel().uiState.test {
            val state = awaitLoaded()
            assertEquals(2, state.groups.size)
            assertEquals(2, state.totalGroupCount)
            assertEquals(Money(85_25, jod), state.owedToYou)
            assertEquals(Money(45_00, jod), state.youOwe)
            assertFalse(state.hasNoGroups)
        }
    }

    @Test
    fun `search filters by name case-insensitively and keeps the total count`() = runTest {
        groups.groups.value = listOf(group("1", "Aqaba Trip", 0), group("2", "Apartment Bills", 0))
        val vm = viewModel()

        vm.uiState.test {
            awaitLoaded()
            vm.onSearchQueryChange("aqaba")
            val state = awaitItem()
            assertEquals(listOf("Aqaba Trip"), state.groups.map { it.name })
            assertEquals(2, state.totalGroupCount)
            assertEquals("aqaba", vm.searchQuery.value)
        }
    }

    @Test
    fun `search with no match shows no-results instead of empty state`() = runTest {
        groups.groups.value = listOf(group("1", "Aqaba Trip", 0))
        val vm = viewModel()

        vm.uiState.test {
            awaitLoaded()
            vm.onSearchQueryChange("zzz")
            val state = awaitItem()
            assertTrue(state.hasNoSearchResults)
            assertFalse(state.hasNoGroups)
        }
    }

    private class FakeProfiles(initial: UserProfile?) : UserProfileRepository {
        val profile = MutableStateFlow(initial)
        override fun observeProfile(): Flow<UserProfile?> = profile
        override suspend fun saveProfile(profile: UserProfile) {
            this.profile.value = profile
        }
    }

    private class FakeGroups : GroupRepository {
        val groups = MutableStateFlow<List<Group>>(emptyList())
        override fun observeGroups(): Flow<List<Group>> = groups
        override suspend fun createGroup(group: Group) { groups.value = listOf(group) + groups.value }
    }

    private class FakeExpenses : ExpenseRepository {
        val expenses = MutableStateFlow<List<Expense>>(emptyList())
        override fun observeExpenses(): Flow<List<Expense>> = expenses
        override suspend fun addExpense(expense: Expense) { expenses.value += expense }
    }
}
