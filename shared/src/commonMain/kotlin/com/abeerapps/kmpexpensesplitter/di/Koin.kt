package com.abeerapps.kmpexpensesplitter.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.abeerapps.kmpexpensesplitter.AppViewModel
import com.abeerapps.kmpexpensesplitter.config.AppConfig
import com.abeerapps.kmpexpensesplitter.config.StaticFeatureFlags
import com.abeerapps.kmpexpensesplitter.core.data.di.dataModule
import com.abeerapps.kmpexpensesplitter.core.domain.config.FeatureFlags
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveOnboardingStatusUseCase
import com.abeerapps.kmpexpensesplitter.feature.home.di.homeModule
import com.abeerapps.kmpexpensesplitter.feature.onboarding.di.onboardingModule
import com.abeerapps.kmpexpensesplitter.feature.registration.di.registrationModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** App-level bindings: config, feature flags, domain use cases, shell ViewModel. */
internal fun appModule(config: AppConfig): Module = module {
    single { config }
    single<FeatureFlags> { StaticFeatureFlags }
    factoryOf(::ObserveOnboardingStatusUseCase)
    viewModelOf(::AppViewModel)
}

/** Every module in the graph; also used by the DI verification test. */
internal fun allModules(config: AppConfig): List<Module> = listOf(
    appModule(config),
    dataModule,
    onboardingModule,
    registrationModule,
    homeModule,
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
