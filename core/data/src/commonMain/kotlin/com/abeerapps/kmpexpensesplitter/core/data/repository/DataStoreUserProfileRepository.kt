package com.abeerapps.kmpexpensesplitter.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okio.IOException

/**
 * [UserProfileRepository] backed by a Preferences DataStore.
 *
 * - Reads are reactive and off the main thread (DataStore handles dispatching).
 * - A read failure (e.g. corrupt file) is logged and treated as "no profile",
 *   so the worst case is the user re-registers instead of the app crashing.
 * - Stored values are re-validated on read; invalid data maps to `null`.
 */
internal class DataStoreUserProfileRepository(
    private val dataStore: DataStore<Preferences>,
    private val logger: Logger = Logger.withTag("UserProfileRepository"),
) : UserProfileRepository {

    override fun observeProfile(): Flow<UserProfile?> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    // Never log the profile contents; it is user PII.
                    logger.e(error) { "Failed to read user profile; treating as absent" }
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { it.toUserProfile() }
            .distinctUntilChanged()

    override suspend fun saveProfile(profile: UserProfile) {
        dataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name.trim()
            prefs[Keys.DEFAULT_CURRENCY] = profile.defaultCurrency.value
        }
    }

    private fun Preferences.toUserProfile(): UserProfile? {
        val name = this[Keys.NAME]?.takeIf { it.isNotBlank() } ?: return null
        val currency = this[Keys.DEFAULT_CURRENCY]?.let(CurrencyCode::of) ?: return null
        return UserProfile(name = name, defaultCurrency = currency)
    }

    /** Storage keys. Renaming one is a data migration: keep them stable. */
    private object Keys {
        val NAME = stringPreferencesKey("user_name")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
    }
}
