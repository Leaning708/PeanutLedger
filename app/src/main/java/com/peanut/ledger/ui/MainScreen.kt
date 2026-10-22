package com.peanut.ledger.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
import com.peanut.ledger.data.entryAutoTotal
import com.peanut.ledger.data.entryFinal
import com.peanut.ledger.data.entryHasAdjustedAmount
import com.peanut.ledger.data.entryTotal
import com.peanut.ledger.data.fmtHM
import com.peanut.ledger.data.fmtMoney
import com.peanut.ledger.data.fmtNum
import com.peanut.ledger.data.fmtSigned
import com.peanut.ledger.data.itemAmount
import com.peanut.ledger.data.itemAutoAmount
import com.peanut.ledger.data.itemIsAdjusted
import com.peanut.ledger.data.round2
import com.peanut.ledger.data.typeSubtotal
import com.peanut.ledger.data.weekLabel
import com.peanut.ledger.ui.theme.AvatarBuy
import com.peanut.ledger.ui.theme.AvatarSell
import com.peanut.ledger.ui.theme.Bg
import com.peanut.ledger.ui.theme.Brown
import com.peanut.ledger.ui.theme.CardBorder
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.GoldBg
import com.peanut.ledger.ui.theme.Green
import com.peanut.ledger.ui.theme.GreenBg
import com.peanut.ledger.ui.theme.IconMuted
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.MutedLight
import com.peanut.ledger.ui.theme.Placeholder
import com.peanut.ledger.ui.theme.Primary
import com.peanut.ledger.ui.theme.PrimaryDark
import com.peanut.ledger.ui.theme.Red
import com.peanut.ledger.ui.theme.RedBg
import com.peanut.ledger.ui.theme.SurfaceSoft
import com.peanut.ledger.ui.theme.TagBg
import com.peanut.ledger.ui.theme.TextMain
import com.peanut.ledger.ui.theme.TextSoft
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

private data class MainPage(val date: LocalDate, val tab: LedgerTab)

private fun pageOrdinal(page: MainPage): Long =
    page.date.toEpochDay() * 2L + if (page.tab == LedgerTab.SALE) 1L else 0L

@Composable
fun MainScreen(state: LedgerState) {
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 72.dp.toPx() }
    var dockHeightPx by remember { mutableIntStateOf(0) }
    val contentBottomPadding = if (dockHeightPx > 0) {
        with(density) { dockHeightPx.toDp() }.plus(24.dp).coerceAtLeast(132.dp)
    } else {
        132.dp
    }
    val swipeModifier = Modifier.pointerInput(state.currentDate, state.tab) {
        var totalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = { totalDrag = 0f },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                totalDrag += dragAmount
            },
            onDragEnd = {
                if (totalDrag <= -swipeThreshold) {
                    state.swipeForwardPage()
                } else if (totalDrag >= swipeThreshold) {
                    state.swipeBackwardPage()
                }
                totalDrag = 0f
            },
            onDragCancel = { totalDrag = 0f }
        )
    }
    val page = MainPage(state.currentDate, state.tab)
    Box(Modifier.fillMaxSize().background(Bg).then(swipeModifier)) {
        Column(Modifier.fillMaxSize()) {
            AppHeader(state)
            AnimatedContent(
                targetState = page,
                modifier = Modifier.fillMaxWidth().weight(1f),
                transitionSpec = {
                    val forward = pageOrdinal(targetState) > pageOrdinal(initialState)
                    val enter = slideInHorizontally(
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) { width -> if (forward) width else -width } + fadeIn(tween(170))
                    val exit = slideOutHorizontally(
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) { width -> if (forward) -width / 3 else width / 3 } + fadeOut(tween(170))
                    enter togetherWith exit
                },
                label = "mainPage"
            ) { targetPage ->
                val listState = rememberLazyListState()
                LaunchedEffect(targetPage) {
                    listState.scrollToItem(0)
                }
                val day = state.day(targetPage.date)
                val entries = day.entries(targetPage.tab)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 14.dp,
                        bottom = contentBottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (day.remark.isNotBlank()) {
                        item(key = "day-remark") {
                            DayRemarkCard(day.remark) { state.openSheet(Sheet.DayRemark) }
                        }
                    }
                    if (entries.isEmpty()) {
                        item(key = "empty") { EmptyState(targetPage.tab) }
                    } else {
                        itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                            EntryCard(
                                state = state,
                                entry = entry,
                                tab = targetPage.tab,
                                privacyIndex = index + 1
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Bg.copy(alpha = 0.9f), Bg)
                    )
                )
        )
        BottomDock(
            state = state,
            stats = state.statsOf(state.currentDate),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { dockHeightPx = it.size.height }
        )
    }
}

@Composable
private fun AppHeader(state: LedgerState) {
    val today = LocalDate.now()
    val isToday = state.currentDate == today
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(TopGradient)
    ) {
        HeaderDecoration(Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 3.dp, bottom = 15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(
                    icon = AppIcon.ChevronLeft,
                    description = "前一天",
                    onClick = { state.previousDay() }
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { state.openCalendar() }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "花生账本",
                        color = CardWhite.copy(alpha = 0.58f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${state.currentDate.monthValue}月${state.currentDate.dayOfMonth}日",
                        color = CardWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${state.currentDate.year}年 · ${weekLabel(state.currentDate)} · ",
                            color = CardWhite.copy(alpha = 0.72f),
                            fontSize = 11.5.sp
                        )
                        if (isToday) {
                            Text(
                                text = "今天",
                                color = CardWhite.copy(alpha = 0.8f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "回到今天",
                                color = CardWhite.copy(alpha = 0.92f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { state.goToday() }
                            )
                        }
                    }
                }
                CircleIconButton(
                    icon = AppIcon.ChevronRight,
                    description = "后一天",
                    onClick = { state.nextDay() }
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(CardWhite.copy(alpha = 0.13f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SegmentButton("采购", state.tab == LedgerTab.PURCHASE) {
                        state.toggleTab(LedgerTab.PURCHASE)
                    }
                    SegmentButton("销售", state.tab == LedgerTab.SALE) {
                        state.toggleTab(LedgerTab.SALE)
                    }
                }
                HeaderIconButton(
                    icon = if (state.privacyMode) AppIcon.EyeOff else AppIcon.Eye,
                    description = if (state.privacyMode) "关闭隐私模式" else "开启隐私模式",
                    active = state.privacyMode,
                    onClick = { state.togglePrivacy() }
                )
                HeaderIconButton(
                    icon = AppIcon.More,
                    description = "更多",
                    onClick = { state.openSheet(Sheet.More) }
                )
            }
        }
    }
}

@Composable
private fun HeaderDecoration(modifier: Modifier) {
    Canvas(modifier) {
        drawCircle(
            color = CardWhite.copy(alpha = 0.05f),
            radius = size.width * 0.34f,
            center = Offset(size.width * 0.93f, size.height * 0.02f)
        )
        drawCircle(
            color = CardWhite.copy(alpha = 0.035f),
            radius = size.width * 0.22f,
            center = Offset(size.width * 0.02f, size.height * 0.92f)
        )
    }
}

@Composable
private fun RowScope.SegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) CardWhite else Color.Transparent,
        animationSpec = tween(180),
        label = "segmentBackground"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) PrimaryDark else CardWhite.copy(alpha = 0.82f),
        animationSpec = tween(180),
        label = "segmentText"
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DayRemarkCard(remark: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = AppIcon.Note,
                tint = Primary,
                background = GoldBg,
                size = 32.dp,
                iconSize = 15.dp
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = remark,
                color = TextSoft,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            AppIconView(AppIcon.ChevronRight, tint = IconMuted, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun EntryCard(
    state: LedgerState,
    entry: LedgerEntry,
    tab: LedgerTab,
    privacyIndex: Int
) {
    val isPurchase = tab == LedgerTab.PURCHASE
    val title = state.displayTitle(entry, tab, if (tab == LedgerTab.SALE) privacyIndex else null)
    val revealed = state.isRevealed(entry.id)
    val masked = state.privacyMode && !revealed
    val total = entryTotal(entry)
    val autoTotal = entryAutoTotal(entry)
    val hasAdjustedAmount = entryHasAdjustedAmount(entry)
    val final = entryFinal(entry)
    val adjust = round2(final - total)
    val finalLabel = if (isPurchase) "实付" else "实收"
    val itemCount = entry.types.sumOf { it.items.size }

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
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        shadowElevation = 2.dp,
        border = BorderStroke(
            width = if (state.privacyMode && revealed) 1.5.dp else 1.dp,
            color = if (state.privacyMode && revealed) Primary.copy(alpha = 0.45f) else CardBorder
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val supplierNumber = if (isPurchase) {
                    entry.supplierName.trim().removePrefix("供货商").removePrefix("户").toIntOrNull()
                } else {
                    null
                }
                val avatarLabel = when {
                    !isPurchase && state.privacyMode -> privacyIndex.toString()
                    supplierNumber != null -> supplierNumber.toString()
                    title.startsWith("户") && title.length > 1 -> title.removePrefix("户").take(2)
                    else -> title.take(1).ifBlank { "?" }
                }
                val metaText = buildString {
                    val time = fmtHM(entry.createdAt)
                    if (time.isNotBlank()) append(time)
                    if (itemCount > 0) {
                        if (isNotEmpty()) append("  ·  ")
                        append(itemCount).append(" 笔称重")
                    } else {
                        if (isNotEmpty()) append("  ·  ")
                        append("暂无称重")
                    }
                }
                EntryAvatar(avatarLabel, isPurchase)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = TextMain,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!isPurchase) {
                            Spacer(Modifier.width(6.dp))
                            SettleTag(entry.settled)
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = metaText,
                            color = Muted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                        if (state.privacyMode && revealed) {
                            Spacer(Modifier.width(6.dp))
                            RevealBadge()
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .semantics { contentDescription = "记录菜单" }
                        .clickable { state.openSheet(Sheet.EntryMenu(entry.id)) },
                    contentAlignment = Alignment.Center
                ) {
                    AppIconView(AppIcon.More, tint = IconMuted, modifier = Modifier.size(18.dp))
                }
            }

            if (!state.privacyMode) entry.location?.let { location ->
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceSoft)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconView(
                        icon = AppIcon.Pin,
                        tint = Primary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = location.displayText(),
                        color = Muted,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (entry.remark.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceSoft)
                        .clickable { state.openSheet(Sheet.EntryRemark(entry.id)) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIconView(
                            icon = AppIcon.Note,
                            tint = IconMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = entry.remark,
                        color = Muted,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            val accent = if (isPurchase) Primary else AvatarSell
            if (entry.types.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceSoft)
                        .clickable { state.openAddType(entry.id) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconView(AppIcon.Plus, tint = Primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "还没有${if (isPurchase) "采购类型" else "销售类型"}，添加一个开始记账",
                        color = Brown,
                        fontSize = 12.5.sp
                    )
                }
            } else {
                entry.types.forEachIndexed { index, type ->
                    if (index > 0) Spacer(Modifier.height(14.dp))
                    TypeSection(state, entry, type, masked, accent)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { state.openAddType(entry.id) }
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconView(AppIcon.Plus, tint = Primary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "添加${if (isPurchase) "采购类型" else "销售类型"}",
                        color = Primary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            ChevronDivider()
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 2.dp)) {
                    TinyLabel("合计")
                    Spacer(Modifier.height(3.dp))
                    if (hasAdjustedAmount && !masked) {
                        Text(
                            text = fmtMoney(autoTotal),
                            color = MutedLight,
                            fontSize = 11.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Spacer(Modifier.height(1.dp))
                    }
                    Text(
                        text = money(total),
                        color = TextSoft,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (state.canEditSensitive(entry.id)) {
                                state.openSheet(Sheet.FinalAmount(entry.id))
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TinyLabel(finalLabel)
                        Spacer(Modifier.width(3.dp))
                        AppIconView(AppIcon.Edit, tint = IconMuted, modifier = Modifier.size(11.dp))
                    }
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = money(final),
                        color = if (masked) Placeholder else Primary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (abs(adjust) >= 0.005 && !masked) {
                        Spacer(Modifier.height(4.dp))
                        val adjustLabel = when {
                            adjust > 0 && isPurchase -> "多付"
                            adjust > 0 -> "多收"
                            isPurchase -> "少付"
                            else -> "少收"
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = adjustLabel,
                                color = PrimaryDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = if (masked) "****" else String.format(
                                    Locale.US,
                                    "%.2f",
                                    abs(adjust)
                                ),
                                color = Primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryAvatar(label: String, isPurchase: Boolean) {
    val base = if (isPurchase) AvatarBuy else AvatarSell
    val start = lerp(base, Color.White, 0.28f)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(Brush.linearGradient(listOf(start, base))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.ifBlank { "?" },
            color = CardWhite,
            fontSize = when {
                label.length > 2 -> 12.5.sp
                label.length > 1 -> 15.sp
                else -> 17.sp
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun SettleTag(settled: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (settled) GreenBg else RedBg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
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
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(GoldBg)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconView(AppIcon.Eye, tint = Primary, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(3.dp))
        Text("已显示", color = Primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TypeSection(
    state: LedgerState,
    entry: LedgerEntry,
    type: LedgerType,
    masked: Boolean,
    accent: Color
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 3.dp, height = 14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = type.name,
                color = TextSoft,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (type.items.size > 3) {
                Spacer(Modifier.width(6.dp))
                CounterTag("${type.items.size} 称")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (masked) "****" else fmtMoney(typeSubtotal(type)),
                color = Brown,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        type.items.forEachIndexed { index, item ->
            if (index > 0) ChevronDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.openItemForm(entry.id, type.id, item.id) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (masked) "****" else fmtNum(item.price),
                        color = TextSoft,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    UnitText(" 元")
                    Text("  ×  ", color = Placeholder, fontSize = 10.sp)
                    Text(
                        text = fmtNum(item.weight),
                        color = TextSoft,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    UnitText(" 斤")
                }
                if (masked) {
                    Text(
                        text = "****",
                        color = TextMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (itemIsAdjusted(item)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = fmtMoney(itemAutoAmount(item)),
                            color = Muted,
                            fontSize = 11.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Text(
                            text = fmtMoney(itemAmount(item)),
                            color = TextMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = fmtMoney(itemAmount(item)),
                        color = TextMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { state.openItemForm(entry.id, type.id, null) }
                .padding(vertical = 9.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconView(AppIcon.Plus, tint = Primary, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("新增称重", color = Primary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun UnitText(text: String) {
    Text(text = text, color = MutedLight, fontSize = 10.5.sp)
}

@Composable
private fun EmptyState(tab: LedgerTab) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(if (tab == LedgerTab.PURCHASE) GoldBg else GreenBg),
            contentAlignment = Alignment.Center
        ) {
            if (tab == LedgerTab.PURCHASE) {
                PeanutMark(
                    tint = Primary.copy(alpha = 0.82f),
                    detailTint = GoldBg.copy(alpha = 0.92f),
                    modifier = Modifier.size(48.dp)
                )
            } else {
                AppIconView(
                    icon = AppIcon.Note,
                    tint = Green.copy(alpha = 0.75f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (tab == LedgerTab.PURCHASE) "这一天还没有采购记录" else "这一天还没有销售记录",
            color = TextSoft,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (tab == LedgerTab.PURCHASE) "点击下方「新增采购」记下第一笔" else "点击下方「新增销售」记下第一笔",
            color = Muted,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BottomDock(state: LedgerState, stats: DayStats, modifier: Modifier = Modifier) {
    val day = state.day(state.currentDate)
    val purchaseWeight = round2(
        day.purchases.sumOf { entry ->
            entry.types.sumOf { type -> type.items.sumOf { it.weight } }
        }
    )
    val saleWeight = round2(
        day.sales.sumOf { entry ->
            entry.types.sumOf { type -> type.items.sumOf { it.weight } }
        }
    )
    val purchaseAvg = if (purchaseWeight > 0.0001) round2(stats.purchase / purchaseWeight) else null
    val saleAvg = if (saleWeight > 0.0001) round2(stats.sale / saleWeight) else null
    val creditAmount = round2(
        day.sales.filter { !it.settled }.sumOf { entryFinal(it) }
    )
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(state.currentDate, state.tab) { expanded = false }
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 36.dp.toPx() }
    val toggleModifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { expanded = !expanded },
                onDoubleTap = { expanded = !expanded }
            )
        }
        .pointerInput(Unit) {
            var totalDrag = 0f
            detectVerticalDragGestures(
                onDragStart = { totalDrag = 0f },
                onVerticalDrag = { change, amount ->
                    change.consume()
                    totalDrag += amount
                },
                onDragEnd = {
                    if (totalDrag <= -swipeThreshold) {
                        expanded = true
                    } else if (totalDrag >= swipeThreshold) {
                        expanded = false
                    }
                    totalDrag = 0f
                },
                onDragCancel = { totalDrag = 0f }
            )
        }

    Box(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
            .animateContentSize()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(22.dp),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.16f)
            )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = CardWhite,
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 0.dp
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(toggleModifier)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (state.currentDate == LocalDate.now()) "今日收支" else "当日收支",
                            color = Muted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        AppIconView(
                            icon = AppIcon.ChevronDown,
                            tint = Muted,
                            modifier = Modifier
                                .size(11.dp)
                                .rotate(if (expanded) 180f else 0f)
                        )
                    }
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = if (state.privacyMode) "****" else fmtSigned(stats.profit),
                        color = when {
                            state.privacyMode -> Placeholder
                            stats.profit >= 0 -> Primary
                            else -> Green
                        },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (!expanded) {
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = if (state.privacyMode) {
                                "采购 ****  ·  销售 ****"
                            } else {
                                "采购 ${fmtMoney(stats.purchase)}  ·  销售 ${fmtMoney(stats.sale)}"
                            },
                            color = Muted,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .shadow(5.dp, RoundedCornerShape(15.dp))
                        .clip(RoundedCornerShape(15.dp))
                        .background(TopGradient)
                        .clickable { state.openSheet(Sheet.AddEntry) }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIconView(AppIcon.Plus, tint = CardWhite, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (state.tab == LedgerTab.PURCHASE) "新增采购" else "新增销售",
                            color = CardWhite,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(160)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(120))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .then(toggleModifier)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(38.dp))
                        Text(
                            "采购",
                            color = Primary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "销售",
                            color = AvatarSell,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    BottomMetricRow(
                        label = "金额",
                        left = if (state.privacyMode) "**** 元" else fmtYuan(stats.purchase),
                        right = if (state.privacyMode) "**** 元" else fmtYuan(stats.sale)
                    )
                    BottomMetricRow(
                        label = "斤数",
                        left = if (state.privacyMode) "**** 斤" else "${fmtNum(purchaseWeight)} 斤",
                        right = if (state.privacyMode) "**** 斤" else "${fmtNum(saleWeight)} 斤"
                    )
                    BottomMetricRow(
                        label = "均价",
                        left = when {
                            state.privacyMode -> "**** 元/斤"
                            purchaseAvg == null -> "—"
                            else -> fmtAvgYuan(purchaseAvg)
                        },
                        right = when {
                            state.privacyMode -> "**** 元/斤"
                            saleAvg == null -> "—"
                            else -> fmtAvgYuan(saleAvg)
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    ChevronDivider()
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("赊账金额", color = Muted, fontSize = 10.5.sp)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = if (state.privacyMode) "**** 元" else fmtYuan(creditAmount),
                            color = if (creditAmount > 0.005 && !state.privacyMode) Red else TextSoft,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        }
    }
}

private fun fmtYuan(value: Double): String =
    fmtMoney(value).removePrefix("¥") + " 元"

private fun fmtAvgYuan(value: Double): String =
    String.format(Locale.US, "%.2f 元/斤", value)

@Composable
private fun BottomMetricRow(label: String, left: String, right: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Muted,
            fontSize = 9.5.sp,
            modifier = Modifier.width(38.dp)
        )
        Text(
            text = left,
            color = TextMain,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = right,
            color = TextMain,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
