package com.abeerapps.kmpexpensesplitter.feature.groups

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
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreateExpenseGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreatePeopleGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.DeleteExpenseGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.DeletePeopleGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpensesUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObservePeopleGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveUserProfileUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.RenameExpenseGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.UpdatePeopleGroupUseCase
import com.abeerapps.kmpexpensesplitter.feature.groups.manage.ManageExpenseGroupsAction
import com.abeerapps.kmpexpensesplitter.feature.groups.manage.ManageExpenseGroupsViewModel
import com.abeerapps.kmpexpensesplitter.feature.groups.manage.ManagePeopleGroupsAction
import com.abeerapps.kmpexpensesplitter.feature.groups.manage.ManagePeopleGroupsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ManageGroupsViewModelTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val today = LocalDate(2026, 4, 29)
    private val you = Participant(Participant.YOU_ID, "Ahmad")
    private val sara = Participant("s", "Sara")
    private val omar = Participant("o", "Omar")

    private val trips = Trips(listOf(ExpenseGroup("t1", "Aqaba Trip", today), ExpenseGroup("t2", "Petra", today)))
    private val people = People(
        listOf(
            PeopleGroup("pg1", "Friends", listOf(you, sara, omar), today),
            PeopleGroup("pg2", "Family", listOf(you, omar), today),
        ),
    )
    private val expenses = Expenses(
        listOf(
            // Friends is used (Sara paid for herself and you); Family is not.
            Expense(
                id = "e1", expenseGroupId = "t1", peopleGroupId = "pg1", title = "Lunch", amount = Money(10_00, jod),
                category = ExpenseCategory.Food, paidBy = sara.id, splitType = SplitType.Equal,
                shares = listOf(ExpenseShare(sara.id, Money(5_00, jod)), ExpenseShare(you.id, Money(5_00, jod))),
                startDate = today, endDate = today, note = null,
            ),
        ),
    )
    private val profiles = object : UserProfileRepository {
        override fun observeProfile(): Flow<UserProfile?> = MutableStateFlow(UserProfile("Ahmad", jod))
        override suspend fun saveProfile(profile: UserProfile) = Unit
    }
    private val dateProvider = DateProvider { today }
    private var nextId = 0

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun expenseGroupsVm() = ManageExpenseGroupsViewModel(
        observeExpenseGroups = ObserveExpenseGroupsUseCase(trips),
        observeExpenses = ObserveExpensesUseCase(expenses),
        createExpenseGroup = CreateExpenseGroupUseCase(trips, dateProvider, newId = { "n${nextId++}" }),
        renameExpenseGroup = RenameExpenseGroupUseCase(trips),
        deleteExpenseGroup = DeleteExpenseGroupUseCase(trips, expenses),
    )

    private fun peopleGroupsVm() = ManagePeopleGroupsViewModel(
        observePeopleGroups = ObservePeopleGroupsUseCase(people, profiles),
        observeExpenses = ObserveExpensesUseCase(expenses),
        observeUserProfile = ObserveUserProfileUseCase(profiles),
        createPeopleGroup = CreatePeopleGroupUseCase(people, profiles, dateProvider, newId = { "n${nextId++}" }),
        updatePeopleGroup = UpdatePeopleGroupUseCase(people, expenses, profiles, newId = { "m${nextId++}" }),
        deletePeopleGroup = DeletePeopleGroupUseCase(people, expenses),
    )

    /** Keeps the state flow collected for the test (it's WhileSubscribed). */
    private fun TestScope.collect(vm: ManageExpenseGroupsViewModel) = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    private fun TestScope.collect(vm: ManagePeopleGroupsViewModel) = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    @Test
    fun `expense groups show their expense counts and filter by search`() = runTest {
        val vm = expenseGroupsVm().also { collect(it) }
        assertEquals(listOf(1, 0), vm.uiState.value.items.map { it.expenseCount })

        vm.onAction(ManageExpenseGroupsAction.SearchChanged("pet"))
        assertEquals(listOf("Petra"), vm.uiState.value.items.map { it.group.name })
    }

    @Test
    fun `expense groups can be added and renamed with the shared sheet`() = runTest {
        val vm = expenseGroupsVm().also { collect(it) }
        vm.onAction(ManageExpenseGroupsAction.AddClicked)
        vm.onAction(ManageExpenseGroupsAction.NameChanged("Wadi Rum"))
        vm.onAction(ManageExpenseGroupsAction.SaveClicked)
        assertNull(vm.uiState.value.editor)
        assertEquals("Wadi Rum", trips.groups.value.first().name)

        vm.onAction(ManageExpenseGroupsAction.EditClicked("t2"))
        assertEquals("Petra", vm.uiState.value.editor?.name)
        vm.onAction(ManageExpenseGroupsAction.NameChanged("Petra & Wadi Musa"))
        vm.onAction(ManageExpenseGroupsAction.SaveClicked)
        assertEquals("Petra & Wadi Musa", trips.groups.value.first { it.id == "t2" }.name)
    }

    @Test
    fun `deleting an expense group asks first and removes its expenses`() = runTest {
        val vm = expenseGroupsVm().also { collect(it) }
        vm.onAction(ManageExpenseGroupsAction.DeleteClicked("t1"))
        assertEquals("t1", vm.uiState.value.pendingDelete?.group?.id)
        assertEquals(2, trips.groups.value.size) // Nothing deleted before confirming.

        vm.onAction(ManageExpenseGroupsAction.DeleteConfirmed)
        assertNull(vm.uiState.value.pendingDelete)
        assertEquals(listOf("t2"), trips.groups.value.map { it.id })
        assertTrue(expenses.items.value.isEmpty())
    }

    @Test
    fun `people groups used by expenses cannot be deleted`() = runTest {
        val vm = peopleGroupsVm().also { collect(it) }
        val (friends, family) = vm.uiState.value.items
        assertTrue(friends.isUsed)
        assertFalse(family.isUsed)

        vm.onAction(ManagePeopleGroupsAction.DeleteClicked("pg1"))
        assertNull(vm.uiState.value.pendingDelete)

        vm.onAction(ManagePeopleGroupsAction.DeleteClicked("pg2"))
        vm.onAction(ManagePeopleGroupsAction.DeleteConfirmed)
        assertEquals(listOf("pg1"), people.groups.value.map { it.id })
    }

    @Test
    fun `people group search matches group or member names`() = runTest {
        val vm = peopleGroupsVm().also { collect(it) }
        vm.onAction(ManagePeopleGroupsAction.SearchChanged("sara"))
        assertEquals(listOf("Friends"), vm.uiState.value.items.map { it.group.name })
        vm.onAction(ManagePeopleGroupsAction.SearchChanged("fam"))
        assertEquals(listOf("Family"), vm.uiState.value.items.map { it.group.name })
    }

    @Test
    fun `editing a used people group locks referenced people and saves changes`() = runTest {
        val vm = peopleGroupsVm().also { collect(it) }
        vm.onAction(ManagePeopleGroupsAction.EditClicked("pg1"))
        val form = vm.uiState.value.form!!
        assertTrue(form.isEditing)
        assertFalse(form.canRemoveYou)
        assertFalse(form.canRemove(form.members[0])) // Sara is in an expense.

        vm.onAction(ManagePeopleGroupsAction.MemberRemoved(1)) // Omar: unused, removable.
        vm.onAction(ManagePeopleGroupsAction.MemberInputChanged("Lina"))
        vm.onAction(ManagePeopleGroupsAction.AddMemberClicked)
        vm.onAction(ManagePeopleGroupsAction.SaveClicked)

        assertNull(vm.uiState.value.form)
        assertEquals(listOf("Ahmad", "Sara", "Lina"), people.groups.value.first { it.id == "pg1" }.members.map { it.name })
    }

    @Test
    fun `people groups can be added with the shared sheet`() = runTest {
        val vm = peopleGroupsVm().also { collect(it) }
        vm.onAction(ManagePeopleGroupsAction.AddClicked)
        vm.onAction(ManagePeopleGroupsAction.NameChanged("Work"))
        vm.onAction(ManagePeopleGroupsAction.MemberInputChanged("Ali"))
        vm.onAction(ManagePeopleGroupsAction.SaveClicked)
        assertEquals(listOf("Ahmad", "Ali"), people.groups.value.first().members.map { it.name })
    }

    private class Trips(initial: List<ExpenseGroup>) : ExpenseGroupRepository {
        val groups = MutableStateFlow(initial)
        override fun observeExpenseGroups(): Flow<List<ExpenseGroup>> = groups
        override suspend fun createExpenseGroup(group: ExpenseGroup) { groups.value = listOf(group) + groups.value }
        override suspend fun updateExpenseGroup(group: ExpenseGroup) { groups.value = groups.value.map { if (it.id == group.id) group else it } }
        override suspend fun deleteExpenseGroup(groupId: String) { groups.value = groups.value.filterNot { it.id == groupId } }
    }

    private class People(initial: List<PeopleGroup>) : PeopleGroupRepository {
        val groups = MutableStateFlow(initial)
        override fun observeGroups(): Flow<List<PeopleGroup>> = groups
        override suspend fun createGroup(group: PeopleGroup) { groups.value = listOf(group) + groups.value }
        override suspend fun updateGroup(group: PeopleGroup) { groups.value = groups.value.map { if (it.id == group.id) group else it } }
        override suspend fun deleteGroup(groupId: String) { groups.value = groups.value.filterNot { it.id == groupId } }
    }

    private class Expenses(initial: List<Expense>) : ExpenseRepository {
        val items = MutableStateFlow(initial)
        override fun observeExpenses(): Flow<List<Expense>> = items
        override suspend fun addExpense(expense: Expense) { items.value += expense }
        override suspend fun deleteExpensesIn(expenseGroupId: String) { items.value = items.value.filterNot { it.expenseGroupId == expenseGroupId } }
    }
}
