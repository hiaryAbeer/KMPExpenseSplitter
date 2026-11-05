package com.abeerapps.kmpexpensesplitter.core.domain.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import kotlinx.coroutines.flow.Flow

/** Source of truth for the user's groups. Implemented in `:core:data`. */
interface GroupRepository {

    /** Emits the user's groups, most recently active first; re-emits on change. */
    fun observeGroups(): Flow<List<GroupSummary>>
}
