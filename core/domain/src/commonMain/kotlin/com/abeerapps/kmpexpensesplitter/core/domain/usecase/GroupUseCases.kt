package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.Group
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.repository.GroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Shows the device owner under their *current* profile name, even if it changed since the group was made. */
internal fun List<Group>.withYourName(name: String?): List<Group> =
    if (name == null) this
    else map { group ->
        group.copy(members = group.members.map { if (it.id == Participant.YOU_ID) it.copy(name = name) else it })
    }

/** Emits every group with its members (newest first), for pickers such as "Add Expense". */
class ObserveGroupListUseCase(
    private val groups: GroupRepository,
    private val profiles: UserProfileRepository,
) {
    operator fun invoke(): Flow<List<Group>> =
        combine(groups.observeGroups(), profiles.observeProfile()) { all, profile -> all.withYourName(profile?.name) }
}

/** Why a member name can't be added. */
enum class MemberNameError { Blank, TooLong, Duplicate, TooMany }

/** A rule a new group breaks. */
enum class GroupDraftError { BlankName, NameTooLong, NoMembers, InvalidMembers }

sealed interface CreateGroupResult {
    data class Success(val group: Group) : CreateGroupResult
    data class Invalid(val errors: List<GroupDraftError>) : CreateGroupResult
    data object NoProfile : CreateGroupResult
    data object StorageError : CreateGroupResult
}

/**
 * Creates a group from a name, the names of the *other* members and whether the
 * device owner is in it (first, as [Participant.YOU_ID]). A group needs a name and
 * at least one member, which may be just you.
 *
 * Member rules ([validateMemberName]): 1–50 characters, unique ignoring case
 * (including your own name while you're in the group), at most [Group.MAX_OTHER_MEMBERS] others.
 */
class CreateGroupUseCase(
    private val groups: GroupRepository,
    private val profiles: UserProfileRepository,
    private val dateProvider: DateProvider,
    private val newId: () -> String = ::randomId,
) {

    /**
     * Checks [name] before adding it to [existing] (other members).
     * @param yourName your name while you're in the group, else `null`.
     */
    fun validateMemberName(name: String, existing: List<String>, yourName: String?): MemberNameError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> MemberNameError.Blank
            trimmed.length > Group.MAX_MEMBER_NAME_LENGTH -> MemberNameError.TooLong
            (existing + listOfNotNull(yourName)).any { it.trim().equals(trimmed, ignoreCase = true) } ->
                MemberNameError.Duplicate
            existing.size >= Group.MAX_OTHER_MEMBERS -> MemberNameError.TooMany
            else -> null
        }
    }

    /** @param yourName your name while you're in the group, else `null`. */
    fun validate(name: String, otherMembers: List<String>, yourName: String?): List<GroupDraftError> = buildList {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) add(GroupDraftError.BlankName)
        if (trimmed.length > Group.MAX_NAME_LENGTH) add(GroupDraftError.NameTooLong)
        if (otherMembers.isEmpty() && yourName == null) add(GroupDraftError.NoMembers)
        val membersValid = otherMembers.indices.all { i ->
            validateMemberName(otherMembers[i], otherMembers.take(i), yourName) == null
        }
        if (!membersValid) add(GroupDraftError.InvalidMembers)
    }

    suspend operator fun invoke(name: String, otherMembers: List<String>, includeYou: Boolean = true): CreateGroupResult {
        val profile = profiles.observeProfile().first() ?: return CreateGroupResult.NoProfile
        val errors = validate(name, otherMembers, profile.name.takeIf { includeYou })
        if (errors.isNotEmpty()) return CreateGroupResult.Invalid(errors)

        val group = Group(
            id = newId(),
            name = name.trim(),
            members = listOfNotNull(Participant(Participant.YOU_ID, profile.name).takeIf { includeYou }) +
                otherMembers.map { Participant(newId(), it.trim()) },
            createdOn = dateProvider.today(),
        )
        return try {
            groups.createGroup(group)
            CreateGroupResult.Success(group)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            CreateGroupResult.StorageError
        }
    }

    private companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun randomId(): String = Uuid.random().toString()
    }
}
