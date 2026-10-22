package com.peanut.ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peanut.ledger.ui.theme.Bg
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.Placeholder
import com.peanut.ledger.ui.theme.Primary
import com.peanut.ledger.ui.theme.PrimaryDark
import com.peanut.ledger.ui.theme.Red
import com.peanut.ledger.ui.theme.RedBg
import com.peanut.ledger.ui.theme.SurfaceSoft
import com.peanut.ledger.ui.theme.TextMain
import com.peanut.ledger.ui.theme.TextSoft

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
                        text = "数据仅保存在本机 · 花生账本 v1.0",
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
                    text = "类型 · 收购方",
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
    val description = when (kind) {
        SettingKind.BUY_TYPES -> "采购时快速选择的花生类型"
        SettingKind.SELL_TYPES -> "销售时快速选择的花生类型"
        SettingKind.BUYERS -> "销售记录中可选的收购方"
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
        Text(description, color = Muted, fontSize = 11.5.sp)
        Spacer(Modifier.height(14.dp))
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                list.forEach { name ->
                    SettingChip(name) { state.deleteSetting(kind, name) }
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
private fun SettingChip(name: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceSoft)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(start = 11.dp, end = 3.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = TextMain, fontSize = 13.sp)
        Spacer(Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            AppIconView(AppIcon.Close, tint = Muted, modifier = Modifier.size(11.dp))
        }
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
                Text("类型与收购方设置会保留", color = Muted, fontSize = 11.sp)
            }
            AppIconView(AppIcon.ChevronRight, tint = Muted, modifier = Modifier.size(14.dp))
        }
    }
}
