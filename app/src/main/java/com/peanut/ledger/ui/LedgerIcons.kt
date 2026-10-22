package com.peanut.ledger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppIcon {
    ChevronLeft,
    ChevronRight,
    ChevronDown,
    ArrowLeft,
    More,
    Eye,
    EyeOff,
    Plus,
    Close,
    Calendar,
    Note,
    Settings,
    Edit,
    Check,
    Trash,
    Tag,
    Cash,
    DragHandle,
    Pin,
    Chart
}

@Composable
fun AppIconView(
    icon: AppIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.8.dp
) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        val dx = (size.width - size.minDimension) / 2f
        val dy = (size.height - size.minDimension) / 2f
        val sw = strokeWidth.toPx().coerceAtLeast(1.1.dp.toPx())
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

        fun pt(x: Float, y: Float) = Offset(dx + x * s, dy + y * s)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, pt(x1, y1), pt(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)
        }
        fun path(block: Path.() -> Unit): Path = Path().apply {
            block()
        }
        fun strokePath(block: Path.() -> Unit) {
            drawPath(path(block), tint, style = stroke)
        }
        fun fillCircle(x: Float, y: Float, radius: Float) {
            drawCircle(tint, radius * s, pt(x, y))
        }

        when (icon) {
            AppIcon.ChevronLeft -> strokePath {
                moveTo(dx + 15f * s, dy + 5f * s)
                lineTo(dx + 8f * s, dy + 12f * s)
                lineTo(dx + 15f * s, dy + 19f * s)
            }

            AppIcon.ChevronRight -> strokePath {
                moveTo(dx + 9f * s, dy + 5f * s)
                lineTo(dx + 16f * s, dy + 12f * s)
                lineTo(dx + 9f * s, dy + 19f * s)
            }

            AppIcon.ChevronDown -> strokePath {
                moveTo(dx + 6f * s, dy + 9f * s)
                lineTo(dx + 12f * s, dy + 15f * s)
                lineTo(dx + 18f * s, dy + 9f * s)
            }

            AppIcon.ArrowLeft -> {
                line(20f, 12f, 4.5f, 12f)
                strokePath {
                    moveTo(dx + 11f * s, dy + 5.5f * s)
                    lineTo(dx + 4.5f * s, dy + 12f * s)
                    lineTo(dx + 11f * s, dy + 18.5f * s)
                }
            }

            AppIcon.More -> {
                fillCircle(5f, 12f, 1.55f)
                fillCircle(12f, 12f, 1.55f)
                fillCircle(19f, 12f, 1.55f)
            }

            AppIcon.Eye -> {
                drawEye(dx, dy, s, tint, sw)
                drawCircle(tint, 2.7f * s, pt(12f, 12f), style = stroke)
            }

            AppIcon.EyeOff -> {
                drawEye(dx, dy, s, tint.copy(alpha = 0.65f), sw)
                drawCircle(tint.copy(alpha = 0.65f), 2.7f * s, pt(12f, 12f), style = stroke)
                line(4f, 4f, 20f, 20f)
            }

            AppIcon.Plus -> {
                line(12f, 5f, 12f, 19f)
                line(5f, 12f, 19f, 12f)
            }

            AppIcon.Close -> {
                line(6f, 6f, 18f, 18f)
                line(18f, 6f, 6f, 18f)
            }

            AppIcon.Calendar -> {
                drawRoundRect(
                    color = tint,
                    topLeft = pt(3.5f, 5.5f),
                    size = Size(17f * s, 15f * s),
                    cornerRadius = CornerRadius(3f * s, 3f * s),
                    style = stroke
                )
                line(3.5f, 10f, 20.5f, 10f)
                line(8f, 2.8f, 8f, 6.5f)
                line(16f, 2.8f, 16f, 6.5f)
                fillCircle(8f, 14f, 1.1f)
                fillCircle(12f, 14f, 1.1f)
                fillCircle(16f, 14f, 1.1f)
                fillCircle(8f, 17.5f, 1.1f)
                fillCircle(12f, 17.5f, 1.1f)
            }

            AppIcon.Note -> {
                drawRoundRect(
                    color = tint,
                    topLeft = pt(5f, 3.5f),
                    size = Size(14f * s, 17f * s),
                    cornerRadius = CornerRadius(2.8f * s, 2.8f * s),
                    style = stroke
                )
                line(8.5f, 8f, 15.5f, 8f)
                line(8.5f, 12f, 15.5f, 12f)
                line(8.5f, 16f, 13f, 16f)
            }

            AppIcon.Settings -> {
                line(4f, 7f, 20f, 7f)
                line(4f, 12f, 20f, 12f)
                line(4f, 17f, 20f, 17f)
                drawCircle(tint, 2.3f * s, pt(9f, 7f), style = stroke)
                drawCircle(tint, 2.3f * s, pt(15.5f, 12f), style = stroke)
                drawCircle(tint, 2.3f * s, pt(8f, 17f), style = stroke)
            }

            AppIcon.Edit -> {
                strokePath {
                    moveTo(dx + 4.5f * s, dy + 19.5f * s)
                    lineTo(dx + 5.9f * s, dy + 14.4f * s)
                    lineTo(dx + 15.3f * s, dy + 5f * s)
                    cubicTo(
                        dx + 16.1f * s, dy + 4.2f * s,
                        dx + 17.1f * s, dy + 4.2f * s,
                        dx + 17.9f * s, dy + 5f * s
                    )
                    lineTo(dx + 19f * s, dy + 6.1f * s)
                    cubicTo(
                        dx + 19.8f * s, dy + 6.9f * s,
                        dx + 19.8f * s, dy + 7.9f * s,
                        dx + 19f * s, dy + 8.7f * s
                    )
                    lineTo(dx + 9.6f * s, dy + 18.1f * s)
                    close()
                }
                line(5.9f, 14.4f, 9.6f, 18.1f)
            }

            AppIcon.Check -> strokePath {
                moveTo(dx + 5f * s, dy + 12.5f * s)
                lineTo(dx + 10f * s, dy + 17.5f * s)
                lineTo(dx + 19f * s, dy + 7f * s)
            }

            AppIcon.Trash -> {
                line(4f, 6.5f, 20f, 6.5f)
                line(9f, 6.5f, 9f, 4.2f)
                line(15f, 6.5f, 15f, 4.2f)
                line(9f, 4.2f, 15f, 4.2f)
                strokePath {
                    moveTo(dx + 6f * s, dy + 6.5f * s)
                    lineTo(dx + 7.1f * s, dy + 19f * s)
                    cubicTo(
                        dx + 7.2f * s, dy + 20.1f * s,
                        dx + 8.1f * s, dy + 21f * s,
                        dx + 9.2f * s, dy + 21f * s
                    )
                    lineTo(dx + 14.8f * s, dy + 21f * s)
                    cubicTo(
                        dx + 15.9f * s, dy + 21f * s,
                        dx + 16.8f * s, dy + 20.1f * s,
                        dx + 16.9f * s, dy + 19f * s
                    )
                    lineTo(dx + 18f * s, dy + 6.5f * s)
                }
                line(10f, 10f, 10.4f, 17.5f)
                line(14f, 10f, 13.6f, 17.5f)
            }

            AppIcon.Tag -> {
                strokePath {
                    moveTo(dx + 4f * s, dy + 6.3f * s)
                    lineTo(dx + 4f * s, dy + 11.4f * s)
                    lineTo(dx + 12.6f * s, dy + 20f * s)
                    lineTo(dx + 20f * s, dy + 12.6f * s)
                    lineTo(dx + 11.4f * s, dy + 4f * s)
                    lineTo(dx + 6.3f * s, dy + 4f * s)
                    cubicTo(
                        dx + 5.03f * s, dy + 4f * s,
                        dx + 4f * s, dy + 5.03f * s,
                        dx + 4f * s, dy + 6.3f * s
                    )
                    close()
                }
                fillCircle(7.7f, 7.7f, 1.15f)
            }

            AppIcon.DragHandle -> {
                line(5f, 8f, 19f, 8f)
                line(5f, 12f, 19f, 12f)
                line(5f, 16f, 19f, 16f)
            }

            AppIcon.Chart -> {
                line(4f, 20f, 20f, 20f)
                line(4f, 20f, 4f, 4f)
                strokePath {
                    moveTo(dx + 7f * s, dy + 16f * s)
                    lineTo(dx + 11f * s, dy + 11f * s)
                    lineTo(dx + 14f * s, dy + 13.5f * s)
                    lineTo(dx + 20f * s, dy + 6f * s)
                }
            }

            AppIcon.Pin -> {
                strokePath {
                    moveTo(dx + 12f * s, dy + 21f * s)
                    cubicTo(
                        dx + 8.3f * s, dy + 16.6f * s,
                        dx + 5f * s, dy + 13f * s,
                        dx + 5f * s, dy + 9.4f * s
                    )
                    cubicTo(
                        dx + 5f * s, dy + 5.5f * s,
                        dx + 8.1f * s, dy + 2.5f * s,
                        dx + 12f * s, dy + 2.5f * s
                    )
                    cubicTo(
                        dx + 15.9f * s, dy + 2.5f * s,
                        dx + 19f * s, dy + 5.5f * s,
                        dx + 19f * s, dy + 9.4f * s
                    )
                    cubicTo(
                        dx + 19f * s, dy + 13f * s,
                        dx + 15.7f * s, dy + 16.6f * s,
                        dx + 12f * s, dy + 21f * s
                    )
                    close()
                }
                drawCircle(tint, 2.3f * s, pt(12f, 9.5f), style = stroke)
            }

            AppIcon.Cash -> {
                drawRoundRect(
                    color = tint,
                    topLeft = pt(2.8f, 6f),
                    size = Size(18.4f * s, 12f * s),
                    cornerRadius = CornerRadius(3f * s, 3f * s),
                    style = stroke
                )
                drawCircle(tint, 3.1f * s, pt(12f, 12f), style = stroke)
                line(5.2f, 12f, 7.4f, 12f)
                line(16.6f, 12f, 18.8f, 12f)
            }
        }
    }
}

private fun DrawScope.drawEye(
    dx: Float,
    dy: Float,
    s: Float,
    tint: Color,
    sw: Float
) {
    val path = Path().apply {
        moveTo(dx + 2.4f * s, dy + 12f * s)
        cubicTo(
            dx + 5.8f * s, dy + 7.2f * s,
            dx + 9.1f * s, dy + 5.8f * s,
            dx + 12f * s, dy + 5.8f * s
        )
        cubicTo(
            dx + 14.9f * s, dy + 5.8f * s,
            dx + 18.2f * s, dy + 7.2f * s,
            dx + 21.6f * s, dy + 12f * s
        )
        cubicTo(
            dx + 18.2f * s, dy + 16.8f * s,
            dx + 14.9f * s, dy + 18.2f * s,
            dx + 12f * s, dy + 18.2f * s
        )
        cubicTo(
            dx + 9.1f * s, dy + 18.2f * s,
            dx + 5.8f * s, dy + 16.8f * s,
            dx + 2.4f * s, dy + 12f * s
        )
        close()
    }
    drawPath(
        path = path,
        color = tint,
        style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

@Composable
fun PeanutMark(
    tint: Color,
    modifier: Modifier = Modifier,
    detailTint: Color = tint.copy(alpha = 0.35f)
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val shell = Path().apply {
            moveTo(w * 0.5f, h * 0.06f)
            cubicTo(
                w * 0.22f, h * 0.06f,
                w * 0.15f, h * 0.27f,
                w * 0.22f, h * 0.42f
            )
            cubicTo(
                w * 0.27f, h * 0.52f,
                w * 0.16f, h * 0.58f,
                w * 0.17f, h * 0.74f
            )
            cubicTo(
                w * 0.18f, h * 0.91f,
                w * 0.34f, h * 0.97f,
                w * 0.5f, h * 0.97f
            )
            cubicTo(
                w * 0.66f, h * 0.97f,
                w * 0.82f, h * 0.91f,
                w * 0.83f, h * 0.74f
            )
            cubicTo(
                w * 0.84f, h * 0.58f,
                w * 0.73f, h * 0.52f,
                w * 0.78f, h * 0.42f
            )
            cubicTo(
                w * 0.85f, h * 0.27f,
                w * 0.78f, h * 0.06f,
                w * 0.5f, h * 0.06f
            )
            close()
        }
        drawPath(shell, tint)

        val lineWidth = (w * 0.035f).coerceAtLeast(1f)
        drawLine(
            color = detailTint,
            start = Offset(w * 0.5f, h * 0.2f),
            end = Offset(w * 0.5f, h * 0.84f),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )
        val leftTop = Path().apply {
            moveTo(w * 0.37f, h * 0.28f)
            cubicTo(
                w * 0.28f, h * 0.36f,
                w * 0.28f, h * 0.44f,
                w * 0.37f, h * 0.52f
            )
        }
        val rightTop = Path().apply {
            moveTo(w * 0.63f, h * 0.28f)
            cubicTo(
                w * 0.72f, h * 0.36f,
                w * 0.72f, h * 0.44f,
                w * 0.63f, h * 0.52f
            )
        }
        val leftBottom = Path().apply {
            moveTo(w * 0.37f, h * 0.55f)
            cubicTo(
                w * 0.28f, h * 0.63f,
                w * 0.28f, h * 0.71f,
                w * 0.37f, h * 0.79f
            )
        }
        val rightBottom = Path().apply {
            moveTo(w * 0.63f, h * 0.55f)
            cubicTo(
                w * 0.72f, h * 0.63f,
                w * 0.72f, h * 0.71f,
                w * 0.63f, h * 0.79f
            )
        }
        listOf(leftTop, rightTop, leftBottom, rightBottom).forEach { path ->
            drawPath(
                path = path,
                color = detailTint,
                style = Stroke(width = lineWidth, cap = StrokeCap.Round)
            )
        }
    }
}
