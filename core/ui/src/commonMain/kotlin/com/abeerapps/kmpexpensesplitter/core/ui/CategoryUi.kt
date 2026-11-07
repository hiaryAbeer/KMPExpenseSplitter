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
import androidx.compose.ui.graphics.Color
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

/**
 * Chart color of an expense category (Group Details stats). Hotel, Food, Transport,
 * Shopping and Entertainment come from the design; Bills is blue to stay distinct.
 * Mid-tone, so they read on both the light and the dark card.
 */
val ExpenseCategory.chartColor: Color
    get() = when (this) {
        ExpenseCategory.Hotel -> Color(0xFF0D9488)
        ExpenseCategory.Food -> Color(0xFFFB923C)
        ExpenseCategory.Transport -> Color(0xFF8B5CF6)
        ExpenseCategory.Shopping -> Color(0xFFEC4899)
        ExpenseCategory.Entertainment -> Color(0xFF06B6D4)
        ExpenseCategory.Bills -> Color(0xFF3B82F6)
    }
