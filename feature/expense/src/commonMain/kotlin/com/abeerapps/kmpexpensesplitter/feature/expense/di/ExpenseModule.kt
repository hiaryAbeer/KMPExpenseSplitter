package com.abeerapps.kmpexpensesplitter.feature.expense.di

import com.abeerapps.kmpexpensesplitter.core.domain.usecase.AddExpenseUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreateGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveGroupListUseCase
import com.abeerapps.kmpexpensesplitter.feature.expense.AddExpenseViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** DI bindings contributed by the expense feature (including creating a group from "Add Expense"). */
val expenseModule = module {
    factory { AddExpenseUseCase(repository = get()) }
    factory { CreateGroupUseCase(groups = get(), profiles = get(), dateProvider = get()) }
    factoryOf(::ObserveGroupListUseCase)
    viewModelOf(::AddExpenseViewModel)
}
