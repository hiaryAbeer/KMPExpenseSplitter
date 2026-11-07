package com.abeerapps.kmpexpensesplitter.core.data.testutil

import app.cash.sqldelight.db.SqlDriver
import com.abeerapps.kmpexpensesplitter.core.data.db.AppDatabase

/** A fresh, empty in-memory SQLite database with the app's schema (real SQL, no mocks). */
internal expect fun inMemorySqlDriver(): SqlDriver

internal fun testDatabase(driver: SqlDriver = inMemorySqlDriver()): AppDatabase = AppDatabase(driver)
