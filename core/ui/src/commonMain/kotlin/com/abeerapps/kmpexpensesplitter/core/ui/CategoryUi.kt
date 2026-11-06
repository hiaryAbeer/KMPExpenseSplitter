package com.abeerapps.kmpexpensesplitter.core.ui

import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.ui.resources.Res
import com.abeerapps.kmpexpensesplitter.core.ui.resources.category_bills
import com.abeerapps.kmpexpensesplitter.core.ui.resources.category_entertainment
import com.abeerapps.kmpexpensesplitter.core.ui.resources.category_food
import com.abeerapps.kmpexpensesplitter.core.ui.resources.category_hotel
import com.abeerapps.kmpexpensesplitter.core.ui.resources.category_shopping
import com.abeerapps.kmpexpensesplitter.core.ui.resources.category_transport
import com.abeerapps.kmpexpensesplitter.core.ui.resources.ic_category_bills
import com.abeerapps.kmpexpensesplitter.core.ui.resources.ic_category_entertainment
import com.abeerapps.kmpexpensesplitter.core.ui.resources.ic_category_food
import com.abeerapps.kmpexpensesplitter.core.ui.resources.ic_category_hotel
import com.abeerapps.kmpexpensesplitter.core.ui.resources.ic_category_shopping
import com.abeerapps.kmpexpensesplitter.core.ui.resources.ic_category_transport
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/** Icon of an expense category (Add Expense tiles, expense lists, stats). */
val ExpenseCategory.icon: DrawableResource
    get() = when (this) {
        ExpenseCategory.Food -> Res.drawable.ic_category_food
        ExpenseCategory.Transport -> Res.drawable.ic_category_transport
        ExpenseCategory.Hotel -> Res.drawable.ic_category_hotel
        ExpenseCategory.Bills -> Res.drawable.ic_category_bills
        ExpenseCategory.Shopping -> Res.drawable.ic_category_shopping
        ExpenseCategory.Entertainment -> Res.drawable.ic_category_entertainment
    }

/** Display name of an expense category. */
val ExpenseCategory.label: StringResource
    get() = when (this) {
        ExpenseCategory.Food -> Res.string.category_food
        ExpenseCategory.Transport -> Res.string.category_transport
        ExpenseCategory.Hotel -> Res.string.category_hotel
        ExpenseCategory.Bills -> Res.string.category_bills
        ExpenseCategory.Shopping -> Res.string.category_shopping
        ExpenseCategory.Entertainment -> Res.string.category_entertainment
    }
