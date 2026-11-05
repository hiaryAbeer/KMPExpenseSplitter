package com.abeerapps.kmpexpensesplitter.feature.home.di

import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CalculateBalanceSummaryUseCase
import com.abeerapps.kmpexpensesplitter.feature.home.HomeViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * DI bindings contributed by the Home feature. Use cases shared with other
 * features (observe profile/groups) are provided by the app module.
 */
val homeModule = module {
    factoryOf(::CalculateBalanceSummaryUseCase)
    viewModelOf(::HomeViewModel)
}
