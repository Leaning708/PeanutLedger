package com.peanut.ledger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peanut.ledger.data.DayStats
import com.peanut.ledger.data.LedgerEntry
import com.peanut.ledger.data.LedgerTab
import com.peanut.ledger.data.LedgerType
import com.peanut.ledger.data.dateLabel
import com.peanut.ledger.data.entryFinal
import com.peanut.ledger.data.entryTotal
import com.peanut.ledger.data.fmtHM
import com.peanut.ledger.data.fmtMoney
import com.peanut.ledger.data.fmtNum
import com.peanut.ledger.data.fmtSigned
import com.peanut.ledger.data.itemAmount
import com.peanut.ledger.data.round2
import com.peanut.ledger.data.typeSubtotal
import com.peanut.ledger.data.weekLabel
import com.peanut.ledger.ui.theme.Bg
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.DashedLine
import com.peanut.ledger.ui.theme.GoldBg
import com.peanut.ledger.ui.theme.Green
import com.peanut.ledger.ui.theme.GreenBg
import com.peanut.ledger.ui.theme.IconMuted
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.Placeholder
import com.peanut.ledger.ui.theme.Primary
import com.peanut.ledger.ui.theme.PrimaryDark
import com.peanut.ledger.ui.theme.Red
import com.peanut.ledger.ui.theme.RedBg
import com.peanut.ledger.ui.theme.SegBg
import com.peanut.ledger.ui.theme.SubBg
import com.peanut.ledger.ui.theme.TagBg
import com.peanut.ledger.ui.theme.TextMain
import com.peanut.ledger.ui.theme.TextSoft
import com.peanut.ledger.ui.theme.Amber
import com.peanut.ledger.ui.theme.Brown
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun MainScreen(state: LedgerState) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.currentDate, state.tab) {
        if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) {
            listState.scrollToItem(0)
        }
    }
    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize()) {
            AppHeader(state)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 116.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val day = state.currentDay
                if (day.remark.isNotBlank()) {
                    item(key = "day-remark") {
                        DayRemarkCard(day.remark) { state.openSheet(Sheet.DayRemark) }
                    }
                }
                val entries = state.currentEntries
                if (entries.isEmpty()) {
                    item(key = "empty") { EmptyState(state.tab) }
                } else {
                    items(entries, key = { it.id }) { entry ->
                        EntryCard(state, entry)
                    }
                }
            }
        }
        BottomBar(
            state = state,
            stats = state.statsOf(state.currentDate),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun AppHeader(state: LedgerState) {
    Column(Modifier.fillMaxWidth().background(Bg)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 5.dp,
                    shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(TopGradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundNavButton("‹", "前一天") { state.previousDay() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .clickable { state.openCalendar() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CalendarGlyph(Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = dateLabel(state.currentDate),
                            color = CardWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    val isToday = state.currentDate == LocalDate.now()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${weekLabel(state.currentDate)} · ",
                            color = CardWhite.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                        if (isToday) {
                            Text("今天", color = CardWhite.copy(alpha = 0.85f), fontSize = 12.sp)
                        } else {
                            Text(
                                text = "回到今天",
                                color = CardWhite.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { state.goToday() }
                            )
                        }
                    }
                }
                RoundNavButton("›", "后一天") { state.nextDay() }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(SegBg)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                TabSegment("采购", state.tab == LedgerTab.BUY, Modifier.weight(1f)) {
                    state.toggleTab(LedgerTab.BUY)
                }
                TabSegment("销售", state.tab == LedgerTab.SELL, Modifier.weight(1f)) {
                    state.toggleTab(LedgerTab.SELL)
                }
            }
            GearButton(
                text = if (state.privacyMode) "🙈" else "👁",
                description = if (state.privacyMode) "关闭隐私模式" else "开启隐私模式",
                active = state.privacyMode,
                onClick = { state.togglePrivacy() }
            )
            GearButton(
                text = "⋯",
                description = "更多",
                active = false,
                onClick = { state.openSheet(Sheet.More) }
            )
        }
    }
}

@Composable
private fun RoundNavButton(text: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(CardWhite.copy(alpha = 0.18f))
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = CardWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun TabSegment(text: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .then(if (active) Modifier.shadow(1.dp, RoundedCornerShape(9.dp)) else Modifier)
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) CardWhite else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (active) PrimaryDark else Muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GearButton(text: String, description: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .semantics { contentDescription = description }
            .shadow(1.dp, RoundedCornerShape(11.dp))
            .clip(RoundedCornerShape(11.dp))
            .background(if (active) GoldBg else CardWhite)
            .then(
                if (active) Modifier.border(2.dp, Primary.copy(alpha = 0.22f), RoundedCornerShape(11.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 19.sp, color = if (active) Primary else Muted)
    }
}

@Composable
private fun DayRemarkCard(remark: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = CardWhite,
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text("📝", fontSize = 14.sp)
            Spacer(Modifier.width(7.dp))
            Text(
                text = remark,
                color = Color(0xFF7A6A55),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun EntryCard(state: LedgerState, entry: LedgerEntry) {
    val tab = state.tab
    val isBuy = tab == LedgerTab.BUY
    val title = entry.title(tab)
    val revealed = state.isRevealed(entry.id)
    val masked = state.privacyMode && !revealed
    val total = entryTotal(entry)
    val final = entryFinal(entry)
    val adjust = round2(final - total)
    val finalLabel = if (isBuy) "实付" else "实收"

    fun money(value: Double): String = if (masked) "****" else fmtMoney(value)

    val gestureModifier = if (state.privacyMode) {
        Modifier.pointerInput(entry.id) {
            detectTapGestures(
                onDoubleTap = { state.toggleReveal(entry.id) },
                onLongPress = { state.toggleReveal(entry.id) }
            )
        }
    } else {
        Modifier
    }

    Surface(
        modifier = Modifier.fillMaxWidth().then(gestureModifier),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 2.dp,
        border = if (state.privacyMode && revealed) {
            androidx.compose.foundation.BorderStroke(2.dp, Primary.copy(alpha = 0.35f))
        } else {
            null
        }
    ) {
        Column(Modifier.padding(start = 13.dp, end = 13.dp, top = 12.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(title, isBuy)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = TextMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!isBuy) {
                        Spacer(Modifier.width(5.dp))
                        SettleTag(entry.settled)
                    }
                    if (state.privacyMode && revealed) {
                        Spacer(Modifier.width(4.dp))
                        RevealBadge()
                    }
                }
                if (entry.createdAt > 0) {
                    Text(
                        text = fmtHM(entry.createdAt),
                        color = IconMuted,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .semantics { contentDescription = "记录菜单" }
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { state.openSheet(Sheet.EntryMenu(entry.id)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⋮", color = IconMuted, fontSize = 20.sp)
                }
            }

            if (entry.remark.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(SubBg)
                        .clickable { state.openSheet(Sheet.EntryRemark(entry.id)) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = entry.remark,
                        color = Muted,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            if (entry.types.isEmpty()) {
                Text(
                    text = "还没有花生类型，先添加一个吧",
                    color = Placeholder,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entry.types.forEach { type ->
                        TypeGroup(
                            state = state,
                            entry = entry,
                            type = type,
                            masked = masked
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            DashedAddButton(
                text = "+ 添加花生类型",
                onClick = {
                    if (state.canEditSensitive(entry.id)) {
                        state.openSheet(Sheet.AddType(entry.id))
                    }
                }
            )

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Line)
            Spacer(Modifier.height(9.dp))

            FooterRow("合计", money(total))
            if (abs(adjust) >= 0.005) {
                val adjustLabel = when {
                    adjust > 0 && isBuy -> "多付"
                    adjust > 0 -> "多收"
                    isBuy -> "少付"
                    else -> "少收"
                }
                FooterRow(
                    label = adjustLabel,
                    value = if (masked) "****" else String.format(java.util.Locale.US, "%.2f", abs(adjust)),
                    valueColor = Primary
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (state.canEditSensitive(entry.id)) {
                            state.openSheet(Sheet.FinalAmount(entry.id))
                        }
                    }
                    .padding(horizontal = 2.dp, vertical = 5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = finalLabel,
                    color = Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "✎",
                    color = IconMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 4.dp, bottom = 2.dp)
                )
                Text(
                    text = money(final),
                    color = if (masked) Placeholder else Primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CalendarGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.4.dp.toPx()
        val color = CardWhite.copy(alpha = 0.92f)
        val top = h * 0.18f
        val bodyHeight = h - top - 0.5f
        drawRoundRect(
            color = color,
            topLeft = Offset(0.5f, top),
            size = Size(w - 1f, bodyHeight),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = color,
            start = Offset(0.5f, top + bodyHeight * 0.3f),
            end = Offset(w - 0.5f, top + bodyHeight * 0.3f),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(w * 0.3f, 0f),
            end = Offset(w * 0.3f, top + stroke),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.7f, 0f),
            end = Offset(w * 0.7f, top + stroke),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        val dotY = top + bodyHeight * 0.66f
        drawCircle(color, radius = 1.1.dp.toPx(), center = Offset(w * 0.34f, dotY))
        drawCircle(color, radius = 1.1.dp.toPx(), center = Offset(w * 0.62f, dotY))
    }
}

@Composable
private fun Avatar(title: String, isBuy: Boolean) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(
                if (isBuy) {
                    Brush.linearGradient(listOf(Color(0xFFF0C08A), Color(0xFFD08C46)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFF9FD3B4), Color(0xFF4C9A6E)))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.take(1).ifBlank { "?" },
            color = CardWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettleTag(settled: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (settled) GreenBg else RedBg)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            text = if (settled) "已结清" else "赊账",
            color = if (settled) Green else Red,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RevealBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(GoldBg)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text("👁 已显示", color = Primary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TypeGroup(
    state: LedgerState,
    entry: LedgerEntry,
    type: LedgerType,
    masked: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SubBg)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD08C46))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = type.name,
                color = Brown,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (masked) "****" else fmtMoney(typeSubtotal(type)),
                color = Color(0xFFB08954),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(2.dp))
        type.items.forEachIndexed { index, item ->
            val manual = item.manualAmount != null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.openItemForm(entry.id, type.id, item.id) }
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fmtNum(item.weight),
                        color = TextSoft,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    UnitText("斤")
                    Text(" × ", color = Placeholder, fontSize = 10.sp)
                    Text(
                        text = if (masked) "****" else fmtNum(item.price),
                        color = TextSoft,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    UnitText("元")
                    if (manual && !masked) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(TagBg)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("改", color = Amber, fontSize = 10.sp)
                        }
                    }
                }
                Text(
                    text = if (masked) "****" else fmtMoney(itemAmount(item)),
                    color = TextMain,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (index != type.items.lastIndex) {
                DashedDivider()
            }
        }
        DashedAddButton(
            text = "+ 新增称",
            modifier = Modifier.padding(top = 6.dp),
            onClick = { state.openItemForm(entry.id, type.id, null) }
        )
    }
}

@Composable
private fun UnitText(text: String) {
    Text(text = text, color = Color(0xFFA2988A), fontSize = 11.sp)
}

@Composable
private fun DashedDivider() {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = Color(0xFFEFE3D0),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
        )
    }
}

@Composable
private fun FooterRow(label: String, value: String, valueColor: Color = TextSoft) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyState(tab: LedgerTab) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (tab == LedgerTab.BUY) "🥜" else "🧾",
            fontSize = 46.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = if (tab == LedgerTab.BUY) "这一天还没有采购记录" else "这一天还没有销售记录",
            color = Color(0xFFB7AC9E),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (tab == LedgerTab.BUY) "点击下方「新增农户」开始记账" else "点击下方「新增收购」开始记账",
            color = Placeholder,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun BottomBar(state: LedgerState, stats: DayStats, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardWhite,
        shadowElevation = 10.dp
    ) {
        Column {
            HorizontalDivider(color = Line)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("今日收支", color = Muted, fontSize = 11.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (state.privacyMode) "****" else fmtSigned(stats.profit),
                            color = when {
                                state.privacyMode -> IconMuted
                                stats.profit >= 0 -> Primary
                                else -> Green
                            },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = if (state.privacyMode) {
                            "采购 **** · 销售 ****"
                        } else {
                            "采购 ${fmtMoney(stats.buy)} · 销售 ${fmtMoney(stats.sell)}"
                        },
                        color = Muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PrimaryButton(
                    text = if (state.tab == LedgerTab.BUY) "+ 新增农户" else "+ 新增收购",
                    onClick = { state.openSheet(Sheet.AddEntry) },
                    modifier = Modifier.shadow(4.dp, RoundedCornerShape(11.dp))
                )
            }
        }
    }
}
