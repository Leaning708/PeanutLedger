package com.peanut.ledger.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.peanut.ledger.data.BackupInfo
import com.peanut.ledger.ui.theme.Bg
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.Placeholder
import com.peanut.ledger.ui.theme.Primary
import com.peanut.ledger.ui.theme.Red
import com.peanut.ledger.ui.theme.RedBg
import com.peanut.ledger.ui.theme.SurfaceSoft
import com.peanut.ledger.ui.theme.TextSoft

private const val BACKUP_PAGE_SIZE = 5
private val FREQUENCY_PRESETS = setOf(0, 1, 7, 30)
private val RETENTION_PRESETS = setOf(7, 30, 365)

private enum class CustomBackupKind { FREQUENCY, RETENTION }

@Composable
fun BackupScreen(state: LedgerState) {
    BackHandler { state.closeBackup() }
    val backups = remember(state.backupRevision) { state.listBackups() }
    var customKind by remember { mutableStateOf<CustomBackupKind?>(null) }
    var pendingDownload by remember { mutableStateOf<BackupInfo?>(null) }

    val downloadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val info = pendingDownload
        pendingDownload = null
        if (uri != null && info != null) {
            state.downloadBackup(info, uri)
        }
    }
    val uploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) state.importBackup(uri)
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        BackupHeader(state)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AutoBackupCard(
                    state = state,
                    latest = backups.firstOrNull(),
                    onCustomFrequency = { customKind = CustomBackupKind.FREQUENCY },
                    onCustomRetention = { customKind = CustomBackupKind.RETENTION }
                )
            }
            item {
                ManualBackupCard(
                    state = state,
                    onUpload = {
                        uploadLauncher.launch(
                            arrayOf(
                                "application/json",
                                "text/plain",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    }
                )
            }
            item {
                BackupHistoryCard(
                    state = state,
                    backups = backups,
                    onDownload = { info ->
                        pendingDownload = info
                        downloadLauncher.launch(info.file.name)
                    }
                )
            }
            item {
                Text(
                    text = "恢复前会自动备份当前数据；备份文件保存在本机应用目录，卸载 APP 或清除应用数据时会一并删除。",
                    color = Placeholder,
                    fontSize = 10.5.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 2.dp)
                )
            }
        }
    }

    customKind?.let { kind ->
        val initial = when (kind) {
            CustomBackupKind.FREQUENCY -> {
                val current = state.backupFrequencyDays
                if (current in FREQUENCY_PRESETS) 14 else current
            }

            CustomBackupKind.RETENTION -> {
                val current = state.backupRetentionDays
                if (current in RETENTION_PRESETS) 90 else current
            }
        }
        BackupCustomDialog(
            kind = kind,
            initialValue = initial,
            onDismiss = { customKind = null },
            onSave = { days ->
                when (kind) {
                    CustomBackupKind.FREQUENCY -> state.setBackupFrequency(days)
                    CustomBackupKind.RETENTION -> state.setBackupRetention(days)
                }
                customKind = null
            }
        )
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
private fun AutoBackupCard(
    state: LedgerState,
    latest: BackupInfo?,
    onCustomFrequency: () -> Unit,
    onCustomRetention: () -> Unit
) {
    val frequency = remember(state.backupRevision) { state.backupFrequencyDays }
    val retention = remember(state.backupRevision) { state.backupRetentionDays }
    val frequencyIsCustom = frequency !in FREQUENCY_PRESETS
    val retentionIsCustom = retention !in RETENTION_PRESETS
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("自动备份", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            CounterTag(
                when {
                    frequency == 0 -> "已关闭"
                    frequency == 1 -> "每天"
                    frequency == 7 -> "每周"
                    frequency == 30 -> "每月"
                    else -> "自定义"
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
            listOf(0 to "关闭", 1 to "每天", 7 to "每周", 30 to "每月").forEach { (days, label) ->
                BackupChoiceChip(
                    text = label,
                    selected = frequency == days,
                    onClick = { state.setBackupFrequency(days) }
                )
            }
            BackupChoiceChip(
                text = "自定义",
                selected = frequencyIsCustom,
                onClick = onCustomFrequency
            )
        }
        if (frequencyIsCustom) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "当前：每 $frequency 天备份一次",
                color = Muted,
                fontSize = 9.5.sp
            )
        }
        Spacer(Modifier.height(13.dp))
        Text("保留时长", color = Muted, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(7.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            listOf(7, 30, 365).forEach { days ->
                BackupChoiceChip(
                    text = "${days}天",
                    selected = retention == days,
                    onClick = { state.setBackupRetention(days) }
                )
            }
            BackupChoiceChip(
                text = "自定义",
                selected = retentionIsCustom,
                onClick = onCustomRetention
            )
        }
        if (retentionIsCustom) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "当前：保留 $retention 天",
                color = Muted,
                fontSize = 9.5.sp
            )
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
private fun ManualBackupCard(state: LedgerState, onUpload: () -> Unit) {
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Text("手动备份", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(
            "保存一份当前数据快照；也可以上传其他设备或云端下载的备份文件进行恢复。",
            color = Muted,
            fontSize = 10.5.sp,
            lineHeight = 16.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimaryButton(
                text = "立即备份",
                onClick = { state.createManualBackup() },
                modifier = Modifier.weight(1f)
            )
            GhostButton(
                text = "上传备份",
                onClick = onUpload,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BackupHistoryCard(
    state: LedgerState,
    backups: List<BackupInfo>,
    onDownload: (BackupInfo) -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    val pageCount = maxOf(1, (backups.size + BACKUP_PAGE_SIZE - 1) / BACKUP_PAGE_SIZE)
    LaunchedEffect(backups.size) {
        if (page > pageCount - 1) page = pageCount - 1
    }
    val visible = backups.drop(page * BACKUP_PAGE_SIZE).take(BACKUP_PAGE_SIZE)
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("备份记录", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            CounterTag(backups.size.toString())
            Spacer(Modifier.weight(1f))
            if (backups.isNotEmpty()) {
                Text(
                    text = "清空",
                    color = Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { state.requestDeleteAllBackups(backups.size) }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            "点击备份可还原；右侧可导出或删除。备份版本高于当前 APP 时会被阻止。",
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
            visible.forEachIndexed { index, info ->
                BackupRow(
                    info = info,
                    onRestore = { state.requestRestore(info) },
                    onDownload = { onDownload(info) },
                    onDelete = { state.requestDeleteBackup(info) }
                )
                if (index != visible.lastIndex) ChevronDivider()
            }
            Spacer(Modifier.height(8.dp))
            ChevronDivider()
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackupPageButton(
                    text = "上一页",
                    enabled = page > 0,
                    onClick = { page-- }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "第 ${page + 1}/${pageCount} 页 · 共 ${backups.size} 条",
                    color = Muted,
                    fontSize = 9.5.sp,
                    maxLines = 1
                )
                Spacer(Modifier.weight(1f))
                BackupPageButton(
                    text = "下一页",
                    enabled = page < pageCount - 1,
                    onClick = { page++ }
                )
            }
        }
    }
}

@Composable
private fun BackupRow(
    info: BackupInfo,
    onRestore: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onRestore)
            .padding(vertical = 8.dp),
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
        Spacer(Modifier.width(10.dp))
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
        Spacer(Modifier.width(6.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = info.sizeLabel,
                color = Muted,
                fontSize = 9.5.sp
            )
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BackupIconAction(
                    icon = AppIcon.Download,
                    description = "导出备份",
                    tint = Muted,
                    onClick = onDownload
                )
                BackupIconAction(
                    icon = AppIcon.Trash,
                    description = "删除备份",
                    tint = Red,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun BackupIconAction(
    icon: AppIcon,
    description: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(SurfaceSoft)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppIconView(icon = icon, tint = tint, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun BackupPageButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) SurfaceSoft else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) TextSoft else Placeholder,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
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

@Composable
private fun BackupCustomDialog(
    kind: CustomBackupKind,
    initialValue: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue.toString()) }
    val max = if (kind == CustomBackupKind.FREQUENCY) 365 else 3650
    val value = text.toIntOrNull()
    val valid = value != null && value in 1..max
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            LedgerCard(contentPadding = PaddingValues(18.dp)) {
                Text(
                    text = if (kind == CustomBackupKind.FREQUENCY) "自定义备份频率" else "自定义保留时长",
                    color = TextSoft,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = if (kind == CustomBackupKind.FREQUENCY) {
                        "输入 1–365 天，例如 14 表示每 14 天备份一次。"
                    } else {
                        "输入 1–3650 天，超过这个天数的备份会自动清理。"
                    },
                    color = Muted,
                    fontSize = 10.5.sp,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(12.dp))
                LedgerTextField(
                    value = text,
                    onValueChange = { input ->
                        text = input.filter { it.isDigit() }.take(4)
                    },
                    placeholder = if (kind == CustomBackupKind.FREQUENCY) "天数" else "保留天数",
                    keyboardType = KeyboardType.Number,
                    minHeight = 48.dp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (valid) "单位：天" else "请输入 1–$max 之间的整数",
                    color = if (valid) Muted else Red,
                    fontSize = 9.5.sp
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GhostButton("取消", onDismiss, Modifier.weight(1f))
                    PrimaryButton(
                        text = "保存",
                        onClick = { value?.let(onSave) },
                        modifier = Modifier.weight(1f),
                        enabled = valid
                    )
                }
            }
        }
    }
}
