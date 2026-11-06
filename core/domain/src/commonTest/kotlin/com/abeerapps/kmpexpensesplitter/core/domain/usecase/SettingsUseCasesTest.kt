package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseReportExporter
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExportResult
import com.abeerapps.kmpexpensesplitter.core.domain.repository.LocalDataCleaner
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakeExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakeExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakePeopleGroupRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsUseCasesTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val eur = CurrencyCode.of("EUR")!!

    private class FakeProfiles(initial: UserProfile?) : UserProfileRepository {
        val profile = MutableStateFlow(initial)
        var failOnSave = false
        override fun observeProfile(): Flow<UserProfile?> = profile
        override suspend fun saveProfile(profile: UserProfile) {
            if (failOnSave) error("disk full")
            this.profile.value = profile
        }
    }

    @Test
    fun `update name trims and keeps currency`() = runTest {
        val repo = FakeProfiles(UserProfile("Ahmad", jod))
        assertEquals(ProfileUpdateResult.Success, UpdateUserProfileUseCase(repo).updateName("  Sara "))
        assertEquals(UserProfile("Sara", jod), repo.profile.value)
    }

    @Test
    fun `invalid name is rejected without saving`() = runTest {
        val repo = FakeProfiles(UserProfile("Ahmad", jod))
        assertEquals(ProfileUpdateResult.InvalidName, UpdateUserProfileUseCase(repo).updateName("  "))
        assertEquals("Ahmad", repo.profile.value?.name)
    }

    @Test
    fun `update currency keeps name`() = runTest {
        val repo = FakeProfiles(UserProfile("Ahmad", jod))
        assertEquals(ProfileUpdateResult.Success, UpdateUserProfileUseCase(repo).updateDefaultCurrency(eur))
        assertEquals(UserProfile("Ahmad", eur), repo.profile.value)
    }

    @Test
    fun `update without a profile reports NoProfile`() = runTest {
        assertEquals(ProfileUpdateResult.NoProfile, UpdateUserProfileUseCase(FakeProfiles(null)).updateName("Sara"))
    }

    @Test
    fun `storage failure is reported`() = runTest {
        val repo = FakeProfiles(UserProfile("Ahmad", jod)).apply { failOnSave = true }
        assertEquals(ProfileUpdateResult.StorageError, UpdateUserProfileUseCase(repo).updateName("Sara"))
    }

    @Test
    fun `clear local data reports success or failure`() = runTest {
        var cleared = false
        assertTrue(ClearLocalDataUseCase(object : LocalDataCleaner {
            override suspend fun clearAll() { cleared = true }
        })())
        assertTrue(cleared)
        assertFalse(ClearLocalDataUseCase(object : LocalDataCleaner {
            override suspend fun clearAll() = error("io")
        })())
    }

    @Test
    fun `export is refused when there are no groups`() = runTest {
        var exporterCalled = false
        val exporter = object : ExpenseReportExporter {
            override suspend fun export(groups: List<GroupSummary>): ExportResult {
                exporterCalled = true
                return ExportResult.Saved("r.pdf", "Downloads")
            }
        }
        val groups = FakeExpenseGroupRepository()
        val observeGroups = ObserveGroupsUseCase(
            groups, FakePeopleGroupRepository(), FakeExpenseRepository(), FakeProfiles(UserProfile("Ahmad", jod)),
        )
        assertEquals(ExportResult.Failed, ExportExpenseReportUseCase(observeGroups, exporter)())
        assertFalse(exporterCalled)

        groups.groups.value = listOf(ExpenseGroup("1", "Trip", LocalDate(2026, 4, 1)))
        assertEquals(ExportResult.Saved("r.pdf", "Downloads"), ExportExpenseReportUseCase(observeGroups, exporter)())
    }
}
