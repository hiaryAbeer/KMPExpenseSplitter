package com.abeerapps.kmpexpensesplitter.core.data.testutil

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import com.abeerapps.kmpexpensesplitter.core.data.db.AppDatabase

internal actual fun inMemorySqlDriver(): SqlDriver = inMemoryDriver(AppDatabase.Schema)
