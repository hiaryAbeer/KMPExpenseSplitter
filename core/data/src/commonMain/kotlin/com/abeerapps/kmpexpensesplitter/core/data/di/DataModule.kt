package com.abeerapps.kmpexpensesplitter.core.data.di

import com.abeerapps.kmpexpensesplitter.core.data.SystemDateProvider
import com.abeerapps.kmpexpensesplitter.core.data.repository.DataStoreUserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.DataStoreAppPreferencesRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.DeviceLocalDataCleaner
import com.abeerapps.kmpexpensesplitter.core.data.db.AppDatabase
import com.abeerapps.kmpexpensesplitter.core.data.repository.SqlDelightExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.SqlDelightExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.SqlDelightPeopleGroupRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.SqlDelightSettlementRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.UnavailableExpenseReportExporter
import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.repository.AppPreferencesRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.SettlementRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseReportExporter
import com.abeerapps.kmpexpensesplitter.core.domain.repository.LocalDataCleaner
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Platform bindings the data layer needs (e.g. DataStore file location).
 * Android needs a `Context`, iOS needs `NSFileManager`, hence expect/actual.
 */
internal expect val platformDataModule: Module

/** Public entry point: everything the data layer contributes to the DI graph. */
val dataModule: Module = module {
    includes(platformDataModule)
    single<UserProfileRepository> { DataStoreUserProfileRepository(dataStore = get()) }
    // One SQLite database (driver from platformDataModule); queries run on the IO dispatcher.
    single { AppDatabase(driver = get()) }
    single<PeopleGroupRepository> { SqlDelightPeopleGroupRepository(database = get(), dispatcher = Dispatchers.IO) }
    single<ExpenseGroupRepository> { SqlDelightExpenseGroupRepository(database = get(), dispatcher = Dispatchers.IO) }
    single<ExpenseRepository> { SqlDelightExpenseRepository(database = get(), dispatcher = Dispatchers.IO) }
    single<SettlementRepository> { SqlDelightSettlementRepository(database = get(), dispatcher = Dispatchers.IO) }
    single<DateProvider> { SystemDateProvider() }
    single<AppPreferencesRepository> { DataStoreAppPreferencesRepository(dataStore = get()) }
    single<LocalDataCleaner> { DeviceLocalDataCleaner(dataStore = get(), database = get(), dispatcher = Dispatchers.IO) }
    single<ExpenseReportExporter> { UnavailableExpenseReportExporter() }
}
