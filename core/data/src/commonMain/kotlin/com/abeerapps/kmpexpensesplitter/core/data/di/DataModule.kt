package com.abeerapps.kmpexpensesplitter.core.data.di

import com.abeerapps.kmpexpensesplitter.core.data.repository.DataStoreUserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.DataStoreAppPreferencesRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.DeviceLocalDataCleaner
import com.abeerapps.kmpexpensesplitter.core.data.repository.InMemoryGroupRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.UnavailableExpenseReportExporter
import com.abeerapps.kmpexpensesplitter.core.domain.repository.AppPreferencesRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseReportExporter
import com.abeerapps.kmpexpensesplitter.core.domain.repository.LocalDataCleaner
import com.abeerapps.kmpexpensesplitter.core.domain.repository.GroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import org.koin.core.module.Module
import org.koin.dsl.bind
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
    single { InMemoryGroupRepository() } bind GroupRepository::class
    single<AppPreferencesRepository> { DataStoreAppPreferencesRepository(dataStore = get()) }
    single<LocalDataCleaner> { DeviceLocalDataCleaner(dataStore = get(), groups = get()) }
    single<ExpenseReportExporter> { UnavailableExpenseReportExporter() }
}
