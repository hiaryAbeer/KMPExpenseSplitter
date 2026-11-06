package com.abeerapps.kmpexpensesplitter.core.domain.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import kotlinx.coroutines.flow.Flow

/** Source of truth for expense groups (trips). Implemented in `:core:data`. */
interface ExpenseGroupRepository {

    /** Emits every expense group, newest first; re-emits on change. */
    fun observeExpenseGroups(): Flow<List<ExpenseGroup>>

    /** Stores [group]. Throws on storage failure. */
    suspend fun createExpenseGroup(group: ExpenseGroup)
}
