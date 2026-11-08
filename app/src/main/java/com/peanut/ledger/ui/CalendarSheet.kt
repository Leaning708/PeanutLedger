package com.peanut.ledger.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peanut.ledger.data.DayStats
import com.peanut.ledger.data.fmtMoney
import com.peanut.ledger.data.fmtProfitMoney
import com.peanut.ledger.data.fmtProfitShort
import com.peanut.ledger.data.fmtSigned
import com.peanut.ledger.ui.theme.CardBorder
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.Green
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.Placeholder
import com.peanut.ledger.ui.theme.Primary
import com.peanut.ledger.ui.theme.PrimaryDark
import com.peanut.ledger.ui.theme.Red
import com.peanut.ledger.ui.theme.SurfaceSoft
import com.peanut.ledger.ui.theme.TextMain
import com.peanut.ledger.ui.theme.TodayBg
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

private val DayCellShape = RoundedCornerShape(12.dp)
private val MonthCellShape = RoundedCornerShape(14.dp)

private data class CalendarPage(val mode: CalendarMode, val anchor: LocalDate)

@Composable
fun CalendarSheet(state: LedgerState) {
    val page = CalendarPage(state.calendarMode, state.calendarAnchor)
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 56.dp.toPx() }
    val swipeModifier = Modifier.pointerInput(state.calendarMode, state.calendarAnchor) {
        var totalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalDrag = 0f },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                totalDrag += dragAmount
            },
            onDragEnd = {
                if (totalDrag <= -swipeThreshold) {
                    state.shiftCalendar(1)
                } else if (totalDrag >= swipeThreshold) {
                    state.shiftCalendar(-1)
                }
                totalDrag = 0f
            },
            onDragCancel = { totalDrag = 0f }
        )
    }

    Column {
        SheetTitle("选择日期")
        Box(Modifier.fillMaxWidth().then(swipeModifier)) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (initialState.mode != targetState.mode) {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                    } else {
                        val forward = targetState.anchor.isAfter(initialState.anchor)
                        val enter = slideInHorizontally(
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        ) { width -> if (forward) width else -width } + fadeIn(tween(160))
                        val exit = slideOutHorizontally(
                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                        ) { width -> if (forward) -width / 3 else width / 3 } + fadeOut(tween(160))
                        enter togetherWith exit
                    }
                },
                label = "calendarPage"
            ) { targetPage ->
                val stats: DayStats = when (targetPage.mode) {
                    CalendarMode.DAY -> state.monthSummary(
                        targetPage.anchor.year,
                        targetPage.anchor.monthValue
                    )
                    CalendarMode.MONTH -> state.yearSummary(targetPage.anchor.year)
                    CalendarMode.YEAR -> state.rangeSummary(targetPage.anchor.year - 5)
                }
                val summaryLabel = when (targetPage.mode) {
                    CalendarMode.DAY -> "本月"
                    CalendarMode.MONTH -> "${targetPage.anchor.year}年"
                    CalendarMode.YEAR -> "近12年"
                }
                Column {
                    CalendarSummary(state, stats, summaryLabel)
                    Spacer(Modifier.height(14.dp))
                    CalendarHeader(state, targetPage)
                    Spacer(Modifier.height(12.dp))
                    when (targetPage.mode) {
                        CalendarMode.DAY -> DayCalendar(state, targetPage.anchor)
                        CalendarMode.MONTH -> MonthCalendar(state, targetPage.anchor)
                        CalendarMode.YEAR -> YearCalendar(state, targetPage.anchor)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GhostButton("回到今天", { state.goToday() }, Modifier.weight(1f))
            PrimaryButton("关闭", { state.closeSheet() }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CalendarSummary(state: LedgerState, stats: DayStats, label: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceSoft,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SummaryCell(
                label = "${label}支出",
                value = if (state.privacyMode) "****" else fmtMoney(stats.purchase),
                color = if (state.privacyMode) Placeholder else TextMain,
                modifier = Modifier.weight(1f)
            )
            SummaryCell(
                label = "${label}收入",
                value = if (state.privacyMode) "****" else fmtMoney(stats.sale),
                color = if (state.privacyMode) Placeholder else TextMain,
                modifier = Modifier.weight(1f)
            )
            SummaryCell(
                label = "${label}收支",
                value = if (state.privacyMode) "****" else fmtSigned(stats.profit),
                color = when {
                    state.privacyMode -> Placeholder
                    stats.profit >= 0 -> Primary
                    else -> Green
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 10.5.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CalendarHeader(state: LedgerState, page: CalendarPage) {
    val startYear = page.anchor.year - 5
    val title = when (page.mode) {
        CalendarMode.DAY -> "${page.anchor.year}年${page.anchor.monthValue}月"
        CalendarMode.MONTH -> "${page.anchor.year}年"
        CalendarMode.YEAR -> "$startYear–${startYear + 11}年"
    }
    val tag = when (page.mode) {
        CalendarMode.DAY -> "日"
        CalendarMode.MONTH -> "月"
        CalendarMode.YEAR -> "年"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CalendarNavButton(AppIcon.ChevronLeft, "上一周期") { state.shiftCalendar(-1) }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    val next = when (page.mode) {
                        CalendarMode.DAY -> CalendarMode.MONTH
                        CalendarMode.MONTH -> CalendarMode.YEAR
                        CalendarMode.YEAR -> CalendarMode.DAY
                    }
                    state.changeCalendarMode(next)
                }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            ModeTag(tag)
        }
        CalendarNavButton(AppIcon.ChevronRight, "下一周期") { state.shiftCalendar(1) }
    }
}

@Composable
private fun CalendarNavButton(icon: AppIcon, description: String, onClick: () -> Unit) {
    CircleIconButton(
        icon = icon,
        description = description,
        onClick = onClick,
        tint = PrimaryDark,
        background = SurfaceSoft
    )
}

@Composable
private fun WeekHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
            Text(
                text = day,
                color = Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class DayCell(val date: LocalDate, val other: Boolean)

private fun buildDayCells(anchor: LocalDate): List<DayCell> {
    val yearMonth = YearMonth.from(anchor)
    val firstDay = yearMonth.atDay(1)
    val leading = firstDay.dayOfWeek.value % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val previousMonth = yearMonth.minusMonths(1)
    val cells = mutableListOf<DayCell>()
    for (index in leading downTo 1) {
        val day = previousMonth.lengthOfMonth() - index + 1
        cells += DayCell(previousMonth.atDay(day), true)
    }
    for (day in 1..daysInMonth) {
        cells += DayCell(yearMonth.atDay(day), false)
    }
    val nextMonth = yearMonth.plusMonths(1)
    var nextDay = 1
    while (cells.size % 7 != 0) {
        cells += DayCell(nextMonth.atDay(nextDay), true)
        nextDay++
    }
    return cells
}

@Composable
private fun DayCalendar(state: LedgerState, anchor: LocalDate) {
    val today = LocalDate.now()
    val cells = buildDayCells(anchor)
    Column {
        WeekHeader()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { cell ->
                        DayCellView(state, cell, today)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCellView(
    state: LedgerState,
    cell: DayCell,
    today: LocalDate
) {
    val selected = cell.date == state.currentDate
    val isToday = cell.date == today
    val stats = state.statsOf(cell.date)
    val shape = DayCellShape
    val base = Modifier
        .weight(1f)
        .heightIn(min = 50.dp)
        .clip(shape)
        .clickable { state.selectDate(cell.date) }
    val backgroundModifier = if (selected) {
        base.background(TopGradient)
    } else {
        base
            .background(if (isToday) TodayBg else Color.Transparent)
            .then(
                if (isToday) {
                    Modifier.border(1.dp, Primary.copy(alpha = 0.22f), shape)
                } else {
                    Modifier
                }
            )
    }
    Box(
        modifier = backgroundModifier.padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        if (state.dayHasUnsettled(cell.date)) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Red)
                    .border(1.dp, CardWhite, CircleShape)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                color = when {
                    selected -> CardWhite
                    cell.other -> Placeholder
                    isToday -> PrimaryDark
                    else -> TextMain
                },
                fontSize = 14.sp,
                fontWeight = if (cell.other && !selected) FontWeight.Medium else FontWeight.SemiBold
            )
            if (!state.privacyMode && abs(stats.profit) >= 0.005) {
                Text(
                    text = fmtProfitShort(stats.profit),
                    color = when {
                        selected -> CardWhite.copy(alpha = 0.92f)
                        cell.other -> profitColor(stats.profit).copy(alpha = 0.5f)
                        else -> profitColor(stats.profit)
                    },
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun MonthCalendar(state: LedgerState, anchor: LocalDate) {
    val currentDate = state.currentDate
    val today = LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (column in 0 until 3) {
                    val month = row * 3 + column + 1
                    val stats = state.monthSummary(anchor.year, month)
                    val isCurrent = anchor.year == currentDate.year && month == currentDate.monthValue
                    val isTodayMonth = anchor.year == today.year && month == today.monthValue
                    val profitVisible = !state.privacyMode && abs(stats.profit) >= 0.005
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 60.dp)
                            .clip(MonthCellShape)
                            .background(
                                when {
                                    isCurrent -> TodayBg
                                    isTodayMonth -> TodayBg
                                    else -> SurfaceSoft
                                }
                            )
                            .border(
                                if (isCurrent) 1.5.dp else 1.dp,
                                if (isCurrent) Primary else Line,
                                MonthCellShape
                            )
                            .clickable { state.pickCalendarMonth(anchor.year, month) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.monthHasUnsettled(anchor.year, month)) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 7.dp, end = 7.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Red)
                                    .border(1.dp, CardWhite, CircleShape)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${month}月",
                                color = if (isCurrent) PrimaryDark else TextMain,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = when {
                                    state.privacyMode -> "****"
                                    profitVisible -> fmtProfitMoney(stats.profit)
                                    else -> "—"
                                },
                                color = when {
                                    state.privacyMode -> Placeholder
                                    profitVisible -> profitColor(stats.profit)
                                    else -> Placeholder
                                },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearCalendar(state: LedgerState, anchor: LocalDate) {
    val startYear = anchor.year - 5
    val currentYear = state.currentDate.year
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (column in 0 until 3) {
                    val year = startYear + row * 3 + column
                    val stats = state.yearSummary(year)
                    val isCurrent = year == currentYear
                    val profitVisible = !state.privacyMode && abs(stats.profit) >= 0.005
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 60.dp)
                            .clip(MonthCellShape)
                            .background(if (isCurrent) TodayBg else SurfaceSoft)
                            .border(
                                if (isCurrent) 1.5.dp else 1.dp,
                                if (isCurrent) Primary else Line,
                                MonthCellShape
                            )
                            .clickable { state.pickCalendarYear(year) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.yearHasUnsettled(year)) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 7.dp, end = 7.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Red)
                                    .border(1.dp, CardWhite, CircleShape)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${year}年",
                                color = if (isCurrent) PrimaryDark else TextMain,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = when {
                                    state.privacyMode -> "****"
                                    profitVisible -> fmtProfitMoney(stats.profit)
                                    else -> "—"
                                },
                                color = when {
                                    state.privacyMode -> Placeholder
                                    profitVisible -> profitColor(stats.profit)
                                    else -> Placeholder
                                },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun profitColor(value: Double): Color = if (value >= 0) Primary else Green
