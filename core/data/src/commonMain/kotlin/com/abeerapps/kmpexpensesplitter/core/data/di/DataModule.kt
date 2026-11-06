package com.abeerapps.kmpexpensesplitter.core.data.di

import com.abeerapps.kmpexpensesplitter.core.data.SystemDateProvider
import com.abeerapps.kmpexpensesplitter.core.data.repository.DataStoreUserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.DataStoreAppPreferencesRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.DeviceLocalDataCleaner
import com.abeerapps.kmpexpensesplitter.core.data.repository.InMemoryExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.InMemoryExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.InMemoryPeopleGroupRepository
import com.abeerapps.kmpexpensesplitter.core.data.repository.UnavailableExpenseReportExporter
import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.repository.AppPreferencesRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseReportExporter
import com.abeerapps.kmpexpensesplitter.core.domain.repository.LocalDataCleaner
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
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
    single { InMemoryPeopleGroupRepository() } bind PeopleGroupRepository::class
    single { InMemoryExpenseGroupRepository() } bind ExpenseGroupRepository::class
    single { InMemoryExpenseRepository() } bind ExpenseRepository::class
    single<DateProvider> { SystemDateProvider() }
    single<AppPreferencesRepository> { DataStoreAppPreferencesRepository(dataStore = get()) }
    single<LocalDataCleaner> { DeviceLocalDataCleaner(dataStore = get(), expenseGroups = get(), groups = get(), expenses = get()) }
    single<ExpenseReportExporter> { UnavailableExpenseReportExporter() }
}
