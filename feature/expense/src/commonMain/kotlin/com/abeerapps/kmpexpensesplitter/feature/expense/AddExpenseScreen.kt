package com.abeerapps.kmpexpensesplitter.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTextField
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTopBar
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.FieldLabel
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.PrimaryButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_arrow_left
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.designsystem.util.ObserveAsEvents
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.ui.CreateExpenseGroupSheet
import com.abeerapps.kmpexpensesplitter.core.ui.PeopleGroupSheet
import com.abeerapps.kmpexpensesplitter.core.ui.PeopleGroupSheetCallbacks
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_back
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_label_amount
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_label_category
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_label_date
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_label_expense_group
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_label_group
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.new_group_failed
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_label_note
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_label_paid_by
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_label_split_between
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_label_split_type
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_label_title
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_note_placeholder
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_save
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_save_failed
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_title
import com.abeerapps.kmpexpensesplitter.feature.expense.resources.add_expense_title_placeholder
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Test tags for UI tests. */
object AddExpenseTestTags {
    const val TITLE = "add_expense_title"
    const val AMOUNT = "add_expense_amount"
    const val SAVE = "add_expense_save"
}

/**
 * Stateful entry point for "Add Expense".
 *
 * @param onBack back arrow tapped.
 * @param onSaved the expense was stored; the caller leaves the screen.
 */
@Composable
fun AddExpenseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddExpenseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AddExpenseEvent.Saved -> onSaved()
        }
    }
    AddExpenseContent(uiState = uiState, onAction = viewModel::onAction, onBack = onBack)
}

/** Stateless "Add Expense" UI. Previewable with any [AddExpenseUiState]. */
@Composable
fun AddExpenseContent(
    uiState: AddExpenseUiState,
    onAction: (AddExpenseAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ExpenseSplitterTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailed = stringResource(Res.string.add_expense_save_failed)
    val createGroupFailed = stringResource(Res.string.new_group_failed)

    LaunchedEffect(uiState.message) {
        val text = when (uiState.message) {
            AddExpenseMessage.SaveFailed -> saveFailed
            AddExpenseMessage.CreateGroupFailed -> createGroupFailed
            null -> return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(text)
        onAction(AddExpenseAction.MessageShown)
    }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                title = stringResource(Res.string.add_expense_title),
                onBack = onBack,
                backDescription = stringResource(Res.string.add_expense_back),
            )
            if (!uiState.isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    FormContent(uiState, onAction)
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }

    when (uiState.openSheet) {
        AddExpenseSheet.ExpenseGroup -> ExpenseGroupSheet(
            query = uiState.expenseGroupQuery,
            allCount = uiState.expenseGroups.size,
            results = uiState.filteredExpenseGroups,
            selectedId = uiState.expenseGroupId,
            onQueryChange = { onAction(AddExpenseAction.ExpenseGroupQueryChanged(it)) },
            onSelect = { onAction(AddExpenseAction.ExpenseGroupSelected(it)) },
            onAdd = { onAction(AddExpenseAction.AddExpenseGroupClicked) },
            onDismiss = { onAction(AddExpenseAction.SheetDismissed) },
        )
        AddExpenseSheet.CreateExpenseGroup -> CreateExpenseGroupSheet(
            name = uiState.newExpenseGroup.name,
            canCreate = uiState.newExpenseGroup.canCreate,
            isCreating = uiState.newExpenseGroup.isCreating,
            onNameChange = { onAction(AddExpenseAction.NewExpenseGroupNameChanged(it)) },
            onCreate = { onAction(AddExpenseAction.CreateExpenseGroupClicked) },
            onDismiss = { onAction(AddExpenseAction.SheetDismissed) },
        )
        AddExpenseSheet.PeopleGroup -> GroupSheet(
            groups = uiState.groups,
            selectedId = uiState.groupId,
            onSelect = { onAction(AddExpenseAction.GroupSelected(it)) },
            onAddGroup = { onAction(AddExpenseAction.AddGroupClicked) },
            onDismiss = { onAction(AddExpenseAction.SheetDismissed) },
        )
        AddExpenseSheet.CreateGroup -> PeopleGroupSheet(
            form = uiState.newGroup,
            yourName = uiState.you?.name.orEmpty(),
            callbacks = PeopleGroupSheetCallbacks(
                onNameChange = { onAction(AddExpenseAction.NewGroupNameChanged(it)) },
                onMemberInputChange = { onAction(AddExpenseAction.MemberInputChanged(it)) },
                onAddMember = { onAction(AddExpenseAction.AddMemberClicked) },
                onRemoveMember = { onAction(AddExpenseAction.MemberRemoved(it)) },
                onRemoveYou = { onAction(AddExpenseAction.YouRemoved) },
                onSave = { onAction(AddExpenseAction.CreateGroupClicked) },
                onDismiss = { onAction(AddExpenseAction.SheetDismissed) },
            ),
        )
        AddExpenseSheet.PaidBy -> PaidBySheet(
            participants = uiState.payerOptions,
            selectedId = uiState.paidBy,
            onSelect = { onAction(AddExpenseAction.PayerSelected(it)) },
            onDismiss = { onAction(AddExpenseAction.SheetDismissed) },
        )
        AddExpenseSheet.Date -> ExpenseDateRangeSheet(
            start = uiState.startDate,
            end = uiState.endDate,
            today = uiState.today,
            onSelect = { start, end -> onAction(AddExpenseAction.DateRangeSelected(start, end)) },
            onDismiss = { onAction(AddExpenseAction.SheetDismissed) },
        )
        null -> Unit
    }
}

@Composable
private fun FormContent(uiState: AddExpenseUiState, onAction: (AddExpenseAction) -> Unit) {
    val spacing = ExpenseSplitterTheme.spacing

    Section(stringResource(Res.string.add_expense_label_title)) {
        AppTextField(
            value = uiState.title,
            onValueChange = { onAction(AddExpenseAction.TitleChanged(it)) },
            placeholder = stringResource(Res.string.add_expense_title_placeholder),
            leadingIcon = null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.testTag(AddExpenseTestTags.TITLE),
        )
    }

    Section(stringResource(Res.string.add_expense_label_expense_group)) {
        ExpenseGroupField(expenseGroup = uiState.expenseGroup, onClick = { onAction(AddExpenseAction.ExpenseGroupClicked) })
    }

    Section(stringResource(Res.string.add_expense_label_date)) {
        DateCard(
            start = uiState.startDate,
            end = uiState.endDate,
            today = uiState.today,
            onClick = { onAction(AddExpenseAction.DateClicked) },
        )
    }

    Section(stringResource(Res.string.add_expense_label_group)) {
        Box(Modifier.testTag(GroupPickerTestTags.FIELD)) {
            GroupField(group = uiState.group, onClick = { onAction(AddExpenseAction.GroupClicked) })
        }
    }

    Section(stringResource(Res.string.add_expense_label_amount)) {
        AmountField(
            value = uiState.amount,
            currency = uiState.currency,
            onValueChange = { onAction(AddExpenseAction.AmountChanged(it)) },
            modifier = Modifier.testTag(AddExpenseTestTags.AMOUNT),
        )
    }

    Section(stringResource(Res.string.add_expense_label_category), contentGap = spacing.smd) {
        CategoryGrid(selected = uiState.category, onSelect = { onAction(AddExpenseAction.CategorySelected(it)) })
    }

    Section(stringResource(Res.string.add_expense_label_paid_by)) {
        PaidByCard(payer = uiState.payer, onClick = { onAction(AddExpenseAction.PaidByClicked) })
    }

    Section(stringResource(Res.string.add_expense_label_split_type)) {
        SplitTypeSelector(selected = uiState.splitType, onSelect = { onAction(AddExpenseAction.SplitTypeSelected(it)) })
        SplitHint(type = uiState.splitType, isError = uiState.isSplitHintError)
    }

    Section(stringResource(Res.string.add_expense_label_split_between), contentGap = spacing.smd) {
        if (uiState.group == null) {
            SplitBetweenDisabled()
        } else Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            uiState.participants.forEach { person ->
                ParticipantRow(
                    participant = person,
                    isSelected = person.id in uiState.selected,
                    splitType = uiState.splitType,
                    shareInput = uiState.shareInput(person.id),
                    currency = uiState.currency,
                    onToggle = { onAction(AddExpenseAction.ParticipantToggled(person.id)) },
                    onShareChange = { onAction(AddExpenseAction.ShareChanged(person.id, it)) },
                )
            }
        }
    }

    Section(stringResource(Res.string.add_expense_label_note)) {
        AppTextField(
            value = uiState.note,
            onValueChange = { onAction(AddExpenseAction.NoteChanged(it)) },
            placeholder = stringResource(Res.string.add_expense_note_placeholder),
            leadingIcon = null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        )
    }

    SaveButton(enabled = uiState.canSave, isSaving = uiState.isSaving, onClick = { onAction(AddExpenseAction.SaveClicked) })
}

/** A labelled block of the form: label, then [content] [contentGap] below it. */
@Composable
private fun Section(
    label: String,
    contentGap: androidx.compose.ui.unit.Dp = ExpenseSplitterTheme.spacing.sm,
    content: @Composable () -> Unit,
) {
    Column {
        FieldLabel(label)
        Spacer(Modifier.height(contentGap))
        content()
    }
}

/**
 * Filled teal when the form is valid; until then the design's outlined look
 * (transparent with a hairline border), so it reads as "not ready yet".
 */
@Composable
private fun SaveButton(enabled: Boolean, isSaving: Boolean, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.large
    PrimaryButton(
        text = stringResource(Res.string.add_expense_save),
        onClick = onClick,
        enabled = enabled || isSaving,
        isLoading = isSaving,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier
            .testTag(AddExpenseTestTags.SAVE)
            .then(
                if (enabled || isSaving) Modifier
                else Modifier.border(ExpenseSplitterTheme.borders.hairline, MaterialTheme.colorScheme.outlineVariant, shape),
            ),
    )
}

// region Previews

private val PreviewToday = LocalDate(2026, 4, 29)

private fun previewState(splitType: SplitType = SplitType.Equal) = AddExpenseUiState(
    startDate = PreviewToday,
    endDate = PreviewToday,
    expenseGroups = listOf(ExpenseGroup("t", "Aqaba Trip", PreviewToday)),
    expenseGroupId = "t",
    today = PreviewToday,
    isLoading = false,
    currency = CurrencyCode.of("JOD")!!,
    you = Participant(Participant.YOU_ID, "Ahmad"),
    groups = listOf(
        PeopleGroup(
            id = "g",
            name = "Aqaba Trip",
            members = listOf(
                Participant(Participant.YOU_ID, "Ahmad"),
                Participant("s", "Sara"),
                Participant("o", "Omar"),
                Participant("l", "Lina"),
            ),
            createdOn = PreviewToday,
        ),
    ),
    groupId = "g",
    paidBy = Participant.YOU_ID,
    selected = setOf(Participant.YOU_ID, "s", "o", "l"),
    splitType = splitType,
)

@Preview
@Composable
private fun AddExpenseEqualPreview() {
    ExpenseSplitterTheme { AddExpenseContent(previewState(), onAction = {}, onBack = {}) }
}

@Preview
@Composable
private fun AddExpensePercentagePreview() {
    ExpenseSplitterTheme { AddExpenseContent(previewState(SplitType.Percentage), onAction = {}, onBack = {}) }
}

/** Fresh form: "No group", so "Split Between" is disabled. */
@Preview
@Composable
private fun AddExpenseNoGroupPreview() {
    ExpenseSplitterTheme {
        AddExpenseContent(
            AddExpenseUiState(startDate = PreviewToday, endDate = PreviewToday, today = PreviewToday, isLoading = false, you = Participant(Participant.YOU_ID, "Ahmad"), paidBy = Participant.YOU_ID),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview
@Composable
private fun AddExpenseExactDarkPreview() {
    ExpenseSplitterTheme(darkTheme = true) {
        AddExpenseContent(previewState(SplitType.Exact), onAction = {}, onBack = {})
    }
}

// endregion
