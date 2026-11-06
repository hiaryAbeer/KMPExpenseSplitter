package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.peopleGroupUsage
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Shows the device owner under their *current* profile name, even if it changed since the group was made. */
internal fun List<PeopleGroup>.withYourName(name: String?): List<PeopleGroup> =
    if (name == null) this
    else map { group ->
        group.copy(members = group.members.map { if (it.id == Participant.YOU_ID) it.copy(name = name) else it })
    }

/** Emits every group with its members (newest first), for pickers such as "Add Expense". */
class ObservePeopleGroupsUseCase(
    private val groups: PeopleGroupRepository,
    private val profiles: UserProfileRepository,
) {
    operator fun invoke(): Flow<List<PeopleGroup>> =
        combine(groups.observeGroups(), profiles.observeProfile()) { all, profile -> all.withYourName(profile?.name) }
}

/** Why a member name can't be added. */
enum class MemberNameError { Blank, TooLong, Duplicate, TooMany }

/** A rule a new or edited group breaks. */
enum class PeopleGroupDraftError { BlankName, NameTooLong, NoMembers, InvalidMembers }

/**
 * The people group rules, shared by create and edit (and by the UI to enable "Add"/"Save").
 *
 * Member names: 1–50 characters, unique ignoring case (including your own name while
 * you're in the group), at most [PeopleGroup.MAX_OTHER_MEMBERS] others. A group needs
 * a name and at least one member, which may be just you.
 */
object PeopleGroupRules {

    /**
     * Checks [name] before adding it to [existing] (other members).
     * @param yourName your name while you're in the group, else `null`.
     */
    fun validateMemberName(name: String, existing: List<String>, yourName: String?): MemberNameError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> MemberNameError.Blank
            trimmed.length > PeopleGroup.MAX_MEMBER_NAME_LENGTH -> MemberNameError.TooLong
            (existing + listOfNotNull(yourName)).any { it.trim().equals(trimmed, ignoreCase = true) } ->
                MemberNameError.Duplicate
            existing.size >= PeopleGroup.MAX_OTHER_MEMBERS -> MemberNameError.TooMany
            else -> null
        }
    }

    /** @param yourName your name while you're in the group, else `null`. */
    fun validate(name: String, otherMembers: List<String>, yourName: String?): List<PeopleGroupDraftError> = buildList {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) add(PeopleGroupDraftError.BlankName)
        if (trimmed.length > PeopleGroup.MAX_NAME_LENGTH) add(PeopleGroupDraftError.NameTooLong)
        if (otherMembers.isEmpty() && yourName == null) add(PeopleGroupDraftError.NoMembers)
        val membersValid = otherMembers.indices.all { i ->
            validateMemberName(otherMembers[i], otherMembers.take(i), yourName) == null
        }
        if (!membersValid) add(PeopleGroupDraftError.InvalidMembers)
    }
}

sealed interface CreatePeopleGroupResult {
    data class Success(val group: PeopleGroup) : CreatePeopleGroupResult
    data class Invalid(val errors: List<PeopleGroupDraftError>) : CreatePeopleGroupResult
    data object NoProfile : CreatePeopleGroupResult
    data object StorageError : CreatePeopleGroupResult
}

/**
 * Creates a group from a name, the names of the *other* members and whether the
 * device owner is in it (first, as [Participant.YOU_ID]). A group needs a name and
 * at least one member, which may be just you.
 *
 * Rules: [PeopleGroupRules].
 */
class CreatePeopleGroupUseCase(
    private val groups: PeopleGroupRepository,
    private val profiles: UserProfileRepository,
    private val dateProvider: DateProvider,
    private val newId: () -> String = ::randomId,
) {

    /** See [PeopleGroupRules.validateMemberName]. */
    fun validateMemberName(name: String, existing: List<String>, yourName: String?): MemberNameError? =
        PeopleGroupRules.validateMemberName(name, existing, yourName)

    /** See [PeopleGroupRules.validate]. */
    fun validate(name: String, otherMembers: List<String>, yourName: String?): List<PeopleGroupDraftError> =
        PeopleGroupRules.validate(name, otherMembers, yourName)

    suspend operator fun invoke(name: String, otherMembers: List<String>, includeYou: Boolean = true): CreatePeopleGroupResult {
        val profile = profiles.observeProfile().first() ?: return CreatePeopleGroupResult.NoProfile
        val errors = validate(name, otherMembers, profile.name.takeIf { includeYou })
        if (errors.isNotEmpty()) return CreatePeopleGroupResult.Invalid(errors)

        val group = PeopleGroup(
            id = newId(),
            name = name.trim(),
            members = listOfNotNull(Participant(Participant.YOU_ID, profile.name).takeIf { includeYou }) +
                otherMembers.map { Participant(newId(), it.trim()) },
            createdOn = dateProvider.today(),
        )
        return try {
            groups.createGroup(group)
            CreatePeopleGroupResult.Success(group)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            CreatePeopleGroupResult.StorageError
        }
    }

    private companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun randomId(): String = Uuid.random().toString()
    }
}

/**
 * A member in the people group form: an existing participant ([id] set) or a new
 * person ([id] `null`, an id is generated on save).
 */
data class MemberDraft(val id: String?, val name: String)

sealed interface UpdatePeopleGroupResult {
    data class Success(val group: PeopleGroup) : UpdatePeopleGroupResult
    data class Invalid(val errors: List<PeopleGroupDraftError>) : UpdatePeopleGroupResult

    /** Someone being removed is referenced by an expense (payer or share). */
    data object MemberInUse : UpdatePeopleGroupResult
    data object NotFound : UpdatePeopleGroupResult
    data object NoProfile : UpdatePeopleGroupResult
    data object StorageError : UpdatePeopleGroupResult
}

/**
 * Edits a people group: its name, whether you're in it, and its other members.
 * Existing members keep their ids, so expenses stay attached to the right people.
 * Business rule: people referenced by an expense of this group can't be removed.
 */
class UpdatePeopleGroupUseCase(
    private val groups: PeopleGroupRepository,
    private val expenses: ExpenseRepository,
    private val profiles: UserProfileRepository,
    private val newId: () -> String = ::randomId,
) {
    suspend operator fun invoke(
        groupId: String,
        name: String,
        otherMembers: List<MemberDraft>,
        includeYou: Boolean,
    ): UpdatePeopleGroupResult {
        val profile = profiles.observeProfile().first() ?: return UpdatePeopleGroupResult.NoProfile
        val current = groups.observeGroups().first().firstOrNull { it.id == groupId }
            ?: return UpdatePeopleGroupResult.NotFound
        val errors = PeopleGroupRules.validate(name, otherMembers.map { it.name }, profile.name.takeIf { includeYou })
        if (errors.isNotEmpty()) return UpdatePeopleGroupResult.Invalid(errors)

        val keptIds = otherMembers.mapNotNullTo(HashSet()) { it.id } + listOfNotNull(Participant.YOU_ID.takeIf { includeYou })
        val removedIds = current.members.map { it.id }.filterNot { it in keptIds }
        val usedIds = expenses.observeExpenses().first().peopleGroupUsage()[groupId].orEmpty()
        if (removedIds.any { it in usedIds }) return UpdatePeopleGroupResult.MemberInUse

        val updated = current.copy(
            name = name.trim(),
            members = listOfNotNull(Participant(Participant.YOU_ID, profile.name).takeIf { includeYou }) +
                otherMembers.map { Participant(it.id ?: newId(), it.name.trim()) },
        )
        return try {
            groups.updateGroup(updated)
            UpdatePeopleGroupResult.Success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            UpdatePeopleGroupResult.StorageError
        }
    }

    private companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun randomId(): String = Uuid.random().toString()
    }
}

enum class DeleteResult { Deleted, InUse, StorageError }

/** Deletes a people group. Business rule: only groups no expense uses can be deleted. */
class DeletePeopleGroupUseCase(
    private val groups: PeopleGroupRepository,
    private val expenses: ExpenseRepository,
) {
    suspend operator fun invoke(groupId: String): DeleteResult {
        if (expenses.observeExpenses().first().any { it.peopleGroupId == groupId }) return DeleteResult.InUse
        return try {
            groups.deleteGroup(groupId)
            DeleteResult.Deleted
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            DeleteResult.StorageError
        }
    }
}
