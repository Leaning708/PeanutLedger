package com.peanut.ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peanut.ledger.data.NamedOption
import com.peanut.ledger.ui.theme.AppTheme
import com.peanut.ledger.ui.theme.Bg
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.GoldBg
import com.peanut.ledger.ui.theme.Green
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.LineStrong
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.Placeholder
import com.peanut.ledger.ui.theme.Primary
import com.peanut.ledger.ui.theme.Red
import com.peanut.ledger.ui.theme.RedBg
import com.peanut.ledger.ui.theme.SurfaceSoft
import com.peanut.ledger.ui.theme.SurfaceWarm
import com.peanut.ledger.ui.theme.themeHeaderColors
import com.peanut.ledger.ui.theme.themePalette
import com.peanut.ledger.ui.theme.TextMain
import com.peanut.ledger.ui.theme.TextSoft
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(state: LedgerState) {
    BackHandler { state.closeSettings() }
    Column(Modifier.fillMaxSize().background(Bg)) {
        SettingsHeader(state)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SettingsBlock(state, SettingKind.BUY_TYPES) }
            item { SettingsBlock(state, SettingKind.SELL_TYPES) }
            item { SettingsBlock(state, SettingKind.BUYERS) }
            item { DisplayBlock(state) }
            item { SettlementBlock(state) }
            item { LocationBlock(state) }
            item { BackupBlock(state) }
            item { DataBlock(state) }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PeanutMark(
                        tint = Primary.copy(alpha = 0.3f),
                        detailTint = Primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = "数据仅保存在本机 · 花生账本 v0.1.9",
                        color = Placeholder,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(state: LedgerState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp),
                clip = false
            )
            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
            .background(TopGradient)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(
                icon = AppIcon.ArrowLeft,
                description = "返回",
                onClick = { state.closeSettings() },
                tint = CardWhite,
                background = CardWhite.copy(alpha = 0.14f)
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("设置", color = CardWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "类型 · 显示 · 位置 · 数据",
                    color = CardWhite.copy(alpha = 0.7f),
                    fontSize = 11.5.sp
                )
            }
            Spacer(Modifier.width(40.dp))
        }
    }
}

@Composable
private fun SettingsBlock(state: LedgerState, kind: SettingKind) {
    val list = state.settingsList(kind)
    var input by remember(kind) { mutableStateOf("") }
    var draggedName by remember(kind) { mutableStateOf<String?>(null) }
    var dragOffsetY by remember(kind) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val rowStepPx = with(density) { 55.dp.toPx() }
    val description = when (kind) {
        SettingKind.BUY_TYPES -> "长按右侧拖动柄排序；点「···」可重命名、失效或删除"
        SettingKind.SELL_TYPES -> "长按右侧拖动柄排序；点「···」可重命名、失效或删除"
        SettingKind.BUYERS -> "长按右侧拖动柄排序；点「···」可重命名、失效或删除"
    }
    LedgerCard(contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = kind.label,
                color = TextSoft,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            CounterTag(list.size.toString())
        }
        Spacer(Modifier.height(3.dp))
        Text(description, color = Muted, fontSize = 11.5.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceSoft)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text("暂无，可在下方添加", color = Placeholder, fontSize = 12.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                list.forEachIndexed { index, option ->
                    val isDragging = draggedName == option.name
                    val dragModifier = Modifier.pointerInput(option.name, list.size) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedName = option.name
                                dragOffsetY = 0f
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffsetY += amount.y
                            },
                            onDragEnd = {
                                val from = list.indexOfFirst { it.name == option.name }
                                val steps = (dragOffsetY / rowStepPx).roundToInt()
                                state.moveSettingTo(kind, option.name, from + steps)
                                draggedName = null
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                draggedName = null
                                dragOffsetY = 0f
                            }
                        )
                    }
                    SettingOptionRow(
                        option = option,
                        isDragging = isDragging,
                        dragOffsetY = if (isDragging) dragOffsetY else 0f,
                        onActions = { state.openSheet(Sheet.SettingActions(kind, option.name)) },
                        dragModifier = dragModifier
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedgerTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = kind.placeholder,
                minHeight = 46.dp
            )
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(3.dp, RoundedCornerShape(13.dp))
                    .clip(RoundedCornerShape(13.dp))
                    .background(TopGradient)
                    .semantics { contentDescription = "添加${kind.label}" }
                    .clickable {
                        state.addSetting(kind, input)
                        input = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                AppIconView(AppIcon.Plus, tint = CardWhite, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SettingOptionRow(
    option: NamedOption,
    isDragging: Boolean,
    dragOffsetY: Float,
    onActions: () -> Unit,
    dragModifier: Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .zIndex(if (isDragging) 2f else 0f)
            .graphicsLayer { translationY = dragOffsetY }
            .then(if (isDragging) Modifier.shadow(10.dp, shape) else Modifier)
            .clip(shape)
            .background(
                if (option.active) {
                    if (isDragging) SurfaceWarm else SurfaceSoft
                } else {
                    SurfaceSoft.copy(alpha = 0.5f)
                }
            )
            .border(1.dp, if (isDragging) Primary.copy(alpha = 0.35f) else Line, shape)
            .clickable(onClick = onActions)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (option.active) Green else Placeholder.copy(alpha = 0.5f))
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = option.name,
            color = if (option.active) TextMain else Placeholder,
            fontSize = 13.5.sp,
            fontWeight = if (option.active) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!option.active) {
            Spacer(Modifier.width(6.dp))
            InactiveTag()
        }
        Spacer(Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .semantics { contentDescription = "长按拖动排序" }
                .then(dragModifier),
            contentAlignment = Alignment.Center
        ) {
            AppIconView(
                icon = AppIcon.DragHandle,
                tint = if (option.active) Muted else Placeholder.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .semantics { contentDescription = "更多操作" }
                .clickable(onClick = onActions),
            contentAlignment = Alignment.Center
        ) {
            AppIconView(AppIcon.More, tint = Muted, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun InactiveTag() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFF1EDE6))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text("已失效", color = Muted, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DisplayBlock(state: LedgerState) {
    LedgerCard(contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("显示与主题", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            CounterTag("本机")
        }
        Spacer(Modifier.height(3.dp))
        Text("调整字体大小和整体配色，修改后立即生效", color = Muted, fontSize = 11.5.sp)
        Spacer(Modifier.height(14.dp))
        Text("字体大小", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fontScaleOptions.forEach { option ->
                val selected = abs(state.fontScale - option.scale) < 0.01f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) Primary else SurfaceSoft)
                        .border(
                            1.dp,
                            if (selected) Primary else Line,
                            RoundedCornerShape(11.dp)
                        )
                        .clickable { state.setFontScale(option.scale) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        color = if (selected) CardWhite else TextSoft,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("主题配色", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTheme.entries.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { theme ->
                        ThemeTile(
                            state = state,
                            theme = theme,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeTile(
    state: LedgerState,
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    val selected = state.themeId == theme.id
    val palette = themePalette(theme)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) Primary else Line,
                RoundedCornerShape(13.dp)
            )
            .clickable { state.setTheme(theme.id) }
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Brush.linearGradient(themeHeaderColors(theme))),
            contentAlignment = Alignment.Center
        ) {
            PeanutMark(
                tint = CardWhite.copy(alpha = 0.35f),
                detailTint = CardWhite.copy(alpha = 0.12f),
                modifier = Modifier.size(20.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(palette.bgOuter)
            )
        }
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = theme.label,
                color = if (selected) Primary else Muted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (selected) {
                Spacer(Modifier.width(2.dp))
                AppIconView(
                    icon = AppIcon.Check,
                    tint = Primary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

private data class FontScaleOption(val label: String, val scale: Float)

private val fontScaleOptions = listOf(
    FontScaleOption("标准", 1.0f),
    FontScaleOption("大", 1.15f),
    FontScaleOption("特大", 1.3f)
)

@Composable
private fun SettlementBlock(state: LedgerState) {
    LedgerCard(contentPadding = PaddingValues(18.dp)) {
        Text("记账偏好", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text("新增销售记录时默认使用的结算状态", color = Muted, fontSize = 11.5.sp)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettlementOption(
                label = "默认赊账",
                selected = !state.defaultSettled,
                modifier = Modifier.weight(1f)
            ) { state.setDefaultSettled(false) }
            SettlementOption(
                label = "默认结清",
                selected = state.defaultSettled,
                modifier = Modifier.weight(1f)
            ) { state.setDefaultSettled(true) }
        }
    }
}

@Composable
private fun SettlementOption(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) Primary else SurfaceSoft)
            .border(1.dp, if (selected) Primary else Line, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) CardWhite else TextSoft,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LocationBlock(state: LedgerState) {
    LedgerCard(contentPadding = PaddingValues(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = AppIcon.Pin,
                tint = Primary,
                background = GoldBg,
                size = 34.dp,
                iconSize = 16.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("位置记录", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "新增采购时记录供货商当前位置并标记在条目上；隐私模式下不显示",
                    color = Muted,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = state.recordLocation,
                onCheckedChange = { state.setRecordLocation(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CardWhite,
                    checkedTrackColor = Primary,
                    checkedBorderColor = Primary,
                    uncheckedThumbColor = CardWhite,
                    uncheckedTrackColor = SurfaceSoft,
                    uncheckedBorderColor = LineStrong
                )
            )
        }
    }
}

@Composable
private fun BackupBlock(state: LedgerState) {
    val backups = remember(state.backupRevision) { state.listBackups() }
    val last = backups.firstOrNull()
    val frequency = when (state.backupFrequencyDays) {
        0 -> "自动备份已关闭"
        1 -> "每天自动备份"
        7 -> "每周自动备份"
        else -> "每 ${state.backupFrequencyDays} 天自动备份"
    }
    LedgerCard(contentPadding = PaddingValues(18.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { state.openBackup() }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = AppIcon.Backup,
                tint = Primary,
                background = SurfaceSoft,
                size = 40.dp,
                iconSize = 19.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "备份与恢复",
                    color = TextSoft,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$frequency · 保留 ${state.backupRetentionDays} 天",
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AppIconView(
                icon = AppIcon.ChevronRight,
                tint = Muted,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = last?.let { "最近备份：${it.timeLabel} · ${it.versionLabel}" }
                ?: "尚无备份，进入后可立即创建",
            color = Placeholder,
            fontSize = 10.5.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun DataBlock(state: LedgerState) {
    LedgerCard(contentPadding = PaddingValues(18.dp)) {
        Text("数据", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text("所有记录仅存储在这台设备上", color = Muted, fontSize = 11.5.sp)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RedBg.copy(alpha = 0.65f))
                .border(1.dp, Red.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .clickable { state.clearAllData() }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = AppIcon.Trash,
                tint = Red,
                background = CardWhite.copy(alpha = 0.7f),
                size = 36.dp,
                iconSize = 17.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "清空所有记账数据",
                    color = Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text("类型与收购商设置会保留", color = Muted, fontSize = 11.sp)
            }
            AppIconView(AppIcon.ChevronRight, tint = Muted, modifier = Modifier.size(14.dp))
        }
    }
}
