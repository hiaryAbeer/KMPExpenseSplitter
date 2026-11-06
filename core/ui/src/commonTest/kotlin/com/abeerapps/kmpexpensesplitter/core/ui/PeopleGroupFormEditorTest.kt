package com.abeerapps.kmpexpensesplitter.core.ui

import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.MemberDraft
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.MemberNameError
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeopleGroupFormEditorTest {

    private val me = "Ahmad"
    private val editor = PeopleGroupFormEditor

    @Test
    fun `a name and you are enough to save a new group`() {
        val form = editor.nameChanged(PeopleGroupForm(), "Trip", me)
        assertTrue(form.canSave)
        assertFalse(editor.removeYou(form, me).canSave) // Nobody left.
    }

    @Test
    fun `adding validates names and your own name brings you back`() {
        var form = editor.nameChanged(PeopleGroupForm(), "Trip", me)
        form = editor.addMember(editor.memberInputChanged(form, "Sara", me), me)
        form = editor.addMember(editor.memberInputChanged(form, "sara", me), me)
        assertEquals(MemberNameError.Duplicate, form.memberError)
        assertEquals(listOf(MemberDraft(null, "Sara")), form.members)

        form = editor.removeYou(form, me)
        form = editor.addMember(editor.memberInputChanged(form, " ahmad ", me), me)
        assertTrue(form.includeYou)
        assertEquals(1, form.members.size)
    }

    @Test
    fun `a valid typed name counts when saving`() {
        var form = editor.removeYou(editor.nameChanged(PeopleGroupForm(), "Parents", me), me)
        form = editor.memberInputChanged(form, "Mom", me)
        assertTrue(form.canSave)
        assertEquals(listOf(MemberDraft(null, "Mom")), editor.candidateMembers(form, me))
    }

    @Test
    fun `editing prefills the group and locks people used in expenses`() {
        val group = PeopleGroup(
            id = "pg",
            name = "Friends",
            members = listOf(Participant(Participant.YOU_ID, me), Participant("s", "Sara"), Participant("o", "Omar")),
            createdOn = LocalDate(2026, 4, 29),
        )
        val form = PeopleGroupForm.forEdit(group, usedIds = setOf(Participant.YOU_ID, "s"), yourName = me)

        assertTrue(form.isEditing)
        assertTrue(form.includeYou)
        assertEquals(listOf(MemberDraft("s", "Sara"), MemberDraft("o", "Omar")), form.members)
        assertTrue(form.canSave)

        // Locked: you and Sara stay; Omar can go.
        assertEquals(form, editor.removeYou(form, me))
        assertEquals(form, editor.removeMember(form, 0, me))
        assertEquals(listOf(MemberDraft("s", "Sara")), editor.removeMember(form, 1, me).members)
    }
}
