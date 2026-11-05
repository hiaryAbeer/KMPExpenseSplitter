package com.abeerapps.kmpexpensesplitter.core.data.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Temporary [GroupRepository]: starts empty and keeps nothing across restarts.
 *
 * Group creation and persistence (database) are not built yet. Replace this
 * binding in `dataModule` with the real implementation when they are; nothing
 * above the data layer needs to change.
 */
internal class InMemoryGroupRepository : GroupRepository {
    private val groups = MutableStateFlow<List<GroupSummary>>(emptyList())

    override fun observeGroups(): Flow<List<GroupSummary>> = groups.asStateFlow()

    /** Used by "Clear local data". */
    fun clear() {
        groups.value = emptyList()
    }
}
