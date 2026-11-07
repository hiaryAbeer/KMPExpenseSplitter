package com.abeerapps.kmpexpensesplitter.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.abeerapps.kmpexpensesplitter.core.data.datastore.USER_PROFILE_DATASTORE_FILE
import com.abeerapps.kmpexpensesplitter.core.data.datastore.createPreferencesDataStore
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.abeerapps.kmpexpensesplitter.core.data.database.DATABASE_FILE
import com.abeerapps.kmpexpensesplitter.core.data.db.AppDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * Stores DataStore files under `Library/Application Support`; the SQLite driver
 * keeps the database under `Library/Application Support/databases`.
 *
 * Unlike `Documents`, this folder is not exposed through the Files app or
 * iTunes file sharing. Files get iOS Data Protection
 * (`CompleteUntilFirstUserAuthentication`) by default.
 */
internal actual val platformDataModule: Module = module {
    single<DataStore<Preferences>> {
        createPreferencesDataStore { "${applicationSupportDirectory()}/$USER_PROFILE_DATASTORE_FILE" }
    }
    single<SqlDriver> { NativeSqliteDriver(AppDatabase.Schema, DATABASE_FILE) }
}

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupportDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true, // Application Support is not created automatically.
        error = null,
    )
    return requireNotNull(url?.path) { "Application Support directory is unavailable" }
}
