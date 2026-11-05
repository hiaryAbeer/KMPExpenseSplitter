package com.abeerapps.kmpexpensesplitter.core.data.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.Group
import com.abeerapps.kmpexpensesplitter.core.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Temporary [GroupRepository]: groups live for the app session only.
 *
 * Replace this binding in `dataModule` with a database implementation (together
 * with [InMemoryExpenseRepository]); nothing above the data layer needs to change.
 */
internal class InMemoryGroupRepository : GroupRepository {
    private val groups = MutableStateFlow<List<Group>>(emptyList())

    override fun observeGroups(): Flow<List<Group>> = groups.asStateFlow()

    override suspend fun createGroup(group: Group) {
        groups.update { listOf(group) + it } // Newest first.
    }

    /** Used by "Clear local data". */
    fun clear() {
        groups.value = emptyList()
    }
}
