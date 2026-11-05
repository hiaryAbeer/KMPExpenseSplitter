package com.abeerapps.kmpexpensesplitter.feature.registration.di

import com.abeerapps.kmpexpensesplitter.core.domain.usecase.RegisterUserUseCase
import com.abeerapps.kmpexpensesplitter.feature.registration.RegistrationViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** DI bindings contributed by the registration feature (currency search comes from the app module). */
val registrationModule = module {
    factoryOf(::RegisterUserUseCase)
    // SavedStateHandle is supplied by Koin from the navigation back stack entry.
    viewModelOf(::RegistrationViewModel)
}
