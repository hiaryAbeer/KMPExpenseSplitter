package com.abeerapps.kmpexpensesplitter.feature.expense.di

import com.abeerapps.kmpexpensesplitter.core.domain.usecase.AddExpenseUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreateExpenseGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreatePeopleGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObservePeopleGroupsUseCase
import com.abeerapps.kmpexpensesplitter.feature.expense.AddExpenseViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** DI bindings contributed by the expense feature (including creating expense and people groups from "Add Expense"). */
val expenseModule = module {
    factory { AddExpenseUseCase(repository = get()) }
    factoryOf(::ObserveExpenseGroupsUseCase)
    factory { CreateExpenseGroupUseCase(repository = get(), dateProvider = get()) }
    factory { CreatePeopleGroupUseCase(groups = get(), profiles = get(), dateProvider = get()) }
    factoryOf(::ObservePeopleGroupsUseCase)
    viewModelOf(::AddExpenseViewModel)
}
