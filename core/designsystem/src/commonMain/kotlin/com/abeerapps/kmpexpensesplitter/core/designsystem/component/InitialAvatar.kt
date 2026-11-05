package com.abeerapps.kmpexpensesplitter.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme

/**
 * A person's first initial on their stable avatar colour (see
 * [com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExtendedColors.avatarColorFor]).
 * Decorative: the person's name is always shown next to it.
 */
@Composable
fun InitialAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val colors = ExpenseSplitterTheme.extendedColors
    Box(
        modifier = modifier.size(size).background(colors.avatarColorFor(name), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onAvatar,
        )
    }
}
