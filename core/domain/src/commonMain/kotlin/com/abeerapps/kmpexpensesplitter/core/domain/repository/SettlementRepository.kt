package com.abeerapps.kmpexpensesplitter.core.domain.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.Settlement
import kotlinx.coroutines.flow.Flow

/** Source of truth for recorded settle-up payments. Implemented in `:core:data`. */
interface SettlementRepository {

    /** Emits every settlement; re-emits on change. */
    fun observeSettlements(): Flow<List<Settlement>>

    /** Stores all of [settlements] at once. Throws on storage failure. */
    suspend fun addSettlements(settlements: List<Settlement>)

    /** Removes every settlement of the expense group [expenseGroupId]. Throws on storage failure. */
    suspend fun deleteSettlementsIn(expenseGroupId: String)
}
