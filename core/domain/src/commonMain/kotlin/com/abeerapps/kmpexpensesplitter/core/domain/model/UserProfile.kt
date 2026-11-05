package com.abeerapps.kmpexpensesplitter.core.domain.model

/**
 * The local user of the app.
 *
 * A profile only exists once the user has registered; until then the app shows
 * onboarding (see [com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveOnboardingStatusUseCase]).
 *
 * @property name display name shown to the user and to group members.
 * @property defaultCurrency currency preselected when creating groups and expenses.
 */
data class UserProfile(
    val name: String,
    val defaultCurrency: CurrencyCode,
)
