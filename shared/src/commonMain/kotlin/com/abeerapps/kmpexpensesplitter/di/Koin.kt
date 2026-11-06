package com.abeerapps.kmpexpensesplitter.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.abeerapps.kmpexpensesplitter.AppViewModel
import com.abeerapps.kmpexpensesplitter.config.AppConfig
import com.abeerapps.kmpexpensesplitter.config.StaticFeatureFlags
import com.abeerapps.kmpexpensesplitter.core.data.di.dataModule
import com.abeerapps.kmpexpensesplitter.core.domain.config.AppInfo
import com.abeerapps.kmpexpensesplitter.core.domain.config.FeatureFlags
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreateExpenseGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreatePeopleGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpensesUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveOnboardingStatusUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObservePeopleGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveThemePreferenceUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveUserProfileUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.SearchCurrenciesUseCase
import com.abeerapps.kmpexpensesplitter.feature.expense.di.expenseModule
import com.abeerapps.kmpexpensesplitter.feature.groups.di.groupsModule
import com.abeerapps.kmpexpensesplitter.feature.home.di.homeModule
import com.abeerapps.kmpexpensesplitter.feature.onboarding.di.onboardingModule
import com.abeerapps.kmpexpensesplitter.feature.registration.di.registrationModule
import com.abeerapps.kmpexpensesplitter.feature.settings.di.settingsModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * App-level bindings: config, feature flags, the shell ViewModel and use cases
 * shared by several features (so no feature depends on another's DI module).
 */
internal fun appModule(config: AppConfig): Module = module {
    single { config }
    single<AppInfo> { config }
    single<FeatureFlags> { StaticFeatureFlags }
    factoryOf(::ObserveOnboardingStatusUseCase)
    factoryOf(::ObserveUserProfileUseCase)
    factoryOf(::ObserveGroupsUseCase)
    // Group use cases shared by Add Expense and the Groups tab's manage screens.
    factoryOf(::ObserveExpenseGroupsUseCase)
    factory { CreateExpenseGroupUseCase(repository = get(), dateProvider = get()) }
    factoryOf(::ObservePeopleGroupsUseCase)
    factoryOf(::ObserveExpensesUseCase)
    factory { CreatePeopleGroupUseCase(groups = get(), profiles = get(), dateProvider = get()) }
    factoryOf(::ObserveThemePreferenceUseCase)
    // Single: it builds a search index of the whole currency catalog once.
    single { SearchCurrenciesUseCase() }
    viewModelOf(::AppViewModel)
}

/** Every module in the graph; also used by the DI verification test. */
internal fun allModules(config: AppConfig): List<Module> = listOf(
    appModule(config),
    dataModule,
    onboardingModule,
    registrationModule,
    homeModule,
    settingsModule,
    expenseModule,
    groupsModule,
)

/**
 * Starts DI and logging. Call exactly once, as early as possible:
 * Android → `Application.onCreate`, iOS → `iOSApp.init`.
 *
 * @param platformDeclaration platform hooks, e.g. `androidContext(...)`.
 */
fun initKoin(config: AppConfig, platformDeclaration: KoinAppDeclaration = {}) {
    // Production builds only log warnings/errors; no PII is ever logged.
    Logger.setMinSeverity(if (config.isDebug) Severity.Debug else Severity.Warn)

    startKoin {
        platformDeclaration()
        modules(allModules(config))
    }
}
