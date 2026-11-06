package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakeExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakePeopleGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeopleGroupUseCasesTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val today = LocalDate(2026, 4, 29)
    private val profiles = object : UserProfileRepository {
        val profile = MutableStateFlow<UserProfile?>(UserProfile("Ahmad", jod))
        override fun observeProfile(): Flow<UserProfile?> = profile
        override suspend fun saveProfile(profile: UserProfile) { this.profile.value = profile }
    }
    private val groups = FakePeopleGroupRepository()
    private var nextId = 0
    private val createGroup = CreatePeopleGroupUseCase(groups, profiles, { today }, newId = { "id${nextId++}" })

    private val you = Participant(Participant.YOU_ID, "Ahmad")

    @Test
    fun `creating a group adds you first and trims names`() = runTest {
        val result = createGroup("  Aqaba Trip ", listOf(" Sara ", "Omar"))

        val expected = PeopleGroup(
            id = "id0",
            name = "Aqaba Trip",
            members = listOf(you, Participant("id1", "Sara"), Participant("id2", "Omar")),
            createdOn = today,
        )
        assertEquals(CreatePeopleGroupResult.Success(expected), result)
        assertEquals(listOf(expected), groups.groups.value)
    }

    @Test
    fun `a group needs a name and at least one member`() = runTest {
        assertEquals(
            CreatePeopleGroupResult.Invalid(listOf(PeopleGroupDraftError.BlankName, PeopleGroupDraftError.NoMembers)),
            createGroup(" ", emptyList(), includeYou = false),
        )
        assertTrue(groups.groups.value.isEmpty())
    }

    @Test
    fun `you alone are enough for a group`() = runTest {
        val result = createGroup("Personal", emptyList())
        assertEquals(listOf(you), (result as CreatePeopleGroupResult.Success).group.members)
    }

    @Test
    fun `a group can be created without you`() = runTest {
        val result = createGroup("Parents", listOf("Mom", "Dad"), includeYou = false)
        assertEquals(listOf("Mom", "Dad"), (result as CreatePeopleGroupResult.Success).group.members.map { it.name })
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
            createGroup.validateMemberName("New", List(PeopleGroup.MAX_OTHER_MEMBERS) { "P$it" }, "Ahmad"),
        )
        assertNull(createGroup.validateMemberName("Lina", listOf("Sara"), "Ahmad"))
        assertEquals(listOf(PeopleGroupDraftError.InvalidMembers), createGroup.validate("Trip", listOf("Sara", "SARA"), "Ahmad"))
    }

    @Test
    fun `storage failure is reported`() = runTest {
        groups.failNext = true
        assertEquals(CreatePeopleGroupResult.StorageError, createGroup("Trip", listOf("Sara")))
    }

    @Test
    fun `group list shows your current profile name`() = runTest {
        createGroup("Trip", listOf("Sara"))
        profiles.profile.value = UserProfile("Sara Ahmad", jod)

        val members = ObservePeopleGroupsUseCase(groups, profiles)().first().single().members
        assertEquals(listOf("Sara Ahmad", "Sara"), members.map { it.name })
    }
}
