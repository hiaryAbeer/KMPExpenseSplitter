package com.abeerapps.kmpexpensesplitter.core.data.repository

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.abeerapps.kmpexpensesplitter.core.data.testutil.InMemoryDataStore
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.ThemePreference
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExportResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
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
    fun `clear all wipes profile and theme and expenses`() = runTest {
        val store = InMemoryDataStore()
        val profiles = DataStoreUserProfileRepository(store)
        val prefs = DataStoreAppPreferencesRepository(store)
        val expenses = InMemoryExpenseRepository()
        val jod = CurrencyCode.of("JOD")!!
        profiles.saveProfile(UserProfile("Ahmad", jod))
        prefs.setTheme(ThemePreference.Dark)
        expenses.addExpense(
            Expense(
                id = "1", groupId = "g1", title = "Lunch", amount = Money(10_00, jod), category = ExpenseCategory.Food,
                paidBy = "you", splitType = SplitType.Equal, shares = listOf(ExpenseShare("you", Money(10_00, jod))),
                date = LocalDate(2026, 4, 29), note = null,
            ),
        )

        DeviceLocalDataCleaner(store, InMemoryGroupRepository(), expenses).clearAll()

        assertNull(profiles.observeProfile().first())
        assertEquals(ThemePreference.System, prefs.observeTheme().first())
        assertTrue(expenses.observeExpenses().first().isEmpty())
    }

    @Test
    fun `report export is unavailable until expenses exist`() = runTest {
        assertTrue(UnavailableExpenseReportExporter().export(emptyList()) is ExportResult.Failed)
    }
}
