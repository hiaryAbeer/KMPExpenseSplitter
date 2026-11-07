package com.abeerapps.kmpexpensesplitter.core.domain.usecase

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
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.model.countByExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.peopleGroupUsage
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakeExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakeExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakePeopleGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakeSettlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupManagementUseCasesTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val today = LocalDate(2026, 4, 29)
    private val you = Participant(Participant.YOU_ID, "Ahmad")
    private val sara = Participant("s", "Sara")
    private val omar = Participant("o", "Omar")
    private val profiles = object : UserProfileRepository {
        override fun observeProfile(): Flow<UserProfile?> = MutableStateFlow(UserProfile("Ahmad", jod))
        override suspend fun saveProfile(profile: UserProfile) = Unit
    }

    private val friends = PeopleGroup("pg", "Friends", listOf(you, sara, omar), today)
    private val people = FakePeopleGroupRepository(listOf(friends))
    private val trips = FakeExpenseGroupRepository(listOf(ExpenseGroup("t1", "Aqaba", today), ExpenseGroup("t2", "Petra", today)))
    private val expenses = FakeExpenseRepository()
    private var nextId = 0
    private val update = UpdatePeopleGroupUseCase(people, expenses, profiles, newId = { "n${nextId++}" })

    /** Sara paid for herself and you, in [trip]. Omar is not referenced. */
    private fun saraPaid(trip: String = "t1", id: String = "e1") = Expense(
        id = id, expenseGroupId = trip, peopleGroupId = "pg", title = "x", amount = Money(10_00, jod),
        category = ExpenseCategory.Food, paidBy = sara.id, splitType = SplitType.Equal,
        shares = listOf(ExpenseShare(sara.id, Money(5_00, jod)), ExpenseShare(you.id, Money(5_00, jod))),
        startDate = today, endDate = today, note = null,
    )

    @Test
    fun `usage lists the people each people group's expenses reference`() {
        val usage = listOf(saraPaid()).peopleGroupUsage()
        assertEquals(mapOf("pg" to setOf(sara.id, you.id)), usage)
        assertEquals(mapOf("t1" to 2, "t2" to 1), listOf(saraPaid(), saraPaid(id = "e2"), saraPaid("t2", "e3")).countByExpenseGroup())
    }

    @Test
    fun `editing keeps existing member ids and adds new members`() = runTest {
        val result = update("pg", " Best Friends ", listOf(MemberDraft("s", "Sara"), MemberDraft("o", "Omar"), MemberDraft(null, "Lina")), includeYou = true)

        val group = (result as UpdatePeopleGroupResult.Success).group
        assertEquals("Best Friends", group.name)
        assertEquals(listOf(you, sara, omar, Participant("n0", "Lina")), group.members)
        assertEquals(group, people.groups.value.single())
    }

    @Test
    fun `members referenced by expenses cannot be removed but others can`() = runTest {
        expenses.expenses.value = listOf(saraPaid())

        assertEquals(
            UpdatePeopleGroupResult.MemberInUse,
            update("pg", "Friends", listOf(MemberDraft("o", "Omar")), includeYou = true), // Removes Sara.
        )
        assertEquals(
            UpdatePeopleGroupResult.MemberInUse,
            update("pg", "Friends", listOf(MemberDraft("s", "Sara"), MemberDraft("o", "Omar")), includeYou = false), // Removes you.
        )
        val ok = update("pg", "Friends", listOf(MemberDraft("s", "Sara")), includeYou = true) // Removes Omar.
        assertEquals(listOf(you, sara), (ok as UpdatePeopleGroupResult.Success).group.members)
    }

    @Test
    fun `edits follow the same rules as creating`() = runTest {
        assertEquals(
            UpdatePeopleGroupResult.Invalid(listOf(PeopleGroupDraftError.BlankName, PeopleGroupDraftError.NoMembers)),
            update("pg", " ", emptyList(), includeYou = false),
        )
        assertEquals(UpdatePeopleGroupResult.NotFound, update("missing", "X", emptyList(), includeYou = true))
    }

    @Test
    fun `a people group can be deleted only when no expense uses it`() = runTest {
        val delete = DeletePeopleGroupUseCase(people, expenses)
        expenses.expenses.value = listOf(saraPaid())
        assertEquals(DeleteResult.InUse, delete("pg"))
        assertEquals(listOf("pg"), people.groups.value.map { it.id })

        expenses.expenses.value = emptyList()
        assertEquals(DeleteResult.Deleted, delete("pg"))
        assertTrue(people.groups.value.isEmpty())
    }

    @Test
    fun `renaming an expense group keeps its id and validates the name`() = runTest {
        val rename = RenameExpenseGroupUseCase(trips)
        assertEquals(RenameExpenseGroupResult.Success(ExpenseGroup("t1", "Aqaba Trip", today)), rename("t1", " Aqaba Trip "))
        assertEquals(RenameExpenseGroupResult.Invalid(ExpenseGroupNameError.Blank), rename("t1", "  "))
        assertEquals(RenameExpenseGroupResult.NotFound, rename("nope", "X"))
    }

    @Test
    fun `deleting an expense group deletes only its expenses and settlements`() = runTest {
        expenses.expenses.value = listOf(saraPaid("t1", "e1"), saraPaid("t2", "e2"))
        val settled = FakeSettlementRepository(
            listOf("t1", "t2").map { Settlement("s-$it", it, Participant.YOU_ID, "s", Money(1_00, jod), today) },
        )
        assertEquals(DeleteResult.Deleted, DeleteExpenseGroupUseCase(trips, expenses, settled)("t1"))
        assertEquals(listOf("t2"), trips.groups.value.map { it.id })
        assertEquals(listOf("e2"), expenses.expenses.value.map { it.id })
        assertEquals(listOf("t2"), settled.settlements.value.map { it.expenseGroupId })
    }
}
