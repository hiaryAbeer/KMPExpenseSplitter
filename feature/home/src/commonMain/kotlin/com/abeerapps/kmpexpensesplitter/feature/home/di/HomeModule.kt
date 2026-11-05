package com.abeerapps.kmpexpensesplitter.feature.home.di

import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CalculateBalanceSummaryUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveUserProfileUseCase
import com.abeerapps.kmpexpensesplitter.feature.home.HomeViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** DI bindings contributed by the Home feature. */
val homeModule = module {
    factoryOf(::ObserveUserProfileUseCase)
    factoryOf(::ObserveGroupsUseCase)
    factoryOf(::CalculateBalanceSummaryUseCase)
    viewModelOf(::HomeViewModel)
}
