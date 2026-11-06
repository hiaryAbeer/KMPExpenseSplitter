package com.abeerapps.kmpexpensesplitter.core.ui

import androidx.compose.runtime.Immutable
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.MemberDraft
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.MemberNameError
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.PeopleGroupRules

/**
 * State of the people group sheet, for creating ([editingId] `null`) or editing a group.
 * Shared by Add Expense and "Manage people groups", so both follow the same rules.
 *
 * @property includeYou whether you are in the group (removable like anyone else).
 * @property members the other people: existing ones keep their id, new ones have none.
 * @property lockedIds people referenced by an expense of this group: they can't be removed.
 * @property memberInput the name being typed, added with the "+" button.
 * @property canSave name and members are valid (also counting a valid name still typed).
 */
@Immutable
data class PeopleGroupForm(
    val editingId: String? = null,
    val name: String = "",
    val includeYou: Boolean = true,
    val members: List<MemberDraft> = emptyList(),
    val lockedIds: Set<String> = emptySet(),
    val memberInput: String = "",
    val memberError: MemberNameError? = null,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean get() = editingId != null

    val canRemoveYou: Boolean get() = Participant.YOU_ID !in lockedIds

    fun canRemove(member: MemberDraft): Boolean = member.id == null || member.id !in lockedIds

    companion object {
        /** Prefilled from [group]; [usedIds] are the people its expenses reference. */
        fun forEdit(group: PeopleGroup, usedIds: Set<String>, yourName: String?): PeopleGroupForm =
            PeopleGroupFormEditor.refreshed(
                PeopleGroupForm(
                    editingId = group.id,
                    name = group.name,
                    includeYou = group.members.any { it.id == Participant.YOU_ID },
                    members = group.members.filter { it.id != Participant.YOU_ID }.map { MemberDraft(it.id, it.name) },
                    lockedIds = usedIds,
                ),
                yourName,
            )
    }
}

/**
 * Pure state transitions of [PeopleGroupForm] (the rules come from [PeopleGroupRules]).
 * Every function takes your current profile name (`null` if unknown) and returns
 * the next form with [PeopleGroupForm.canSave] recomputed.
 */
object PeopleGroupFormEditor {

    fun nameChanged(form: PeopleGroupForm, value: String, yourName: String?): PeopleGroupForm =
        if (value.length > PeopleGroup.MAX_NAME_LENGTH) form else refreshed(form.copy(name = value), yourName)

    fun memberInputChanged(form: PeopleGroupForm, value: String, yourName: String?): PeopleGroupForm =
        if (value.length > PeopleGroup.MAX_MEMBER_NAME_LENGTH) form
        else refreshed(form.copy(memberInput = value, memberError = null), yourName)

    /** Adds the typed name. Typing your own name after removing yourself adds *you* back. */
    fun addMember(form: PeopleGroupForm, yourName: String?): PeopleGroupForm {
        if (!form.includeYou && isYourName(form.memberInput, yourName)) {
            return refreshed(form.copy(includeYou = true, memberInput = "", memberError = null), yourName)
        }
        val error = PeopleGroupRules.validateMemberName(form.memberInput, form.members.map { it.name }, yourNameIn(form, yourName))
        return refreshed(
            if (error != null) form.copy(memberError = error)
            else form.copy(members = form.members + MemberDraft(null, form.memberInput.trim()), memberInput = "", memberError = null),
            yourName,
        )
    }

    fun removeMember(form: PeopleGroupForm, index: Int, yourName: String?): PeopleGroupForm {
        val member = form.members.getOrNull(index) ?: return form
        if (!form.canRemove(member)) return form
        return refreshed(form.copy(members = form.members.filterIndexed { i, _ -> i != index }), yourName)
    }

    fun removeYou(form: PeopleGroupForm, yourName: String?): PeopleGroupForm =
        if (!form.canRemoveYou) form else refreshed(form.copy(includeYou = false), yourName)

    /** Whether you'll be in the group: the toggle, or your own name still typed in the input. */
    fun candidateIncludesYou(form: PeopleGroupForm, yourName: String?): Boolean =
        form.includeYou || isYourName(form.memberInput, yourName)

    /** The other members to save: the list plus a valid name still in the input. */
    fun candidateMembers(form: PeopleGroupForm, yourName: String?): List<MemberDraft> {
        val pending = form.memberInput.trim()
        val pendingIsValid = pending.isNotEmpty() &&
            !(!form.includeYou && isYourName(pending, yourName)) &&
            PeopleGroupRules.validateMemberName(pending, form.members.map { it.name }, yourNameIn(form, yourName)) == null
        return if (pendingIsValid) form.members + MemberDraft(null, pending) else form.members
    }

    internal fun refreshed(form: PeopleGroupForm, yourName: String?): PeopleGroupForm {
        val you = yourName?.takeIf { candidateIncludesYou(form, yourName) }
        val valid = PeopleGroupRules.validate(form.name, candidateMembers(form, yourName).map { it.name }, you).isEmpty()
        return form.copy(canSave = valid)
    }

    private fun yourNameIn(form: PeopleGroupForm, yourName: String?): String? = yourName?.takeIf { form.includeYou }

    private fun isYourName(name: String, yourName: String?): Boolean =
        yourName != null && name.trim().equals(yourName.trim(), ignoreCase = true)
}
