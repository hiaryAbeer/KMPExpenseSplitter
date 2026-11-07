package com.abeerapps.kmpexpensesplitter.feature.groups.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppBottomSheet
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppCard
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.InitialAvatar
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.PrimaryButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroupDetails
import com.abeerapps.kmpexpensesplitter.core.domain.model.PersonBalance
import com.abeerapps.kmpexpensesplitter.core.domain.model.Transfer
import com.abeerapps.kmpexpensesplitter.core.ui.format
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_gets_back
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_mark_settled
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_owes
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_settle_up
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_settle_up_message
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_settled
import com.abeerapps.kmpexpensesplitter.feature.groups.resources.details_transfer
import org.jetbrains.compose.resources.stringResource

/**
 * Balances tab: one card per person, then "Settle Up" while anyone still owes money.
 * Once the group is marked as settled every card reads "Settled" and the button is gone.
 */
internal fun LazyListScope.balances(details: ExpenseGroupDetails, canSettleUp: Boolean, onSettleUp: () -> Unit) {
    items(details.balances, key = { it.participantId }) { BalanceRow(it) }
    if (canSettleUp) {
        item(key = "settle-up") {
            PrimaryButton(
                text = stringResource(Res.string.details_settle_up),
                onClick = onSettleUp,
                modifier = Modifier.padding(top = ExpenseSplitterTheme.spacing.md).testTag(GroupDetailsTestTags.SETTLE_UP),
            )
        }
    }
}

/** Avatar, name, "Gets back" / "Owes" / "Settled", and the signed amount in green or red. */
@Composable
private fun BalanceRow(person: PersonBalance) {
    val colors = MaterialTheme.colorScheme
    val extended = ExpenseSplitterTheme.extendedColors
    val balance = person.balance
    val (status, amountColor) = when {
        balance.isPositive -> stringResource(Res.string.details_gets_back) to extended.positive
        balance.isNegative -> stringResource(Res.string.details_owes) to extended.negative
        else -> stringResource(Res.string.details_settled) to colors.onSurfaceVariant
    }
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(person.name, size = 40.dp)
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
            Column(Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = ExpenseSplitterTheme.spacing.xs),
                )
            }
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.sm))
            Text(text = balance.format(showSign = true), style = MaterialTheme.typography.titleSmall, color = amountColor)
        }
    }
}

/**
 * The payments that clear every balance ("Lina pays Ahmad 52.75 JOD") and
 * "Mark as Settled", which records them all. Dismissable, so no Cancel button.
 */
@Composable
internal fun SettleUpSheet(
    details: ExpenseGroupDetails,
    isSettling: Boolean,
    onMarkSettled: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(title = stringResource(Res.string.details_settle_up), onDismiss = onDismiss) {
        Text(
            text = stringResource(Res.string.details_settle_up_message, details.group.name),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        details.transfers.forEach { TransferRow(it) }
        PrimaryButton(
            text = stringResource(Res.string.details_mark_settled),
            onClick = onMarkSettled,
            isLoading = isSettling,
            modifier = Modifier.testTag(GroupDetailsTestTags.MARK_SETTLED),
        )
    }
}

@Composable
private fun TransferRow(transfer: Transfer) {
    AppCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialAvatar(transfer.fromName, size = 40.dp)
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.smd))
            Text(
                text = stringResource(Res.string.details_transfer, transfer.fromName, transfer.toName),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(ExpenseSplitterTheme.spacing.sm))
            Text(
                text = transfer.amount.format(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
