package com.abeerapps.kmpexpensesplitter.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppCard
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_users
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.ui.resources.Res
import com.abeerapps.kmpexpensesplitter.core.ui.resources.group_card_description
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val MAX_VISIBLE_AVATARS = 3
private val AvatarSize = 32.dp
private val AvatarOverlapStep = 24.dp // Each avatar overlaps the previous by 8dp.

/**
 * One expense group (Home, Groups tab): icon tile | name + your balance / member avatars + total.
 * A settled group (zero balance) shows no balance, as in the design.
 */
@Composable
fun GroupCard(
    group: GroupSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ExpenseSplitterTheme.extendedColors
    val description = stringResource(
        Res.string.group_card_description,
        group.name,
        group.yourBalance.format(showSign = true),
        group.totalSpent.format(),
    )

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            // Read as one item by screen readers instead of 8 separate texts.
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(DsRes.drawable.ic_users),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (!group.yourBalance.isZero) {
                        Text(
                            text = group.yourBalance.format(showSign = true),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (group.yourBalance.isPositive) colors.positive else colors.negative,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarStack(group.memberNames, Modifier.weight(1f))
                    Text(
                        text = group.totalSpent.format(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Up to 3 overlapping initials, then a "+N" bubble for the rest. */
@Composable
fun AvatarStack(names: List<String>, modifier: Modifier = Modifier) {
    val colors = ExpenseSplitterTheme.extendedColors
    val visible = if (names.size > MAX_VISIBLE_AVATARS) names.take(MAX_VISIBLE_AVATARS) else names
    val overflow = names.size - visible.size

    Box(modifier.height(AvatarSize)) {
        visible.forEachIndexed { index, name ->
            Avatar(
                text = name.trim().take(1).uppercase(),
                background = colors.avatarColorFor(name),
                textColor = colors.onAvatar,
                modifier = Modifier.offset(x = AvatarOverlapStep * index),
            )
        }
        if (overflow > 0) {
            Avatar(
                text = "+$overflow",
                background = colors.avatarOverflow,
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.offset(x = AvatarOverlapStep * visible.size),
            )
        }
    }
}

@Composable
private fun Avatar(
    text: String,
    background: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(AvatarSize)
            .background(background, CircleShape)
            .border(1.46.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = textColor)
    }
}
