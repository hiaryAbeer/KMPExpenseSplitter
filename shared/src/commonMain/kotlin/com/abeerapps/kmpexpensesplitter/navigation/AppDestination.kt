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
     * The expense form. [expenseGroupId] preselects that expense group (from Group
     * Details); [expenseId] edits that expense instead of adding one ("Edit Expense").
     * Names match `AddExpenseViewModel.ARG_EXPENSE_GROUP_ID` / `ARG_EXPENSE_ID`.
     */
    @Serializable
    data class AddExpense(val expenseGroupId: String? = null, val expenseId: String? = null) : AppDestination

    /** One expense: payer, shares, Edit and Delete. [expenseId] matches `ExpenseDetailsViewModel.ARG_EXPENSE_ID`. */
    @Serializable
    data class ExpenseDetails(val expenseId: String) : AppDestination

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
