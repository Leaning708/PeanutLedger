package com.peanut.ledger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peanut.ledger.data.formatCompactAxis
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.LineStrong
import com.peanut.ledger.ui.theme.Muted
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

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
    axisFormatter: (Double) -> String = { formatCompactAxis(it) }
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Muted, fontSize = 9.sp)
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

    Canvas(modifier.fillMaxWidth().height(height)) {
        val left = 48.dp.toPx()
        val right = 8.dp.toPx()
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

        // Horizontal guide lines.
        for (i in 0..3) {
            val y = top + plotHeight * i / 3f
            drawLine(
                color = Line,
                start = Offset(left, y),
                end = Offset(left + plotWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Emphasise the zero line when the chart crosses it.
        if (allowNegative && chartMin < 0.0 && chartMax > 0.0) {
            val zeroY = yAt(0.0)
            drawLine(
                color = LineStrong,
                start = Offset(left, zeroY),
                end = Offset(left + plotWidth, zeroY),
                strokeWidth = 1.2.dp.toPx()
            )
        }

        // Y-axis labels.
        listOf(chartMax, (chartMax + chartMin) / 2.0, chartMin).forEach { value ->
            val layout = textMeasurer.measure(
                text = AnnotatedString(axisFormatter(value)),
                style = labelStyle
            )
            val y = (yAt(value) - layout.size.height / 2f)
                .coerceIn(0f, size.height - layout.size.height)
            drawText(layout, topLeft = Offset(0f, y))
        }

        // Series paths and points.
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
        }

        // X-axis labels: keep at most five to avoid crowding.
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
}
