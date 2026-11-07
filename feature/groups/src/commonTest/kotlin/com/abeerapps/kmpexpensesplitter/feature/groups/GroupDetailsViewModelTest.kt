package com.abeerapps.kmpexpensesplitter.feature.groups

import androidx.lifecycle.SavedStateHandle
import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseGroupDetailsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.SettleUpUseCase
import com.abeerapps.kmpexpensesplitter.feature.groups.details.DetailsTab
import com.abeerapps.kmpexpensesplitter.feature.groups.details.GroupDetailsAction
import com.abeerapps.kmpexpensesplitter.feature.groups.details.GroupDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
class GroupDetailsViewModelTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val today = LocalDate(2026, 4, 29)
    private val you = Participant(Participant.YOU_ID, "Ahmad")
    private val sara = Participant("s", "Sara")

    private val trips = MutableStateFlow(listOf(ExpenseGroup("t1", "Aqaba Trip", today)))
    private val tripRepo = object : ExpenseGroupRepository {
        override fun observeExpenseGroups(): Flow<List<ExpenseGroup>> = trips
        override suspend fun createExpenseGroup(group: ExpenseGroup) = Unit
        override suspend fun updateExpenseGroup(group: ExpenseGroup) = Unit
        override suspend fun deleteExpenseGroup(groupId: String) { trips.value = trips.value.filterNot { it.id == groupId } }
    }
    private val peopleRepo = object : PeopleGroupRepository {
        override fun observeGroups(): Flow<List<PeopleGroup>> = MutableStateFlow(listOf(PeopleGroup("pg", "Friends", listOf(you, sara), today)))
        override suspend fun createGroup(group: PeopleGroup) = Unit
        override suspend fun updateGroup(group: PeopleGroup) = Unit
        override suspend fun deleteGroup(groupId: String) = Unit
    }
    private val expenseRepo = object : ExpenseRepository {
        override fun observeExpenses(): Flow<List<Expense>> = MutableStateFlow(
            listOf(
                Expense(
                    id = "e1", expenseGroupId = "t1", peopleGroupId = "pg", title = "Hotel", amount = Money(90_00, jod),
                    category = ExpenseCategory.Hotel, paidBy = sara.id, splitType = SplitType.Equal,
                    shares = listOf(ExpenseShare(sara.id, Money(45_00, jod)), ExpenseShare(you.id, Money(45_00, jod))),
                    startDate = today, endDate = today, note = null,
                ),
            ),
        )
        override suspend fun addExpense(expense: Expense) = Unit
        override suspend fun updateExpense(expense: Expense) = Unit
        override suspend fun deleteExpense(expenseId: String) = Unit
        override suspend fun deleteExpensesIn(expenseGroupId: String) = Unit
    }
    private val settlements = FakeSettlementRepository()
    private val profiles = object : UserProfileRepository {
        override fun observeProfile(): Flow<UserProfile?> = MutableStateFlow(UserProfile("Ahmad", jod))
        override suspend fun saveProfile(profile: UserProfile) = Unit
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(groupId: String = "t1") = GroupDetailsViewModel(
        observeDetails = ObserveExpenseGroupDetailsUseCase(tripRepo, peopleRepo, expenseRepo, settlements, profiles),
        settleUp = SettleUpUseCase(settlements, { today }, newId = { "s${settlements.settlements.value.size}" }),
        dateProvider = DateProvider { today },
        savedStateHandle = SavedStateHandle(mapOf(GroupDetailsViewModel.ARG_GROUP_ID to groupId)),
    )

    @Test
    fun `shows the group's expenses with payer names and balances`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val details = vm.uiState.value.details!!
        assertEquals("Aqaba Trip", details.group.name)
        assertEquals(listOf("Hotel" to "Sara"), details.expenses.map { it.expense.title to it.payerName })
        assertEquals(listOf("Sara" to 45_00L, "Ahmad" to -45_00L), details.balances.map { it.name to it.balance.minorUnits })
        assertEquals(DetailsTab.Expenses, vm.uiState.value.tab)
        assertTrue(vm.uiState.value.showAddExpense)

        vm.onAction(GroupDetailsAction.TabSelected(DetailsTab.Stats))
        assertEquals(DetailsTab.Stats, vm.uiState.value.tab)
        assertFalse(vm.uiState.value.showAddExpense) // The "+" button is for the Expenses tab only.

        // Balances ends with Settle Up instead of the "+" button.
        vm.onAction(GroupDetailsAction.TabSelected(DetailsTab.Balances))
        assertFalse(vm.uiState.value.showAddExpense)
    }

    @Test
    fun `marking as settled records the payments and hides Settle Up`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertTrue(vm.uiState.value.canSettleUp)
        assertEquals(listOf(Triple("Ahmad", "Sara", 45_00L)), vm.uiState.value.details!!.transfers.map { Triple(it.fromName, it.toName, it.amount.minorUnits) })

        vm.onAction(GroupDetailsAction.SettleUpClicked)
        assertTrue(vm.uiState.value.showSettleUp)
        vm.onAction(GroupDetailsAction.MarkSettledClicked)

        val state = vm.uiState.value
        assertFalse(state.showSettleUp)
        assertFalse(state.canSettleUp)
        assertEquals(listOf(0L, 0L), state.details!!.balances.map { it.balance.minorUnits })
        assertEquals(listOf(Triple(you.id, sara.id, 45_00L)), settlements.settlements.value.map { Triple(it.fromId, it.toId, it.amount.minorUnits) })

        // Nothing left to settle: the button does nothing.
        vm.onAction(GroupDetailsAction.SettleUpClicked)
        assertFalse(vm.uiState.value.showSettleUp)
    }

    @Test
    fun `a failed settle up keeps the sheet open and reports the error once`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        settlements.failNext = true

        vm.onAction(GroupDetailsAction.SettleUpClicked)
        vm.onAction(GroupDetailsAction.MarkSettledClicked)
        assertTrue(vm.uiState.value.showSettleUp)
        assertTrue(vm.uiState.value.settleFailed)
        assertTrue(vm.uiState.value.canSettleUp)

        vm.onAction(GroupDetailsAction.MessageShown)
        assertFalse(vm.uiState.value.settleFailed)
    }

    @Test
    fun `a deleted group is reported as missing so the screen closes`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertFalse(vm.uiState.value.isMissing)

        tripRepo.deleteExpenseGroup("t1")
        assertTrue(vm.uiState.value.isMissing)
    }
}
