package com.abeerapps.kmpexpensesplitter.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation routes for the whole app (Navigation Compose). */
@Serializable
sealed interface AppDestination {

    @Serializable
    data object Onboarding : AppDestination

    /** Name + default currency form. Placeholder until the feature lands. */
    @Serializable
    data object Registration : AppDestination

    /** Main app (groups & expenses). Placeholder until the feature lands. */
    @Serializable
    data object Home : AppDestination
}
