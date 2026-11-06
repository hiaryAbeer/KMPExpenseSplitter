package com.abeerapps.kmpexpensesplitter.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme

/**
 * Generic empty state from the design: tinted icon tile, title, message and an
 * optional call to action. Reuse it for any empty list in the app.
 *
 * @param icon drawn tinted with the primary color, at 96dp in a 160dp tile.
 * @param action e.g. a [PrimaryButton]; omit when the screen already offers the
 *   action elsewhere (Home has the "+" button, so its empty state has none).
 */
@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    // No outer padding: the screen controls spacing around components, so gaps
    // never add up (e.g. header's 16dp + an extra 16dp here would be 32dp).
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null, // Decorative; the title describes the state.
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(46.dp),
            )
        }
        Spacer(Modifier.height(ExpenseSplitterTheme.spacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(ExpenseSplitterTheme.spacing.sm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 315.dp),
        )
        if (action != null) {
            Spacer(Modifier.height(ExpenseSplitterTheme.spacing.md))
            action()
        }
    }
}
