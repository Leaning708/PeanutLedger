package com.peanut.ledger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.peanut.ledger.data.formatCompactAxis
import com.peanut.ledger.ui.theme.CardBorder
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.LineStrong
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.TextSoft
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

data class LineSeriesUi(
    val name: String,
    val color: Color,
    val values: List<Double?>
)

@Composable
fun LineChart(
    labels: List<String>,
    series: List<LineSeriesUi>,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    allowNegative: Boolean = false,
    startAtZero: Boolean = true,
    axisFormatter: (Double) -> String = { formatCompactAxis(it) },
    tooltipFormatter: (Double) -> String = { formatCompactAxis(it) }
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labelStyle = TextStyle(color = Muted, fontSize = 9.sp)
    var selectedIndex by remember(labels) { mutableStateOf<Int?>(null) }
    LaunchedEffect(labels, series) {
        selectedIndex = null
    }

    val allValues = series.flatMap { it.values }.filterNotNull().filter { it.isFinite() }
    var rawMin = allValues.minOrNull() ?: 0.0
    var rawMax = allValues.maxOrNull() ?: 0.0
    if (allowNegative) {
        rawMin = minOf(rawMin, 0.0)
        rawMax = maxOf(rawMax, 0.0)
    } else if (startAtZero) {
        rawMin = 0.0
    }
    var span = rawMax - rawMin
    if (span < 0.0001) {
        span = if (abs(rawMax) > 0.0001) abs(rawMax) * 0.2 else 1.0
    }
    val padding = span * 0.12
    val chartMin = when {
        allowNegative -> rawMin - padding
        startAtZero -> 0.0
        else -> max(0.0, rawMin - padding)
    }
    val chartMax = rawMax + padding

    BoxWithConstraints(modifier.fillMaxWidth().height(height)) {
        val leftDp = 48.dp
        val rightDp = 8.dp
        val widthPx = with(density) { maxWidth.toPx() }
        val leftPx = with(density) { leftDp.toPx() }
        val rightPx = with(density) { rightDp.toPx() }
        val plotWidthPx = (widthPx - leftPx - rightPx).coerceAtLeast(1f)
        val selectAt: (Float) -> Unit = { x ->
            if (labels.isNotEmpty()) {
                val ratio = ((x - leftPx) / plotWidthPx).coerceIn(0f, 1f)
                selectedIndex = (ratio * (labels.size - 1))
                    .roundToInt()
                    .coerceIn(0, labels.lastIndex)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(labels, series) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset -> selectAt(offset.x) },
                        onDrag = { change, _ ->
                            selectAt(change.position.x)
                            change.consume()
                        }
                    )
                }
        ) {
            val left = leftDp.toPx()
            val right = rightDp.toPx()
            val top = 8.dp.toPx()
            val bottom = 22.dp.toPx()
            val plotWidth = size.width - left - right
            val plotHeight = size.height - top - bottom
            if (plotWidth <= 1f || plotHeight <= 1f || labels.isEmpty()) return@Canvas

            fun xAt(index: Int): Float {
                if (labels.size <= 1) return left + plotWidth / 2f
                return left + plotWidth * index / (labels.size - 1).toFloat()
            }

            fun yAt(value: Double): Float {
                val ratio = ((value - chartMin) / (chartMax - chartMin)).toFloat()
                return top + plotHeight * (1f - ratio)
            }

            for (i in 0..3) {
                val y = top + plotHeight * i / 3f
                drawLine(
                    color = Line,
                    start = Offset(left, y),
                    end = Offset(left + plotWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (allowNegative && chartMin < 0.0 && chartMax > 0.0) {
                val zeroY = yAt(0.0)
                drawLine(
                    color = LineStrong,
                    start = Offset(left, zeroY),
                    end = Offset(left + plotWidth, zeroY),
                    strokeWidth = 1.2.dp.toPx()
                )
            }

            listOf(chartMax, (chartMax + chartMin) / 2.0, chartMin).forEach { value ->
                val layout = textMeasurer.measure(
                    text = AnnotatedString(axisFormatter(value)),
                    style = labelStyle
                )
                val y = (yAt(value) - layout.size.height / 2f)
                    .coerceIn(0f, size.height - layout.size.height)
                drawText(layout, topLeft = Offset(0f, y))
            }

            selectedIndex?.let { index ->
                if (index in labels.indices) {
                    drawLine(
                        color = LineStrong,
                        start = Offset(xAt(index), top),
                        end = Offset(xAt(index), top + plotHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            series.forEach { item ->
                val path = Path()
                var started = false
                item.values.forEachIndexed { index, value ->
                    if (value == null) {
                        started = false
                    } else {
                        val px = xAt(index)
                        val py = yAt(value)
                        if (!started) {
                            path.moveTo(px, py)
                            started = true
                        } else {
                            path.lineTo(px, py)
                        }
                    }
                }
                drawPath(
                    path = path,
                    color = item.color,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                if (labels.size <= 14) {
                    item.values.forEachIndexed { index, value ->
                        if (value != null) {
                            drawCircle(
                                color = item.color,
                                radius = 2.5.dp.toPx(),
                                center = Offset(xAt(index), yAt(value))
                            )
                        }
                    }
                }
                selectedIndex?.let { index ->
                    val value = item.values.getOrNull(index)
                    if (value != null && index in labels.indices) {
                        drawCircle(
                            color = item.color,
                            radius = 4.dp.toPx(),
                            center = Offset(xAt(index), yAt(value))
                        )
                    }
                }
            }

            val step = if (labels.size <= 6) 1 else ceil((labels.size - 1) / 4.0).toInt().coerceAtLeast(1)
            val indices = if (labels.size <= 6) {
                labels.indices.toList()
            } else {
                listOf(0, step, step * 2, step * 3, labels.size - 1)
                    .distinct()
                    .filter { it in labels.indices }
            }
            indices.forEach { index ->
                val layout = textMeasurer.measure(
                    text = AnnotatedString(labels[index]),
                    style = labelStyle
                )
                val center = xAt(index)
                val x = (center - layout.size.width / 2f)
                    .coerceIn(0f, size.width - layout.size.width)
                drawText(
                    layout,
                    topLeft = Offset(x, size.height - layout.size.height)
                )
            }
        }

        selectedIndex?.let { index ->
            if (index in labels.indices) {
                val tooltipWidth = 174.dp
                val ratio = if (labels.size <= 1) 0.5f else index / (labels.lastIndex).toFloat()
                val center = leftDp + (maxWidth - leftDp - rightDp) * ratio
                val x = (center - tooltipWidth / 2)
                    .coerceIn(0.dp, (maxWidth - tooltipWidth).coerceAtLeast(0.dp))
                ChartTooltip(
                    label = labels[index],
                    series = series,
                    index = index,
                    formatter = tooltipFormatter,
                    modifier = Modifier
                        .offset(x = x, y = 4.dp)
                        .zIndex(2f)
                        .width(tooltipWidth)
                )
            }
        }
    }
}

@Composable
private fun ChartTooltip(
    label: String,
    series: List<LineSeriesUi>,
    index: Int,
    formatter: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(CardWhite.copy(alpha = 0.98f))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = Muted,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(3.dp))
        series.forEach { item ->
            val value = item.values.getOrNull(index)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(item.color)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = item.name,
                    color = Muted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (value == null) "—" else formatter(value),
                    color = TextSoft,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
