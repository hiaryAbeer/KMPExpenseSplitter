package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlinx.datetime.LocalDate

/**
 * A set of people who share expenses.
 *
 * @property members everyone in the group. The device owner, when in it, has id
 *   [Participant.YOU_ID]; their stored name is replaced by the current profile
 *   name when read (see `ObservePeopleGroupsUseCase`).
 * @property createdOn used to order groups that have no expenses yet.
 */
data class PeopleGroup(
    val id: String,
    val name: String,
    val members: List<Participant>,
    val createdOn: LocalDate,
) {
    companion object {
        const val MAX_NAME_LENGTH = 50
        const val MAX_MEMBER_NAME_LENGTH = 50

        /** Other people you can add (you are added automatically). */
        const val MAX_OTHER_MEMBERS = 20
    }
}
