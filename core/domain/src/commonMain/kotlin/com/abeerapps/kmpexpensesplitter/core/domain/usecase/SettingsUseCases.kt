package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.ThemePreference
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.AppPreferencesRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseReportExporter
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExportResult
import com.abeerapps.kmpexpensesplitter.core.domain.repository.LocalDataCleaner
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

/** Outcome of editing the registered profile. */
sealed interface ProfileUpdateResult {
    data object Success : ProfileUpdateResult

    /** Name is blank or longer than [UserProfile.MAX_NAME_LENGTH]. */
    data object InvalidName : ProfileUpdateResult

    /** There is no registered profile to update. */
    data object NoProfile : ProfileUpdateResult

    data object StorageError : ProfileUpdateResult
}

/**
 * Edits the registered profile (Settings). Shares the name rules with
 * registration; keeps whatever field is not being changed.
 */
class UpdateUserProfileUseCase(
    private val repository: UserProfileRepository,
) {
    suspend fun updateName(name: String): ProfileUpdateResult {
        if (!UserProfile.isValidName(name)) return ProfileUpdateResult.InvalidName
        return update { it.copy(name = name.trim()) }
    }

    suspend fun updateDefaultCurrency(currency: CurrencyCode): ProfileUpdateResult =
        update { it.copy(defaultCurrency = currency) }

    private suspend fun update(change: (UserProfile) -> UserProfile): ProfileUpdateResult {
        return try {
            val current = repository.observeProfile().first() ?: return ProfileUpdateResult.NoProfile
            repository.saveProfile(change(current))
            ProfileUpdateResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ProfileUpdateResult.StorageError
        }
    }
}

class ObserveThemePreferenceUseCase(private val repository: AppPreferencesRepository) {
    operator fun invoke(): Flow<ThemePreference> = repository.observeTheme()
}

class SetThemePreferenceUseCase(private val repository: AppPreferencesRepository) {
    suspend operator fun invoke(theme: ThemePreference) = repository.setTheme(theme)
}

/**
 * Wipes all local data. Afterwards the app behaves like a fresh install
 * (onboarding is shown again).
 */
class ClearLocalDataUseCase(private val cleaner: LocalDataCleaner) {
    suspend operator fun invoke(): Boolean = try {
        cleaner.clearAll()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }
}

/**
 * Exports the expense report. Business rule: there must be something to
 * export; with no groups the export is refused without touching storage.
 */
class ExportExpenseReportUseCase(
    private val observeGroups: ObserveGroupsUseCase,
    private val exporter: ExpenseReportExporter,
) {
    suspend operator fun invoke(): ExportResult {
        val groups = observeGroups().first()
        if (groups.isEmpty()) return ExportResult.Failed
        return try {
            exporter.export(groups)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ExportResult.Failed
        }
    }
}
