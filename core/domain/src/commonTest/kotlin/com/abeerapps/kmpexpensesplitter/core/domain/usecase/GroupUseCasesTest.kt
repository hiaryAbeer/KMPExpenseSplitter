package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.Group
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakeExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakeGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GroupUseCasesTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val today = LocalDate(2026, 4, 29)
    private val profiles = object : UserProfileRepository {
        val profile = MutableStateFlow<UserProfile?>(UserProfile("Ahmad", jod))
        override fun observeProfile(): Flow<UserProfile?> = profile
        override suspend fun saveProfile(profile: UserProfile) { this.profile.value = profile }
    }
    private val groups = FakeGroupRepository()
    private var nextId = 0
    private val createGroup = CreateGroupUseCase(groups, profiles, { today }, newId = { "id${nextId++}" })

    private val you = Participant(Participant.YOU_ID, "Ahmad")

    @Test
    fun `creating a group adds you first and trims names`() = runTest {
        val result = createGroup("  Aqaba Trip ", listOf(" Sara ", "Omar"))

        val expected = Group(
            id = "id0",
            name = "Aqaba Trip",
            members = listOf(you, Participant("id1", "Sara"), Participant("id2", "Omar")),
            createdOn = today,
        )
        assertEquals(CreateGroupResult.Success(expected), result)
        assertEquals(listOf(expected), groups.groups.value)
    }

    @Test
    fun `a group needs a name and at least one member`() = runTest {
        assertEquals(
            CreateGroupResult.Invalid(listOf(GroupDraftError.BlankName, GroupDraftError.NoMembers)),
            createGroup(" ", emptyList(), includeYou = false),
        )
        assertTrue(groups.groups.value.isEmpty())
    }

    @Test
    fun `you alone are enough for a group`() = runTest {
        val result = createGroup("Personal", emptyList())
        assertEquals(listOf(you), (result as CreateGroupResult.Success).group.members)
    }

    @Test
    fun `a group can be created without you`() = runTest {
        val result = createGroup("Parents", listOf("Mom", "Dad"), includeYou = false)
        assertEquals(listOf("Mom", "Dad"), (result as CreateGroupResult.Success).group.members.map { it.name })
        // Without you, your name is free for another person.
        assertNull(createGroup.validateMemberName("Ahmad", emptyList(), yourName = null))
    }

    @Test
    fun `member names must be unique ignoring case including your name`() {
        assertEquals(MemberNameError.Duplicate, createGroup.validateMemberName("sara", listOf("Sara"), "Ahmad"))
        assertEquals(MemberNameError.Duplicate, createGroup.validateMemberName(" ahmad ", emptyList(), "Ahmad"))
        assertEquals(MemberNameError.Blank, createGroup.validateMemberName("  ", emptyList(), "Ahmad"))
        assertEquals(MemberNameError.TooLong, createGroup.validateMemberName("x".repeat(51), emptyList(), "Ahmad"))
        assertEquals(
            MemberNameError.TooMany,
            createGroup.validateMemberName("New", List(Group.MAX_OTHER_MEMBERS) { "P$it" }, "Ahmad"),
        )
        assertNull(createGroup.validateMemberName("Lina", listOf("Sara"), "Ahmad"))
        assertEquals(listOf(GroupDraftError.InvalidMembers), createGroup.validate("Trip", listOf("Sara", "SARA"), "Ahmad"))
    }

    @Test
    fun `storage failure is reported`() = runTest {
        groups.failNext = true
        assertEquals(CreateGroupResult.StorageError, createGroup("Trip", listOf("Sara")))
    }

    @Test
    fun `group list shows your current profile name`() = runTest {
        createGroup("Trip", listOf("Sara"))
        profiles.profile.value = UserProfile("Sara Ahmad", jod)

        val members = ObserveGroupListUseCase(groups, profiles)().first().single().members
        assertEquals(listOf("Sara Ahmad", "Sara"), members.map { it.name })
    }

    @Test
    fun `summaries total the group and compute your balance`() = runTest {
        val sara = Participant("s", "Sara")
        groups.groups.value = listOf(Group("g", "Trip", listOf(you, sara), today))
        val expenses = FakeExpenseRepository(
            listOf(
                // You paid 30, split equally: Sara owes you 15.
                expense("e1", paidBy = Participant.YOU_ID, total = 30_00, shares = mapOf(you.id to 15_00L, "s" to 15_00L)),
                // Sara paid 10, all yours: you owe 10.
                expense("e2", paidBy = "s", total = 10_00, shares = mapOf(you.id to 10_00L)),
                // Another currency: ignored.
                expense("e3", paidBy = "s", total = 99_00, shares = mapOf(you.id to 99_00L), currency = CurrencyCode.of("USD")!!),
            ),
        )

        val summary = ObserveGroupsUseCase(groups, expenses, profiles)().first().single()

        assertEquals(GroupSummary("g", "Trip", listOf("Ahmad", "Sara"), Money(40_00, jod), Money(5_00, jod)), summary)
    }

    @Test
    fun `summaries are ordered by latest activity`() = runTest {
        groups.groups.value = listOf(
            Group("new", "New", listOf(you), today),
            Group("old", "Old", listOf(you), LocalDate(2026, 1, 1)),
        )
        val expenses = FakeExpenseRepository(
            listOf(expense("e", paidBy = you.id, total = 1_00, shares = mapOf(you.id to 1_00L), groupId = "old", date = LocalDate(2026, 4, 30))),
        )
        val order = ObserveGroupsUseCase(groups, expenses, profiles)().first().map { it.id }
        assertEquals(listOf("old", "new"), order)
    }

    private fun expense(
        id: String,
        paidBy: String,
        total: Long,
        shares: Map<String, Long>,
        currency: CurrencyCode = jod,
        groupId: String = "g",
        date: LocalDate = today,
    ) = Expense(
        id = id,
        groupId = groupId,
        title = id,
        amount = Money(total, currency),
        category = ExpenseCategory.Food,
        paidBy = paidBy,
        splitType = SplitType.Exact,
        shares = shares.map { (who, units) -> ExpenseShare(who, Money(units, currency)) },
        date = date,
        note = null,
    )
}
