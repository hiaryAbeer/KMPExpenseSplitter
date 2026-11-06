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
import com.abeerapps.kmpexpensesplitter.feature.groups.details.DetailsTab
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
        override suspend fun deleteExpensesIn(expenseGroupId: String) = Unit
    }
    private val profiles = object : UserProfileRepository {
        override fun observeProfile(): Flow<UserProfile?> = MutableStateFlow(UserProfile("Ahmad", jod))
        override suspend fun saveProfile(profile: UserProfile) = Unit
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(groupId: String = "t1") = GroupDetailsViewModel(
        observeDetails = ObserveExpenseGroupDetailsUseCase(tripRepo, peopleRepo, expenseRepo, profiles),
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

        vm.onTabSelected(DetailsTab.Stats)
        assertEquals(DetailsTab.Stats, vm.uiState.value.tab)
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
