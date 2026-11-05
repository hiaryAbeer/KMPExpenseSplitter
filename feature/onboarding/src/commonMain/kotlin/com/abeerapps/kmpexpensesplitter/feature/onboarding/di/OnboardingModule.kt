package com.abeerapps.kmpexpensesplitter.feature.onboarding.di

import com.abeerapps.kmpexpensesplitter.feature.onboarding.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** DI bindings contributed by the onboarding feature. */
val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
}
