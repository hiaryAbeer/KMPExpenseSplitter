package com.abeerapps.kmpexpensesplitter.feature.groups.di

import com.abeerapps.kmpexpensesplitter.core.domain.usecase.DeleteExpenseGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.DeletePeopleGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpensesUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.RenameExpenseGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.UpdatePeopleGroupUseCase
import com.abeerapps.kmpexpensesplitter.feature.groups.GroupsViewModel
import com.abeerapps.kmpexpensesplitter.feature.groups.manage.ManageExpenseGroupsViewModel
import com.abeerapps.kmpexpensesplitter.feature.groups.manage.ManagePeopleGroupsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * DI bindings contributed by the Groups tab and its manage screens. Use cases shared
 * with other features (observe/create groups) come from the app module.
 */
val groupsModule = module {
    factoryOf(::ObserveExpensesUseCase)
    factoryOf(::RenameExpenseGroupUseCase)
    factoryOf(::DeleteExpenseGroupUseCase)
    factory { UpdatePeopleGroupUseCase(groups = get(), expenses = get(), profiles = get()) }
    factoryOf(::DeletePeopleGroupUseCase)
    viewModelOf(::GroupsViewModel)
    viewModelOf(::ManageExpenseGroupsViewModel)
    viewModelOf(::ManagePeopleGroupsViewModel)
}
