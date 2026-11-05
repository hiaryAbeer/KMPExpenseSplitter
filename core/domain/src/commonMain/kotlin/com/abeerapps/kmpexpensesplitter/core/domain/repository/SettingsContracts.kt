package com.abeerapps.kmpexpensesplitter.core.domain.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

/** App-level preferences that are not part of the user's profile. */
interface AppPreferencesRepository {
    /** Emits the stored theme choice ([ThemePreference.System] if never set). */
    fun observeTheme(): Flow<ThemePreference>

    suspend fun setTheme(theme: ThemePreference)
}

/** Deletes everything the app stores on this device (profile, groups, preferences). */
interface LocalDataCleaner {
    suspend fun clearAll()
}

/**
 * Produces the downloadable expense report (PDF) for the given groups.
 *
 * Platform implementations write the file to the device's Downloads/Files and
 * return where it went. The report's content depends on expense data, which the
 * app does not store yet; see `UnavailableExpenseReportExporter` in `:core:data`.
 */
interface ExpenseReportExporter {
    suspend fun export(groups: List<GroupSummary>): ExportResult
}

sealed interface ExportResult {
    /** @property location human-readable place the file was saved (e.g. "Downloads"). */
    data class Saved(val fileName: String, val location: String) : ExportResult

    /** Export is not possible (e.g. not supported yet, or storage error). */
    data object Failed : ExportResult
}
