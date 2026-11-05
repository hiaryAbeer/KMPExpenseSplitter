package com.abeerapps.kmpexpensesplitter.feature.settings.di

import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ClearLocalDataUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ExportExpenseReportUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.SetThemePreferenceUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.UpdateUserProfileUseCase
import com.abeerapps.kmpexpensesplitter.feature.settings.SettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * DI bindings contributed by Settings. Shared use cases (profile, groups, theme,
 * currency search) are provided by the app module or other features.
 */
val settingsModule = module {
    factoryOf(::UpdateUserProfileUseCase)
    factoryOf(::SetThemePreferenceUseCase)
    factoryOf(::ExportExpenseReportUseCase)
    factoryOf(::ClearLocalDataUseCase)
    viewModelOf(::SettingsViewModel)
}
