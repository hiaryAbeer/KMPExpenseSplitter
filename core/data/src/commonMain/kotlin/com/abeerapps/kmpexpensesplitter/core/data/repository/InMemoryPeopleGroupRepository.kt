package com.abeerapps.kmpexpensesplitter.core.data.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Temporary [PeopleGroupRepository]: groups live for the app session only.
 *
 * Replace this binding in `dataModule` with a database implementation (together
 * with [InMemoryExpenseRepository]); nothing above the data layer needs to change.
 */
internal class InMemoryPeopleGroupRepository : PeopleGroupRepository {
    private val groups = MutableStateFlow<List<PeopleGroup>>(emptyList())

    override fun observeGroups(): Flow<List<PeopleGroup>> = groups.asStateFlow()

    override suspend fun createGroup(group: PeopleGroup) {
        groups.update { listOf(group) + it } // Newest first.
    }

    override suspend fun updateGroup(group: PeopleGroup) {
        groups.update { all -> all.map { if (it.id == group.id) group else it } }
    }

    override suspend fun deleteGroup(groupId: String) {
        groups.update { all -> all.filterNot { it.id == groupId } }
    }

    /** Used by "Clear local data". */
    fun clear() {
        groups.value = emptyList()
    }
}
