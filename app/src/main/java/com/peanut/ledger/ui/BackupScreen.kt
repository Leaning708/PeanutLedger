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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peanut.ledger.data.BackupInfo
import com.peanut.ledger.data.BackupManager
import com.peanut.ledger.data.BackupReason
import com.peanut.ledger.ui.theme.Bg
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.GoldBg
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.Placeholder
import com.peanut.ledger.ui.theme.Primary
import com.peanut.ledger.ui.theme.Red
import com.peanut.ledger.ui.theme.RedBg
import com.peanut.ledger.ui.theme.SurfaceSoft
import com.peanut.ledger.ui.theme.TextSoft

@Composable
fun BackupScreen(state: LedgerState) {
    BackHandler { state.closeBackup() }
    val backups = remember(state.backupRevision) { state.listBackups() }
    Column(Modifier.fillMaxSize().background(Bg)) {
        BackupHeader(state)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { AutoBackupCard(state, backups.firstOrNull()) }
            item { ManualBackupCard(state) }
            item { BackupHistoryCard(state, backups) }
            item {
                Text(
                    text = "备份默认每天一次、滚动保留 30 天。\n" +
                        "恢复前会自动备份当前数据；备份文件保存在本机应用目录，卸载 APP 或清除应用数据时会一并删除。",
                    color = Placeholder,
                    fontSize = 10.5.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BackupHeader(state: LedgerState) {
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
                onClick = { state.closeBackup() },
                tint = CardWhite,
                background = CardWhite.copy(alpha = 0.14f)
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("备份与恢复", color = CardWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "自动备份 · 版本校验 · 安全恢复",
                    color = CardWhite.copy(alpha = 0.7f),
                    fontSize = 11.5.sp
                )
            }
            Spacer(Modifier.width(40.dp))
        }
    }
}

@Composable
private fun AutoBackupCard(state: LedgerState, latest: BackupInfo?) {
    val frequency = state.backupFrequencyDays
    val retention = state.backupRetentionDays
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("自动备份", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            CounterTag(
                when (frequency) {
                    0 -> "已关闭"
                    1 -> "每天"
                    7 -> "每周"
                    else -> "每${frequency}天"
                }
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = "APP 进入后台时创建，每个周期最多一次；错过周期会在下次后台时补备。",
            color = Muted,
            fontSize = 10.5.sp,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(14.dp))
        Text("备份频率", color = Muted, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(7.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            listOf(0 to "关闭", 1 to "每天", 3 to "每3天", 7 to "每周").forEach { (days, label) ->
                BackupChoiceChip(
                    text = label,
                    selected = frequency == days,
                    onClick = { state.setBackupFrequency(days) }
                )
            }
        }
        Spacer(Modifier.height(13.dp))
        Text("保留时长", color = Muted, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(7.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            listOf(7, 15, 30).forEach { days ->
                BackupChoiceChip(
                    text = "${days}天",
                    selected = retention == days,
                    onClick = { state.setBackupRetention(days) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = latest?.let { "最近备份：${it.timeLabel} · ${it.reason.label}" }
                ?: "还没有备份，点击下方按钮创建第一份",
            color = Placeholder,
            fontSize = 10.5.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun ManualBackupCard(state: LedgerState) {
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Text("手动备份", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(
            "随时保存一份当前数据快照，便于在误操作后恢复。",
            color = Muted,
            fontSize = 10.5.sp,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = "立即备份",
            onClick = { state.createManualBackup() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BackupHistoryCard(state: LedgerState, backups: List<BackupInfo>) {
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("备份记录", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            CounterTag(backups.size.toString())
        }
        Spacer(Modifier.height(3.dp))
        Text(
            "点击备份可还原；备份版本高于当前 APP 时会被阻止。",
            color = Muted,
            fontSize = 10.5.sp,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(10.dp))
        if (backups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceSoft)
                    .padding(horizontal = 14.dp, vertical = 16.dp)
            ) {
                Text("暂无备份记录", color = Placeholder, fontSize = 12.sp)
            }
        } else {
            backups.forEachIndexed { index, info ->
                BackupRow(info) { state.requestRestore(info) }
                if (index != backups.lastIndex) ChevronDivider()
            }
        }
    }
}

@Composable
private fun BackupRow(info: BackupInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (info.blocked) RedBg.copy(alpha = 0.65f) else SurfaceSoft),
            contentAlignment = Alignment.Center
        ) {
            AppIconView(
                icon = AppIcon.Backup,
                tint = if (info.blocked) Red else Primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = info.reason.label,
                    color = TextSoft,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = info.versionLabel,
                    color = if (info.blocked) Red else Muted,
                    fontSize = 9.5.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = info.timeLabel,
                color = Muted,
                fontSize = 10.sp,
                maxLines = 1
            )
            if (info.blocked) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = info.issue ?: "版本不兼容",
                    color = Red,
                    fontSize = 9.5.sp,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = info.sizeLabel,
                color = Muted,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(3.dp))
            AppIconView(
                icon = AppIcon.ChevronRight,
                tint = if (info.blocked) Red.copy(alpha = 0.65f) else Muted,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun BackupChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Primary else SurfaceSoft)
            .border(
                1.dp,
                if (selected) Primary else Line,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) CardWhite else TextSoft,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
