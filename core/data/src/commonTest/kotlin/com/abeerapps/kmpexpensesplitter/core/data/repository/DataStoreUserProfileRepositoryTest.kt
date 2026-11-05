package com.abeerapps.kmpexpensesplitter.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import okio.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataStoreUserProfileRepositoryTest {

    private val silentLogger = Logger(loggerConfigInit())
    private val eur = CurrencyCode.of("EUR")!!

    @Test
    fun `empty store emits null`() = runTest {
        val repository = DataStoreUserProfileRepository(InMemoryDataStore(), silentLogger)

        repository.observeProfile().test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `saved profile is emitted with trimmed name`() = runTest {
        val repository = DataStoreUserProfileRepository(InMemoryDataStore(), silentLogger)

        repository.observeProfile().test {
            assertNull(awaitItem())
            repository.saveProfile(UserProfile(name = "  Abeer  ", defaultCurrency = eur))
            assertEquals(UserProfile("Abeer", eur), awaitItem())
        }
    }

    @Test
    fun `invalid stored currency is treated as no profile`() = runTest {
        val store = InMemoryDataStore(
            mutablePreferencesOf(
                stringPreferencesKey("user_name") to "Abeer",
                stringPreferencesKey("default_currency") to "not-a-code",
            ),
        )
        val repository = DataStoreUserProfileRepository(store, silentLogger)

        repository.observeProfile().test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `missing name is treated as no profile`() = runTest {
        val store = InMemoryDataStore(
            mutablePreferencesOf(stringPreferencesKey("default_currency") to "EUR"),
        )
        val repository = DataStoreUserProfileRepository(store, silentLogger)

        repository.observeProfile().test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `io error while reading falls back to no profile instead of crashing`() = runTest {
        val repository = DataStoreUserProfileRepository(FailingDataStore(), silentLogger)

        repository.observeProfile().test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    /** Minimal in-memory DataStore; avoids touching the file system in unit tests. */
    private class InMemoryDataStore(
        initial: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state
        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = transform(state.value).also { state.value = it }
    }

    private class FailingDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("corrupt file") }
        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = throw IOException("read-only")
    }
}
