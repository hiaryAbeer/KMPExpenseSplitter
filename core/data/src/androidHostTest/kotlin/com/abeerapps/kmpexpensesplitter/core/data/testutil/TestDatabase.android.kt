package com.abeerapps.kmpexpensesplitter.core.data.testutil

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.abeerapps.kmpexpensesplitter.core.data.db.AppDatabase

internal actual fun inMemorySqlDriver(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { AppDatabase.Schema.create(it) }
