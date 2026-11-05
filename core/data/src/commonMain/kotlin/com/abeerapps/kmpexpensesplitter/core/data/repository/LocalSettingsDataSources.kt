package com.abeerapps.kmpexpensesplitter.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.ThemePreference
import com.abeerapps.kmpexpensesplitter.core.domain.repository.AppPreferencesRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseReportExporter
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExportResult
import com.abeerapps.kmpexpensesplitter.core.domain.repository.LocalDataCleaner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okio.IOException

/**
 * Theme preference stored in the app's Preferences DataStore. Unknown or
 * missing values fall back to [ThemePreference.System].
 */
internal class DataStoreAppPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesRepository {

    override fun observeTheme(): Flow<ThemePreference> =
        dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { prefs ->
                prefs[THEME]?.let { stored -> ThemePreference.entries.firstOrNull { it.name == stored } }
                    ?: ThemePreference.System
            }
            .distinctUntilChanged()

    override suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME] = theme.name }
    }

    private companion object {
        /** Storage key. Renaming it is a data migration: keep it stable. */
        val THEME = stringPreferencesKey("theme_preference")
    }
}

/**
 * Wipes everything stored locally: the Preferences DataStore (profile + app
 * preferences) and the groups. Add new stores here as they are introduced.
 */
internal class DeviceLocalDataCleaner(
    private val dataStore: DataStore<Preferences>,
    private val groups: InMemoryGroupRepository,
) : LocalDataCleaner {
    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
        groups.clear()
    }
}

/**
 * Placeholder until expenses are stored: the report's content (who paid what)
 * does not exist yet, so export reports failure instead of writing an empty
 * file. Replace with platform PDF writers (Android PdfDocument → Downloads,
 * iOS UIGraphicsPDFRenderer → Files) when the expense model lands.
 */
internal class UnavailableExpenseReportExporter : ExpenseReportExporter {
    override suspend fun export(groups: List<GroupSummary>): ExportResult = ExportResult.Failed
}
