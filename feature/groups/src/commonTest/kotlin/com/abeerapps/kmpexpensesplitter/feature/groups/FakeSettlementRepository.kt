package com.abeerapps.kmpexpensesplitter.feature.groups

import com.abeerapps.kmpexpensesplitter.core.domain.model.Settlement
import com.abeerapps.kmpexpensesplitter.core.domain.repository.SettlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettlementRepository : SettlementRepository {
    val settlements = MutableStateFlow<List<Settlement>>(emptyList())
    var failNext = false
    override fun observeSettlements(): Flow<List<Settlement>> = settlements
    override suspend fun addSettlements(settlements: List<Settlement>) {
        if (failNext) {
            failNext = false
            error("disk full")
        }
        this.settlements.value += settlements
    }
    override suspend fun deleteSettlementsIn(expenseGroupId: String) {
        settlements.value = settlements.value.filterNot { it.expenseGroupId == expenseGroupId }
    }
}
