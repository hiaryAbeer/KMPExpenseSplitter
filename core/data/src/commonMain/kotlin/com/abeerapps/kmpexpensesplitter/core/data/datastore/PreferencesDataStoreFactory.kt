package com.abeerapps.kmpexpensesplitter.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/** File name of the DataStore that holds the user profile. */
internal const val USER_PROFILE_DATASTORE_FILE = "user_profile.preferences_pb"

/**
 * Creates a Preferences DataStore at the absolute path returned by [producePath].
 *
 * The path is platform-specific (see `platformDataModule` actuals), which keeps
 * this factory in common code. A DataStore must be a singleton per file;
 * Koin's `single` guarantees that.
 */
internal fun createPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
