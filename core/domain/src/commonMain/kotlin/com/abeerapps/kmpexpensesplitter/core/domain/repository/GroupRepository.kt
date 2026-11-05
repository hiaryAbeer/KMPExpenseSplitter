package com.abeerapps.kmpexpensesplitter.core.domain.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.Group
import kotlinx.coroutines.flow.Flow

/** Source of truth for the user's groups. Implemented in `:core:data`. */
interface GroupRepository {

    /** Emits every group, newest first; re-emits on change. */
    fun observeGroups(): Flow<List<Group>>

    /** Stores [group]. Throws on storage failure. */
    suspend fun createGroup(group: Group)
}
