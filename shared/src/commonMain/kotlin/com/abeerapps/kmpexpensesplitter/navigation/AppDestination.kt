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

    /**
     * "Add Expense", from the "+" buttons. [expenseGroupId] preselects that expense
     * group (from Group Details); the name matches `AddExpenseViewModel.ARG_EXPENSE_GROUP_ID`.
     */
    @Serializable
    data class AddExpense(val expenseGroupId: String? = null) : AppDestination

    /** Search, add, rename and delete expense groups (from the Groups tab header). */
    @Serializable
    data object ManageExpenseGroups : AppDestination

    /** Search, add, edit and delete people groups (from the Groups tab header). */
    @Serializable
    data object ManagePeopleGroups : AppDestination

    /** An expense group's expenses, balances and stats. [groupId] matches `GroupDetailsViewModel.ARG_GROUP_ID`. */
    @Serializable
    data class GroupDetails(val groupId: String) : AppDestination
}
