package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseReportExporter
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExportResult
import com.abeerapps.kmpexpensesplitter.core.domain.repository.GroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.LocalDataCleaner
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
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
        val noGroups = object : GroupRepository {
            override fun observeGroups(): Flow<List<GroupSummary>> = flowOf(emptyList())
        }
        assertEquals(ExportResult.Failed, ExportExpenseReportUseCase(noGroups, exporter)())
        assertFalse(exporterCalled)

        val someGroups = object : GroupRepository {
            override fun observeGroups() = flowOf(
                listOf(GroupSummary("1", "Trip", listOf("A"), Money(1_00, jod), Money(0, jod))),
            )
        }
        assertEquals(ExportResult.Saved("r.pdf", "Downloads"), ExportExpenseReportUseCase(someGroups, exporter)())
    }
}
