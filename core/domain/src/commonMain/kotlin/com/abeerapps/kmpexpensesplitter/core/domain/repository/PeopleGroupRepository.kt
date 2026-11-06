package com.abeerapps.kmpexpensesplitter.core.domain.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import kotlinx.coroutines.flow.Flow

/** Source of truth for the user's groups. Implemented in `:core:data`. */
interface PeopleGroupRepository {

    /** Emits every group, newest first; re-emits on change. */
    fun observeGroups(): Flow<List<PeopleGroup>>

    /** Stores [group]. Throws on storage failure. */
    suspend fun createGroup(group: PeopleGroup)

    /** Replaces the group with [group]'s id. Throws on storage failure. */
    suspend fun updateGroup(group: PeopleGroup)

    /** Removes the group with [groupId], if present. Throws on storage failure. */
    suspend fun deleteGroup(groupId: String)
}
