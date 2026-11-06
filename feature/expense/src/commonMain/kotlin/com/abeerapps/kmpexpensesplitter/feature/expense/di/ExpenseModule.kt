package com.abeerapps.kmpexpensesplitter.feature.expense.di

import com.abeerapps.kmpexpensesplitter.core.domain.usecase.AddExpenseUseCase
import com.abeerapps.kmpexpensesplitter.feature.expense.AddExpenseViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * DI bindings contributed by the expense feature. Group use cases (expense and people
 * groups) are shared with the Groups tab's manage screens, so they live in the app module.
 */
val expenseModule = module {
    factory { AddExpenseUseCase(repository = get()) }
    viewModelOf(::AddExpenseViewModel)
}
