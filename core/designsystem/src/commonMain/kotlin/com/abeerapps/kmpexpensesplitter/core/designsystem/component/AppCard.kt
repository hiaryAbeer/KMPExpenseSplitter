package com.abeerapps.kmpexpensesplitter.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme

/**
 * The design's card: surface fill, 16dp radius, hairline border and a soft
 * 1dp shadow. Used for balance cards, the search bar and group rows.
 *
 * @param tint optional brush drawn over the surface (e.g. the green/red
 *   gradients of the balance cards).
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    tint: Brush? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .shadow(elevation = 1.dp, shape = shape, clip = false)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (tint != null) Modifier.background(tint) else Modifier)
            .border(ExpenseSplitterTheme.borders.hairline, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(contentPadding),
        content = content,
    )
}
