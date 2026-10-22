package com.peanut.ledger.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peanut.ledger.ui.theme.Bg
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.ChipBg
import com.peanut.ledger.ui.theme.IconMuted
import com.peanut.ledger.ui.theme.Placeholder

@Composable
fun SettingsScreen(state: LedgerState) {
    BackHandler { state.closeSettings() }
    Column(Modifier.fillMaxSize().background(Bg)) {
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
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .semantics { contentDescription = "返回" }
                        .clip(CircleShape)
                        .background(CardWhite.copy(alpha = 0.18f))
                        .clickable { state.closeSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = CardWhite, fontSize = 24.sp, lineHeight = 24.sp)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("设置", color = CardWhite, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text("类型 · 收购方", color = CardWhite.copy(alpha = 0.85f), fontSize = 12.sp)
                }
                Spacer(Modifier.width(38.dp))
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SettingsBlock(state, SettingKind.BUY_TYPES) }
            item { SettingsBlock(state, SettingKind.SELL_TYPES) }
            item { SettingsBlock(state, SettingKind.BUYERS) }
            item {
                DangerGhostButton(
                    text = "清空所有记账数据",
                    onClick = { state.clearAllData() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "数据仅保存在本机",
                        color = Placeholder,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "花生账本 · v1.0",
                        color = Placeholder.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsBlock(state: LedgerState, kind: SettingKind) {
    val list = state.settingsList(kind)
    var input by remember(kind) { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = kind.label,
                color = Color(0xFF5C5348),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            if (list.isEmpty()) {
                Text(
                    text = "暂无，可在下方添加",
                    color = Placeholder,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    list.forEach { name ->
                        SettingChip(
                            name = name,
                            onDelete = { state.deleteSetting(kind, name) }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LedgerTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = kind.placeholder
                )
                PrimaryButton(
                    text = "添加",
                    onClick = {
                        state.addSetting(kind, input)
                        input = ""
                    },
                    modifier = Modifier.width(76.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingChip(name: String, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(ChipBg)
            .padding(start = 13.dp, end = 5.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, color = Color(0xFF7A6A55), fontSize = 13.5.sp)
        Spacer(Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Text("×", color = IconMuted, fontSize = 16.sp)
        }
    }
}
