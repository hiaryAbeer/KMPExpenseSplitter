package com.abeerapps.kmpexpensesplitter.core.data.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.Settlement
import com.abeerapps.kmpexpensesplitter.core.domain.repository.SettlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Temporary [SettlementRepository], like [InMemoryExpenseRepository]: settlements live
 * for the app session only. Replace the binding in `dataModule` with a database
 * implementation together with expenses; nothing above the data layer changes.
 */
internal class InMemorySettlementRepository : SettlementRepository {
    private val settlements = MutableStateFlow<List<Settlement>>(emptyList())

    override fun observeSettlements(): Flow<List<Settlement>> = settlements.asStateFlow()

    override suspend fun addSettlements(settlements: List<Settlement>) {
        this.settlements.update { it + settlements }
    }

    override suspend fun deleteSettlementsIn(expenseGroupId: String) {
        settlements.update { all -> all.filterNot { it.expenseGroupId == expenseGroupId } }
    }

    /** Used by "Clear local data". */
    fun clear() {
        settlements.value = emptyList()
    }
}
