package com.abeerapps.kmpexpensesplitter.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.abeerapps.kmpexpensesplitter.core.data.db.AppDatabase
import com.abeerapps.kmpexpensesplitter.core.data.db.SelectAllWithMembers
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * [PeopleGroupRepository] stored in SQLite: one row per group plus one per member.
 * Groups and their members are written in one transaction and read with one join,
 * so observers never see a group with half its members.
 */
internal class SqlDelightPeopleGroupRepository(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : PeopleGroupRepository {
    private val queries = database.peopleGroupEntityQueries

    override fun observeGroups(): Flow<List<PeopleGroup>> =
        queries.selectAllWithMembers().asFlow().mapToList(dispatcher).map(::toGroups)

    override suspend fun createGroup(group: PeopleGroup) {
        withContext(dispatcher) {
            database.transaction {
                queries.insertGroup(group.id, group.name, group.createdOn.toString())
                insertMembers(group)
            }
        }
    }

    override suspend fun updateGroup(group: PeopleGroup) {
        withContext(dispatcher) {
            database.transaction {
                queries.updateGroup(name = group.name, createdOn = group.createdOn.toString(), id = group.id)
                queries.deleteMembers(group.id)
                insertMembers(group)
            }
        }
    }

    override suspend fun deleteGroup(groupId: String) {
        withContext(dispatcher) {
            database.transaction {
                queries.deleteMembers(groupId)
                queries.deleteGroup(groupId)
            }
        }
    }

    private fun insertMembers(group: PeopleGroup) {
        group.members.forEachIndexed { position, member ->
            queries.insertMember(group.id, member.id, member.name, position.toLong())
        }
    }

    /** Rows arrive grouped by group (newest first) and in member order. */
    private fun toGroups(rows: List<SelectAllWithMembers>): List<PeopleGroup> =
        rows.groupBy { it.id }.values.map { groupRows ->
            val first = groupRows.first()
            PeopleGroup(
                id = first.id,
                name = first.name,
                members = groupRows.mapNotNull { row ->
                    row.member_id?.let { Participant(it, requireNotNull(row.member_name)) }
                },
                createdOn = LocalDate.parse(first.created_on),
            )
        }
}
