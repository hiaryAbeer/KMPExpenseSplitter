package com.abeerapps.kmpexpensesplitter.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.abeerapps.kmpexpensesplitter.core.data.datastore.USER_PROFILE_DATASTORE_FILE
import com.abeerapps.kmpexpensesplitter.core.data.datastore.createPreferencesDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Stores DataStore files in the app-private `filesDir`, which is sandboxed per
 * app and excluded from backups by `data_extraction_rules.xml`.
 */
internal actual val platformDataModule: Module = module {
    single<DataStore<Preferences>> {
        val context = androidContext()
        createPreferencesDataStore {
            context.filesDir.resolve(USER_PROFILE_DATASTORE_FILE).absolutePath
        }
    }
}
