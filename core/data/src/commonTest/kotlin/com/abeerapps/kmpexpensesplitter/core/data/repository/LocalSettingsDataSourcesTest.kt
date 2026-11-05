package com.abeerapps.kmpexpensesplitter.core.data.repository

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.abeerapps.kmpexpensesplitter.core.data.testutil.InMemoryDataStore
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.ThemePreference
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExportResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalSettingsDataSourcesTest {

    @Test
    fun `theme defaults to System and persists changes`() = runTest {
        val repo = DataStoreAppPreferencesRepository(InMemoryDataStore())
        repo.observeTheme().test {
            assertEquals(ThemePreference.System, awaitItem())
            repo.setTheme(ThemePreference.Dark)
            assertEquals(ThemePreference.Dark, awaitItem())
        }
    }

    @Test
    fun `unknown stored theme falls back to System`() = runTest {
        val store = InMemoryDataStore(mutablePreferencesOf(stringPreferencesKey("theme_preference") to "Purple"))
        assertEquals(ThemePreference.System, DataStoreAppPreferencesRepository(store).observeTheme().first())
    }

    @Test
    fun `clear all wipes profile and theme`() = runTest {
        val store = InMemoryDataStore()
        val profiles = DataStoreUserProfileRepository(store)
        val prefs = DataStoreAppPreferencesRepository(store)
        profiles.saveProfile(UserProfile("Ahmad", CurrencyCode.of("JOD")!!))
        prefs.setTheme(ThemePreference.Dark)

        DeviceLocalDataCleaner(store, InMemoryGroupRepository()).clearAll()

        assertNull(profiles.observeProfile().first())
        assertEquals(ThemePreference.System, prefs.observeTheme().first())
    }

    @Test
    fun `report export is unavailable until expenses exist`() = runTest {
        assertTrue(UnavailableExpenseReportExporter().export(emptyList()) is ExportResult.Failed)
    }
}
