package com.abeerapps.kmpexpensesplitter.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation routes for the whole app (Navigation Compose). */
@Serializable
sealed interface AppDestination {

    @Serializable
    data object Onboarding : AppDestination

    /** "Set up your profile": name + default currency. */
    @Serializable
    data object Registration : AppDestination

    /** Main app: bottom-navigation shell (Home, Groups, Settings). */
    @Serializable
    data object Home : AppDestination

    /** Placeholder until group creation is built. */
    @Serializable
    data object CreateGroup : AppDestination

    /** Placeholder until group details are built. */
    @Serializable
    data class GroupDetails(val groupId: String) : AppDestination
}
