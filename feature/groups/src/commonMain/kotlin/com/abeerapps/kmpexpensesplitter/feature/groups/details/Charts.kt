package com.abeerapps.kmpexpensesplitter.feature.groups.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Axis maths for the bar chart, kept pure so it is unit-tested. */
internal object ChartScale {

    private val niceSteps = listOf(1.0, 1.5, 2.0, 2.5, 3.0, 5.0, 10.0)

    /**
     * Round axis ticks from 0 up to at least [maxMinor] (money in minor units), about
     * [intervals] steps apart: 590.00 → 0, 150, 300, 450, 600. An empty chart still
     * gets a 0–1.00 scale.
     */
    fun ticks(maxMinor: Long, intervals: Int = 4): List<Long> {
        val top = max(maxMinor, Money100)
        val raw = top.toDouble() / intervals
        val magnitude = 10.0.pow(floor(log10(raw)))
        val step = ceil(niceSteps.first { it * magnitude >= raw } * magnitude).toLong()
        val count = ceil(top.toDouble() / step).toInt()
        return (0..count).map { it * step }
    }

    /** A tick in major units without needless decimals: 15000 → "150", 250 → "2.5". */
    fun label(minor: Long): String {
        val whole = minor / 100
        val cents = minor % 100
        return when {
            cents == 0L -> whole.toString()
            cents % 10 == 0L -> "$whole.${cents / 10}"
            else -> "$whole.${cents.toString().padStart(2, '0')}"
        }
    }

    private const val Money100 = 100L
}

@Immutable
internal data class PieSlice(val label: String, val fraction: Float, val color: Color)

/**
 * Pie chart as in the design: slices drawn counter-clockwise from 3 o'clock,
 * separated by thin card-colored lines, each labelled "Hotel 45%" outside.
 * Slices under 3% stay unlabelled so labels never pile up; the legend lists them.
 */
@Composable
internal fun PieChart(slices: List<PieSlice>, description: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val separator = MaterialTheme.colorScheme.surface
    val labelStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(250.dp)
            .semantics { contentDescription = description },
    ) {
        val radius = min(80.dp.toPx(), size.minDimension / 2 - 24.dp.toPx())
        val arcSize = Size(radius * 2, radius * 2)
        val topLeft = Offset(center.x - radius, center.y - radius)

        var start = 0f
        val boundaries = mutableListOf<Float>()
        slices.forEach { slice ->
            val sweep = slice.fraction * 360f
            drawArc(slice.color, startAngle = start, sweepAngle = -sweep, useCenter = true, topLeft = topLeft, size = arcSize)
            boundaries += start
            start -= sweep
        }
        if (slices.size > 1) {
            boundaries.forEach { angle -> drawLine(separator, center, pointOnCircle(angle, radius), strokeWidth = 1.5.dp.toPx()) }
        }

        start = 0f
        slices.forEach { slice ->
            val sweep = slice.fraction * 360f
            if (slice.fraction >= 0.03f) {
                drawSliceLabel(measurer, slice, labelStyle, angle = start - sweep / 2, distance = radius + 12.dp.toPx())
            }
            start -= sweep
        }
    }
}

private fun DrawScope.pointOnCircle(angleDegrees: Float, distance: Float): Offset {
    val radians = angleDegrees * PI.toFloat() / 180f
    return Offset(center.x + cos(radians) * distance, center.y + sin(radians) * distance)
}

/** Places a label just outside the slice: left of the point on the left half, right of it on the right. */
private fun DrawScope.drawSliceLabel(measurer: TextMeasurer, slice: PieSlice, style: TextStyle, angle: Float, distance: Float) {
    val anchor = pointOnCircle(angle, distance)
    val layout = measurer.measure(
        text = slice.label,
        style = style.copy(color = slice.color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        constraints = Constraints(maxWidth = (size.width / 2).toInt()),
    )
    val width = layout.size.width.toFloat()
    val height = layout.size.height.toFloat()
    val radians = angle * PI.toFloat() / 180f
    val x = if (cos(radians) >= 0f) anchor.x else anchor.x - width
    val y = when {
        sin(radians) < -0.5f -> anchor.y - height // Top: sit above the point.
        sin(radians) > 0.5f -> anchor.y // Bottom: hang below it.
        else -> anchor.y - height / 2
    }
    drawText(
        layout,
        topLeft = Offset(x.coerceIn(0f, max(0f, size.width - width)), y.coerceIn(0f, max(0f, size.height - height))),
    )
}

@Immutable
internal data class BarValue(val label: String, val valueMinor: Long)

/**
 * Bar chart as in the design: a left axis with round ticks, one rounded teal bar
 * per member and their name underneath.
 */
@Composable
internal fun BarChart(bars: List<BarValue>, description: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val axisColor = colors.onSurfaceVariant.copy(alpha = 0.6f)
    val labelStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, color = colors.onSurfaceVariant)
    val ticks = ChartScale.ticks(bars.maxOfOrNull { it.valueMinor } ?: 0L)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = description },
    ) {
        val tickLabels = ticks.map { measurer.measure(ChartScale.label(it), labelStyle) }
        val tickLength = 6.dp.toPx()
        val axisX = (tickLabels.maxOf { it.size.width } + tickLength + 4.dp.toPx())
        val top = tickLabels.first().size.height / 2f
        val bottom = size.height - 22.dp.toPx()
        val right = size.width - 4.dp.toPx()
        val plotHeight = bottom - top
        val maxTick = ticks.last().toFloat()

        ticks.forEachIndexed { i, tick ->
            val y = bottom - tick / maxTick * plotHeight
            val label = tickLabels[i]
            drawText(label, topLeft = Offset(axisX - tickLength - 4.dp.toPx() - label.size.width, y - label.size.height / 2f))
            drawLine(axisColor, Offset(axisX - tickLength, y), Offset(axisX, y), strokeWidth = 1.dp.toPx())
        }
        drawLine(axisColor, Offset(axisX, top), Offset(axisX, bottom), strokeWidth = 1.dp.toPx())
        drawLine(axisColor, Offset(axisX, bottom), Offset(right, bottom), strokeWidth = 1.dp.toPx())

        if (bars.isEmpty()) return@Canvas
        val slot = (right - axisX) / bars.size
        val barWidth = min(48.dp.toPx(), slot * 0.7f)
        bars.forEachIndexed { i, bar ->
            val centerX = axisX + slot * (i + 0.5f)
            val height = bar.valueMinor / maxTick * plotHeight
            if (height > 0f) drawTopRoundedBar(colors.primary, Rect(centerX - barWidth / 2, bottom - height, centerX + barWidth / 2, bottom))
            drawLine(axisColor, Offset(centerX, bottom), Offset(centerX, bottom + tickLength), strokeWidth = 1.dp.toPx())
            val name = measurer.measure(
                text = bar.label,
                style = labelStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(maxWidth = slot.toInt().coerceAtLeast(1)),
            )
            drawText(name, topLeft = Offset(centerX - name.size.width / 2f, bottom + tickLength + 2.dp.toPx()))
        }
    }
}

private fun DrawScope.drawTopRoundedBar(color: Color, rect: Rect) {
    val radius = CornerRadius(min(4.dp.toPx(), rect.height))
    val path = Path().apply {
        addRoundRect(RoundRect(rect, topLeft = radius, topRight = radius, bottomRight = CornerRadius.Zero, bottomLeft = CornerRadius.Zero))
    }
    drawPath(path, color)
}
