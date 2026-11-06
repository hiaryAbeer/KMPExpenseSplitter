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

    /** "Add Expense", opened from the Home "+" button. */
    @Serializable
    data object AddExpense : AppDestination

    /** Search, add, rename and delete expense groups (from the Groups tab header). */
    @Serializable
    data object ManageExpenseGroups : AppDestination

    /** Search, add, edit and delete people groups (from the Groups tab header). */
    @Serializable
    data object ManagePeopleGroups : AppDestination

    /** Placeholder until group details are built. */
    @Serializable
    data class GroupDetails(val groupId: String) : AppDestination
}
