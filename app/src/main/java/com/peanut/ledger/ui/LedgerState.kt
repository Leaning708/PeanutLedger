package com.peanut.ledger.ui

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.peanut.ledger.data.BackupInfo
import com.peanut.ledger.data.BackupManager
import com.peanut.ledger.data.BackupReason
import com.peanut.ledger.data.DayData
import com.peanut.ledger.data.DayStats
import com.peanut.ledger.data.RestoreCheck
import com.peanut.ledger.data.EntryLocation
import com.peanut.ledger.data.LedgerData
import com.peanut.ledger.data.LedgerEntry
import com.peanut.ledger.data.LedgerItem
import com.peanut.ledger.data.LedgerRepository
import com.peanut.ledger.data.LedgerSettings
import com.peanut.ledger.data.LedgerTab
import com.peanut.ledger.data.LedgerType
import com.peanut.ledger.data.NamedOption
import com.peanut.ledger.data.newId
import com.peanut.ledger.data.nextSupplierName
import com.peanut.ledger.data.nextSupplierNumber
import com.peanut.ledger.data.round2
import com.peanut.ledger.ui.theme.AppTheme
import com.peanut.ledger.ui.theme.setActiveTheme
import java.time.LocalDate
import kotlin.math.abs

enum class AppScreen { MAIN, SETTINGS, STATS, BACKUP }

enum class CalendarMode { DAY, MONTH, YEAR }

enum class SettingKind(val label: String, val placeholder: String) {
    BUY_TYPES("采购类型", "例如：特大果"),
    SELL_TYPES("销售类型", "例如：统货米"),
    BUYERS("收购商", "例如：某某油厂")
}

data class ToastMessage(val id: Long, val text: String)

sealed interface Sheet {
    data object More : Sheet
    data object Calendar : Sheet
    data object DayRemark : Sheet
    data object AddEntry : Sheet
    data class AddType(val entryId: String) : Sheet
    data class ItemForm(
        val entryId: String,
        val typeId: String,
        val itemId: String?,
        val formKey: Long = System.nanoTime()
    ) : Sheet
    data class EntryMenu(val entryId: String) : Sheet
    data class RenameEntry(val entryId: String) : Sheet
    data class EntryRemark(val entryId: String) : Sheet
    data class FinalAmount(val entryId: String) : Sheet
    data class SettingActions(val kind: SettingKind, val name: String) : Sheet
    data class RenameSetting(val kind: SettingKind, val oldName: String) : Sheet
    data class Confirm(
        val title: String,
        val message: String = "",
        val confirmLabel: String = "确定",
        val destructive: Boolean = true,
        val icon: AppIcon? = null,
        val onConfirm: () -> Unit
    ) : Sheet
}

class LedgerState(private val context: Context) {
    var data by mutableStateOf(LedgerRepository.load(context))
        private set

    var currentDate by mutableStateOf(LocalDate.now())
        private set

    var lastToday: LocalDate = LocalDate.now()
        private set

    var tab by mutableStateOf(LedgerTab.BUY)
        private set

    var screen by mutableStateOf(AppScreen.MAIN)
        private set

    var sheet by mutableStateOf<Sheet?>(null)
        private set

    var revealedEntryId by mutableStateOf<String?>(null)
        private set

    var toast by mutableStateOf<ToastMessage?>(null)
        private set

    var dataRevision by mutableIntStateOf(0)
        private set

    var backupRevision by mutableIntStateOf(0)
        private set

    private val backupManager = BackupManager(context)

    var calendarMode by mutableStateOf(CalendarMode.DAY)
        private set

    var calendarAnchor by mutableStateOf(LocalDate.now())
        private set

    private var toastCounter = 0L

    val settings: LedgerSettings get() = data.settings

    val fontScale: Float get() = data.settings.fontScale

    val themeId: String get() = data.settings.theme

    val defaultSettled: Boolean get() = data.settings.defaultSettled

    val recordLocation: Boolean get() = data.settings.recordLocation

    init {
        setActiveTheme(AppTheme.fromId(data.settings.theme))
        pruneAllEmptyTypes()
    }

    fun setFontScale(scale: Float) {
        val clean = scale.coerceIn(1.0f, 1.3f)
        if (data.settings.fontScale == clean) return
        data = data.copy(settings = data.settings.copy(fontScale = clean))
        persist()
    }

    fun setTheme(themeId: String) {
        val theme = AppTheme.fromId(themeId)
        if (data.settings.theme == theme.id) {
            setActiveTheme(theme)
            return
        }
        data = data.copy(settings = data.settings.copy(theme = theme.id))
        setActiveTheme(theme)
        persist()
    }

    fun setDefaultSettled(settled: Boolean) {
        if (data.settings.defaultSettled == settled) return
        data = data.copy(settings = data.settings.copy(defaultSettled = settled))
        persist()
    }

    fun setRecordLocation(enabled: Boolean) {
        if (data.settings.recordLocation == enabled) return
        data = data.copy(settings = data.settings.copy(recordLocation = enabled))
        persist()
    }

    val currentDay: DayData get() = data.day(currentDate)

    val currentEntries: List<LedgerEntry>
        get() = if (tab == LedgerTab.BUY) currentDay.buys else currentDay.sells

    val privacyMode: Boolean get() = data.settings.privacyMode

    fun isRevealed(entryId: String): Boolean = !privacyMode || revealedEntryId == entryId

    fun displayTitle(
        entry: LedgerEntry,
        targetTab: LedgerTab = tab,
        displayIndex: Int? = null
    ): String {
        if (targetTab == LedgerTab.SELL && privacyMode) {
            val number = displayIndex
                ?: (currentDay.sells.indexOfFirst { it.id == entry.id } + 1).takeIf { it > 0 }
            return if (number != null) "收购商$number" else "收购商"
        }
        return entry.title(targetTab)
    }

    fun toggleTab(next: LedgerTab) {
        if (tab == next) return
        tab = next
        revealedEntryId = null
    }

    fun previousDay() {
        currentDate = currentDate.minusDays(1)
        revealedEntryId = null
        sheet = null
    }

    fun nextDay() {
        currentDate = currentDate.plusDays(1)
        revealedEntryId = null
        sheet = null
    }

    /** Swipe left: buy -> sell (same day), sell -> buy (next day). */
    fun swipeForwardPage() {
        if (tab == LedgerTab.BUY) {
            tab = LedgerTab.SELL
        } else {
            tab = LedgerTab.BUY
            currentDate = currentDate.plusDays(1)
        }
        revealedEntryId = null
        sheet = null
    }

    /** Swipe right: sell -> buy (same day), buy -> sell (previous day). */
    fun swipeBackwardPage() {
        if (tab == LedgerTab.SELL) {
            tab = LedgerTab.BUY
        } else {
            tab = LedgerTab.SELL
            currentDate = currentDate.minusDays(1)
        }
        revealedEntryId = null
        sheet = null
    }

    fun goToday() {
        currentDate = LocalDate.now()
        revealedEntryId = null
        sheet = null
    }

    fun selectDate(date: LocalDate) {
        currentDate = date
        revealedEntryId = null
        sheet = null
        showToast(com.peanut.ledger.data.dateLabel(date))
    }

    fun togglePrivacy() {
        data = data.copy(settings = data.settings.copy(privacyMode = !privacyMode))
        if (!privacyMode) revealedEntryId = null
        persist()
        showToast(if (privacyMode) "隐私模式已开启" else "隐私模式已关闭")
    }

    fun toggleReveal(entryId: String) {
        if (!privacyMode) return
        revealedEntryId = if (revealedEntryId == entryId) null else entryId
        showToast(if (revealedEntryId == entryId) "已单独显示该户金额" else "已重新隐藏该户金额")
    }

    fun canEditSensitive(entryId: String): Boolean {
        if (!privacyMode || revealedEntryId == entryId) return true
        showToast("请先双击或长按卡片显示后再操作")
        return false
    }

    fun showToast(message: String) {
        toast = ToastMessage(++toastCounter, message)
    }

    fun clearToast(id: Long) {
        if (toast?.id == id) toast = null
    }

    fun openSheet(next: Sheet) {
        sheet = next
    }

    fun closeSheet() {
        val current = sheet
        if (current is Sheet.ItemForm) {
            pruneEmptyType(current.entryId, current.typeId)
        }
        sheet = null
    }

    fun openSettings() {
        sheet = null
        screen = AppScreen.SETTINGS
    }

    fun closeSettings() {
        screen = AppScreen.MAIN
    }

    fun openStats() {
        sheet = null
        screen = AppScreen.STATS
    }

    fun closeStats() {
        screen = AppScreen.MAIN
    }

    fun openBackup() {
        sheet = null
        screen = AppScreen.BACKUP
    }

    fun closeBackup() {
        screen = AppScreen.SETTINGS
    }

    val backupFrequencyDays: Int get() = backupManager.frequencyDays
    val backupRetentionDays: Int get() = backupManager.retentionDays
    val backupLastSuccessAt: Long get() = backupManager.lastSuccessAt

    fun setBackupFrequency(days: Int) {
        backupManager.frequencyDays = days
        backupRevision++
    }

    fun setBackupRetention(days: Int) {
        backupManager.retentionDays = days
        backupRevision++
    }

    fun listBackups(): List<BackupInfo> = backupManager.listBackups()

    fun createManualBackup() {
        val info = backupManager.createBackup(data, BackupReason.MANUAL)
        if (info != null) {
            backupRevision++
            showToast("已创建手动备份")
        } else {
            showToast("备份失败，请检查存储空间")
        }
    }

    /** Safe checkpoint when the APP leaves the foreground or starts a new day. */
    fun maybeAutoBackup() {
        if (backupManager.maybeAutoBackup(data) != null) {
            backupRevision++
        }
    }

    fun requestRestore(info: BackupInfo) {
        when (val check = backupManager.checkRestore(info.file)) {
            is RestoreCheck.Blocked -> {
                openSheet(
                    Sheet.Confirm(
                        title = "无法还原此备份",
                        message = check.message,
                        confirmLabel = "知道了",
                        destructive = false,
                        icon = AppIcon.Backup,
                        onConfirm = {}
                    )
                )
            }

            is RestoreCheck.Invalid -> {
                openSheet(
                    Sheet.Confirm(
                        title = "备份不可用",
                        message = check.message,
                        confirmLabel = "知道了",
                        destructive = false,
                        icon = AppIcon.Backup,
                        onConfirm = {}
                    )
                )
            }

            is RestoreCheck.Ready -> {
                val migratedLine = if (check.migrated) {
                    "\n这是一份旧版本备份，将按当前版本的数据结构自动兼容读取。"
                } else {
                    ""
                }
                val message = "备份时间：${check.info.timeLabel}\n" +
                    "备份数据版本：${check.info.versionLabel}（code ${check.info.appVersionCode}）\n" +
                    "当前 APP 版本：v${BackupManager.currentAppVersionName()}（code ${BackupManager.currentAppVersionCode()}）\n\n" +
                    "恢复后，当前所有记账、类型、收购商和设置都会被这份历史数据覆盖，且无法撤销。" +
                    "恢复前会先自动创建一份当前数据的安全备份。$migratedLine"
                openSheet(
                    Sheet.Confirm(
                        title = "恢复此备份？",
                        message = message,
                        confirmLabel = "覆盖并恢复",
                        destructive = true,
                        icon = AppIcon.Backup,
                        onConfirm = { restoreBackup(check.info, check.data) }
                    )
                )
            }
        }
    }

    fun downloadBackup(info: BackupInfo, uri: Uri) {
        if (backupManager.copyBackupTo(info.file, uri)) {
            showToast("备份文件已导出")
        } else {
            showToast("导出失败，请检查存储权限或空间")
        }
    }

    fun importBackup(uri: Uri) {
        val file = backupManager.importBackup(uri)
        if (file == null) {
            showToast("备份文件读取失败")
            return
        }
        when (val check = backupManager.checkRestore(file)) {
            is RestoreCheck.Ready -> {
                backupRevision++
                requestRestore(check.info)
            }

            is RestoreCheck.Blocked -> {
                backupManager.deleteBackup(file)
                backupRevision++
                openSheet(
                    Sheet.Confirm(
                        title = "无法还原此备份",
                        message = check.message,
                        confirmLabel = "知道了",
                        destructive = false,
                        icon = AppIcon.Backup,
                        onConfirm = {}
                    )
                )
            }

            is RestoreCheck.Invalid -> {
                backupManager.deleteBackup(file)
                backupRevision++
                openSheet(
                    Sheet.Confirm(
                        title = "备份文件不可用",
                        message = check.message,
                        confirmLabel = "知道了",
                        destructive = false,
                        icon = AppIcon.Backup,
                        onConfirm = {}
                    )
                )
            }
        }
    }

    fun requestDeleteBackup(info: BackupInfo) {
        openSheet(
            Sheet.Confirm(
                title = "删除此备份？",
                message = "将删除 ${info.timeLabel} 的「${info.reason.label}」（${info.versionLabel}）。\n删除后无法恢复，但不会影响当前账本数据。",
                confirmLabel = "删除备份",
                destructive = true,
                icon = AppIcon.Trash,
                onConfirm = {
                    if (backupManager.deleteBackup(info.file)) {
                        backupRevision++
                        showToast("备份已删除")
                    } else {
                        showToast("删除失败")
                    }
                }
            )
        )
    }

    fun requestDeleteAllBackups(count: Int) {
        if (count <= 0) return
        openSheet(
            Sheet.Confirm(
                title = "删除全部备份？",
                message = "将删除全部 $count 条备份记录，删除后无法恢复。\n当前账本数据不会被删除。",
                confirmLabel = "全部删除",
                destructive = true,
                icon = AppIcon.Trash,
                onConfirm = {
                    val deleted = backupManager.deleteAllBackups()
                    backupRevision++
                    showToast(if (deleted > 0) "已删除 $deleted 条备份" else "没有可删除的备份")
                }
            )
        )
    }

    private fun restoreBackup(info: BackupInfo, restored: LedgerData) {
        val safety = backupManager.createBackup(data, BackupReason.BEFORE_RESTORE)
        if (safety == null) {
            showToast("无法创建恢复前安全备份，已取消恢复")
            return
        }
        data = restored
        setActiveTheme(AppTheme.fromId(data.settings.theme))
        LedgerRepository.save(context, data)
        dataRevision++
        backupRevision++
        currentDate = LocalDate.now()
        lastToday = LocalDate.now()
        tab = LedgerTab.BUY
        revealedEntryId = null
        sheet = null
        showToast("已恢复 ${info.timeLabel} 的备份")
    }

    fun openCalendar() {
        calendarAnchor = currentDate
        calendarMode = CalendarMode.DAY
        sheet = Sheet.Calendar
    }

    fun changeCalendarMode(mode: CalendarMode) {
        calendarMode = mode
    }

    fun shiftCalendar(direction: Int) {
        calendarAnchor = when (calendarMode) {
            CalendarMode.DAY -> calendarAnchor.withDayOfMonth(1).plusMonths(direction.toLong())
            CalendarMode.MONTH -> LocalDate.of(calendarAnchor.year + direction, 1, 1)
            CalendarMode.YEAR -> LocalDate.of(calendarAnchor.year + direction * 12, 1, 1)
        }
    }

    fun pickCalendarMonth(year: Int, month: Int) {
        calendarAnchor = LocalDate.of(year, month, 1)
        calendarMode = CalendarMode.DAY
    }

    fun pickCalendarYear(year: Int) {
        calendarAnchor = LocalDate.of(year, 1, 1)
        calendarMode = CalendarMode.MONTH
    }

    fun checkDayChange() {
        val now = LocalDate.now()
        if (now == lastToday) return
        val wasToday = currentDate == lastToday
        lastToday = now
        if (wasToday) {
            currentDate = now
            revealedEntryId = null
            sheet = null
        }
        maybeAutoBackup()
    }

    // ---------------------------------------------------------------- data ops

    fun day(date: LocalDate): DayData = data.day(date)

    fun entriesFor(date: LocalDate, tab: LedgerTab): List<LedgerEntry> = data.day(date).entries(tab)

    fun findEntry(entryId: String): LedgerEntry? {
        val day = currentDay
        return day.buys.firstOrNull { it.id == entryId } ?: day.sells.firstOrNull { it.id == entryId }
    }

    fun statsOf(date: LocalDate): DayStats {
        val day = data.days[date.toString()] ?: return DayStats(0.0, 0.0)
        return DayStats(
            buy = round2(day.buys.sumOf { com.peanut.ledger.data.entryFinal(it) }),
            sell = round2(day.sells.sumOf { com.peanut.ledger.data.entryFinal(it) })
        )
    }

    fun dayHasUnsettled(date: LocalDate): Boolean =
        data.days[date.toString()]?.sells?.any { !it.settled } == true

    fun monthHasUnsettled(year: Int, month: Int): Boolean {
        val prefix = String.format("%04d-%02d", year, month)
        return data.days.any { (key, day) -> key.startsWith(prefix) && day.sells.any { !it.settled } }
    }

    fun yearHasUnsettled(year: Int): Boolean {
        val prefix = String.format("%04d-", year)
        return data.days.any { (key, day) -> key.startsWith(prefix) && day.sells.any { !it.settled } }
    }

    fun monthSummary(year: Int, month: Int): DayStats {
        val prefix = String.format("%04d-%02d-", year, month)
        var buy = 0.0
        var sell = 0.0
        data.days.forEach { (key, day) ->
            if (key.startsWith(prefix)) {
                buy += day.buys.sumOf { com.peanut.ledger.data.entryFinal(it) }
                sell += day.sells.sumOf { com.peanut.ledger.data.entryFinal(it) }
            }
        }
        return DayStats(round2(buy), round2(sell))
    }

    fun yearSummary(year: Int): DayStats {
        val prefix = String.format("%04d-", year)
        var buy = 0.0
        var sell = 0.0
        data.days.forEach { (key, day) ->
            if (key.startsWith(prefix)) {
                buy += day.buys.sumOf { com.peanut.ledger.data.entryFinal(it) }
                sell += day.sells.sumOf { com.peanut.ledger.data.entryFinal(it) }
            }
        }
        return DayStats(round2(buy), round2(sell))
    }

    fun rangeSummary(startYear: Int, count: Int = 12): DayStats {
        var buy = 0.0
        var sell = 0.0
        for (year in startYear until startYear + count) {
            val summary = yearSummary(year)
            buy += summary.buy
            sell += summary.sell
        }
        return DayStats(round2(buy), round2(sell))
    }

    fun nextSupplierName(): String = nextSupplierName(currentDay.buys)

    fun addEntry(
        name: String,
        buyer: String,
        location: EntryLocation? = null
    ) {
        var openTypeFor: String? = null
        if (tab == LedgerTab.BUY) {
            val finalName = name.trim().ifBlank { nextSupplierName() }
            val entry = LedgerEntry(
                id = newId(),
                name = finalName,
                createdAt = System.currentTimeMillis(),
                location = location
            )
            updateDay { it.copy(buys = it.buys + entry) }
            openTypeFor = entry.id
            showToast("已添加 $finalName，请选择采购类型")
        } else {
            val finalBuyer = buyer.trim()
            val validBuyer = data.settings.buyers.any { it.active && it.name == finalBuyer }
            if (finalBuyer.isBlank()) {
                showToast("请选择收购商")
                return
            }
            if (!validBuyer) {
                showToast("请选择设置中的收购商")
                return
            }
            val entry = LedgerEntry(
                id = newId(),
                buyer = finalBuyer,
                settled = data.settings.defaultSettled,
                createdAt = System.currentTimeMillis()
            )
            updateDay { it.copy(sells = it.sells + entry) }
            showToast("已添加 $finalBuyer")
        }
        persist()
        sheet = openTypeFor?.let { Sheet.AddType(it) }
    }

    fun renameEntry(entryId: String, newName: String) {
        if (tab != LedgerTab.BUY) {
            showToast("收购商名称请在设置中修改")
            return
        }
        val clean = newName.trim()
        if (clean.isBlank()) {
            showToast("请输入名称")
            return
        }
        updateEntry(entryId) { entry -> entry.copy(name = clean) }
        sheet = null
        showToast("已修改")
    }

    private fun normalizeSupplierNames() {
        var changed = false
        val days = data.days.mapValues { (_, day) ->
            val buys = day.buys.map { entry ->
                val clean = entry.name.trim()
                val number = when {
                    clean.startsWith("户") -> clean.removePrefix("户").toIntOrNull()
                    else -> clean.toIntOrNull()
                }
                if (number != null && clean != "供货商$number") {
                    changed = true
                    entry.copy(name = "供货商$number")
                } else {
                    entry
                }
            }
            if (buys != day.buys) day.copy(buys = buys) else day
        }
        if (changed) {
            data = data.copy(days = days)
            persist()
        }
    }

    private fun ensureSaleBuyersBound() {
        val existing = data.settings.buyers.map { it.name }.toSet()
        val missing = data.days.values
            .flatMap { it.sells }
            .map { it.buyer.trim() }
            .filter { it.isNotBlank() && it !in existing }
            .distinct()
        if (missing.isEmpty()) return
        data = data.copy(
            settings = data.settings.copy(
                buyers = data.settings.buyers + missing.map { NamedOption(it) }
            )
        )
        persist()
    }

    fun toggleSettled(entryId: String) {
        var settled = false
        updateEntry(entryId) { entry ->
            settled = !entry.settled
            entry.copy(settled = settled)
        }
        sheet = null
        showToast(if (settled) "已标记为结清" else "已标记为赊账")
    }

    fun deleteEntry(entryId: String) {
        val entry = findEntry(entryId) ?: return
        val label = displayTitle(entry, tab)
        openSheet(
            Sheet.Confirm(
                title = "删除记录",
                message = "确定删除「$label」及其全部记录？",
                confirmLabel = "删除"
            ) {
                updateDay { day ->
                    day.copy(
                        buys = day.buys.filterNot { it.id == entryId },
                        sells = day.sells.filterNot { it.id == entryId }
                    )
                }
                if (revealedEntryId == entryId) revealedEntryId = null
                sheet = null
                showToast("已删除")
            }
        )
    }

    fun saveEntryRemark(entryId: String, remark: String) {
        updateEntry(entryId) { it.copy(remark = remark.trim()) }
        sheet = null
        showToast("备注已保存")
    }

    fun clearEntryRemark(entryId: String) {
        updateEntry(entryId) { it.copy(remark = "") }
        sheet = null
        showToast("备注已清除")
    }

    fun saveDayRemark(remark: String) {
        updateDay { it.copy(remark = remark.trim()) }
        sheet = null
        showToast("备注已保存")
    }

    fun clearDayRemark() {
        updateDay { it.copy(remark = "") }
        sheet = null
        showToast("备注已清除")
    }

    fun saveFinalAmount(entryId: String, value: Double) {
        val entry = findEntry(entryId) ?: return
        val total = com.peanut.ledger.data.entryTotal(entry)
        updateEntry(entryId) {
            if (abs(value - total) < 0.005) {
                it.copy(finalOverride = null, adjust = 0.0)
            } else {
                it.copy(finalOverride = round2(value), adjust = 0.0)
            }
        }
        sheet = null
        showToast("已保存")
    }

    fun addTypeAndOpen(entryId: String, name: String, saveAsPreset: Boolean) {
        val clean = name.trim()
        if (clean.isBlank()) {
            showToast("请输入类型名称")
            return
        }
        var typeId = ""
        updateEntry(entryId) { entry ->
            val existing = entry.types.firstOrNull { it.name == clean }
            if (existing != null) {
                typeId = existing.id
                entry
            } else {
                val type = LedgerType(id = newId(), name = clean)
                typeId = type.id
                entry.copy(types = entry.types + type)
            }
        }
        if (saveAsPreset) addPreset(clean)
        sheet = Sheet.ItemForm(entryId, typeId, null)
    }

    fun revealForAction(entryId: String) {
        if (privacyMode) revealedEntryId = entryId
    }

    fun openAddType(entryId: String) {
        revealForAction(entryId)
        sheet = Sheet.AddType(entryId)
    }

    fun openItemForm(entryId: String, typeId: String, itemId: String?) {
        revealForAction(entryId)
        sheet = Sheet.ItemForm(entryId, typeId, itemId)
    }

    fun saveItem(
        entryId: String,
        typeId: String,
        itemId: String?,
        weight: Double,
        price: Double,
        amountOverride: Double?,
        keepOpen: Boolean
    ): Boolean {
        val cleanWeight = round2(weight)
        if (cleanWeight <= 0.0) return false
        val cleanPrice = round2(price)
        val cleanAmount = amountOverride?.takeIf {
            abs(round2(it) - round2(cleanWeight * cleanPrice)) >= 0.005
        }
        updateEntry(entryId) { entry ->
            entry.copy(
                types = entry.types.map { type ->
                    if (type.id != typeId) return@map type
                    val items = if (itemId != null) {
                        type.items.map { item ->
                            if (item.id == itemId) {
                                item.copy(
                                    weight = cleanWeight,
                                    price = cleanPrice,
                                    manualAmount = cleanAmount
                                )
                            } else {
                                item
                            }
                        }
                    } else {
                        type.items + LedgerItem(
                            id = newId(),
                            weight = cleanWeight,
                            price = cleanPrice,
                            manualAmount = cleanAmount
                        )
                    }
                    type.copy(items = items)
                }
            )
        }
        if (keepOpen) {
            sheet = Sheet.ItemForm(entryId, typeId, null)
            showToast("已保存，继续记下一称")
        } else {
            sheet = null
            showToast("已保存")
        }
        return true
    }

    fun deleteItem(entryId: String, typeId: String, itemId: String) {
        updateEntry(entryId) { entry ->
            entry.copy(
                types = entry.types.map { type ->
                    if (type.id == typeId) {
                        type.copy(items = type.items.filterNot { it.id == itemId })
                    } else {
                        type
                    }
                }
            )
        }
        pruneEmptyType(entryId, typeId)
        sheet = null
        showToast("已删除")
    }

    fun findLastPrice(entryId: String, typeId: String): Double? {
        val entry = findEntry(entryId) ?: return null
        com.peanut.ledger.data.findLastPriceInEntry(entry, typeId)?.let { return it }

        val targetName = entry.title(tab)
        val targetType = entry.types.firstOrNull { it.id == typeId }?.name ?: return null
        if (targetName.isBlank() || targetType.isBlank()) return null
        val dates = data.days.keys.sortedDescending()
        for (key in dates) {
            if (key == currentDate.toString()) continue
            val day = data.days[key] ?: continue
            val list = day.entries(tab)
            val match = list.firstOrNull { it.title(tab) == targetName } ?: continue
            val type = match.types.firstOrNull { it.name == targetType } ?: continue
            type.items.lastOrNull()?.let { return it.price }
        }
        return null
    }

    fun addPreset(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val isBuy = tab == LedgerTab.BUY
        val list = if (isBuy) data.settings.buyTypes else data.settings.sellTypes
        val index = list.indexOfFirst { it.name == clean }
        val next = if (index >= 0) {
            if (list[index].active) return
            list.toMutableList().also { it[index] = it[index].copy(active = true) }
        } else {
            list + NamedOption(clean)
        }
        data = data.copy(
            settings = if (isBuy) {
                data.settings.copy(buyTypes = next)
            } else {
                data.settings.copy(sellTypes = next)
            }
        )
        persist()
    }

    fun activeSettingsNames(kind: SettingKind): List<String> =
        settingsList(kind).filter { it.active }.map { it.name }

    fun activeBuyTypes(): List<String> = activeSettingsNames(SettingKind.BUY_TYPES)

    fun activeSellTypes(): List<String> = activeSettingsNames(SettingKind.SELL_TYPES)

    fun activeBuyers(): List<String> = activeSettingsNames(SettingKind.BUYERS)

    fun addSetting(kind: SettingKind, name: String) {
        val clean = name.trim()
        if (clean.isBlank()) {
            showToast("请输入名称")
            return
        }
        val list = settingsList(kind)
        val index = list.indexOfFirst { it.name == clean }
        if (index >= 0) {
            if (list[index].active) {
                showToast("已存在")
                return
            }
            updateSettingsList(
                kind,
                list.toMutableList().also { it[index] = it[index].copy(active = true) }
            )
            showToast("已恢复使用")
            return
        }
        updateSettingsList(kind, list + NamedOption(clean))
        showToast("已添加")
    }

    fun setSettingActive(kind: SettingKind, name: String, active: Boolean) {
        val list = settingsList(kind)
        val index = list.indexOfFirst { it.name == name }
        if (index < 0) return
        if (list[index].active == active) return
        updateSettingsList(
            kind,
            list.toMutableList().also { it[index] = it[index].copy(active = active) }
        )
        showToast(if (active) "已恢复使用" else "已设为失效")
    }

    fun moveSetting(kind: SettingKind, name: String, direction: Int) {
        val list = settingsList(kind).toMutableList()
        val index = list.indexOfFirst { it.name == name }
        val target = index + direction
        if (index < 0 || target !in list.indices) return
        val item = list[index]
        list[index] = list[target]
        list[target] = item
        updateSettingsList(kind, list)
    }

    fun moveSettingTo(kind: SettingKind, name: String, targetIndex: Int) {
        val list = settingsList(kind).toMutableList()
        val from = list.indexOfFirst { it.name == name }
        if (from < 0) return
        val target = targetIndex.coerceIn(0, list.lastIndex)
        if (from == target) return
        val item = list.removeAt(from)
        list.add(target, item)
        updateSettingsList(kind, list)
    }

    fun lastUsedType(targetTab: LedgerTab = tab): String? {
        val entries = data.days.values
            .flatMap { if (targetTab == LedgerTab.BUY) it.buys else it.sells }
            .filter { it.types.isNotEmpty() }
        val last = entries.maxByOrNull { it.createdAt }?.types?.lastOrNull()?.name
        val active = activeSettingsNames(
            if (targetTab == LedgerTab.BUY) SettingKind.BUY_TYPES else SettingKind.SELL_TYPES
        )
        return when {
            last == null -> active.firstOrNull()
            last in active -> last
            else -> active.firstOrNull()
        }
    }

    fun settingUsage(kind: SettingKind, name: String): Int = when (kind) {
        SettingKind.BUY_TYPES -> data.days.values.sumOf { day ->
            day.buys.count { entry -> entry.types.any { it.name == name } }
        }
        SettingKind.SELL_TYPES -> data.days.values.sumOf { day ->
            day.sells.count { entry -> entry.types.any { it.name == name } }
        }
        SettingKind.BUYERS -> data.days.values.sumOf { day ->
            day.sells.count { it.buyer == name }
        }
    }

    fun requestDeleteSetting(kind: SettingKind, name: String) {
        val usage = settingUsage(kind, name)
        if (usage > 0) {
            openSheet(
                Sheet.Confirm(
                    title = "无法删除",
                    message = "「$name」已被 $usage 条记录使用。可以将其设为失效，失效后记账时将不再显示。",
                    confirmLabel = "设为失效",
                    destructive = false,
                    icon = AppIcon.EyeOff
                ) {
                    setSettingActive(kind, name, false)
                    sheet = null
                }
            )
        } else {
            openSheet(
                Sheet.Confirm(
                    title = "删除${kind.label}",
                    message = "确定删除「$name」吗？",
                    confirmLabel = "删除"
                ) {
                    updateSettingsList(kind, settingsList(kind).filterNot { it.name == name })
                    sheet = null
                    showToast("已删除")
                }
            )
        }
    }

    fun renameSetting(kind: SettingKind, oldName: String, newName: String): Boolean {
        val clean = newName.trim()
        if (clean.isBlank()) {
            showToast("请输入名称")
            return false
        }
        if (clean == oldName) {
            sheet = null
            return true
        }
        val list = settingsList(kind)
        if (list.any { it.name == clean }) {
            showToast("名称已存在")
            return false
        }
        val index = list.indexOfFirst { it.name == oldName }
        if (index < 0) {
            showToast("选项不存在")
            return false
        }
        val usage = settingUsage(kind, oldName)
        updateSettingsList(
            kind,
            list.toMutableList().also { it[index] = it[index].copy(name = clean) }
        )
        data = data.copy(
            days = data.days.mapValues { (_, day) -> renameInDay(kind, day, oldName, clean) }
        )
        persist()
        sheet = null
        showToast(if (usage > 0) "已重命名，并同步更新 $usage 条记录" else "已重命名")
        return true
    }

    fun settingsList(kind: SettingKind): List<NamedOption> = when (kind) {
        SettingKind.BUY_TYPES -> data.settings.buyTypes
        SettingKind.SELL_TYPES -> data.settings.sellTypes
        SettingKind.BUYERS -> data.settings.buyers
    }

    private fun updateSettingsList(kind: SettingKind, list: List<NamedOption>) {
        val next = when (kind) {
            SettingKind.BUY_TYPES -> data.settings.copy(buyTypes = list)
            SettingKind.SELL_TYPES -> data.settings.copy(sellTypes = list)
            SettingKind.BUYERS -> data.settings.copy(buyers = list)
        }
        data = data.copy(settings = next)
        persist()
    }

    private fun renameInDay(kind: SettingKind, day: DayData, oldName: String, newName: String): DayData =
        when (kind) {
            SettingKind.BUY_TYPES -> day.copy(
                buys = day.buys.map {
                    it.copy(types = renameTypes(it.types, oldName, newName))
                }
            )
            SettingKind.SELL_TYPES -> day.copy(
                sells = day.sells.map {
                    it.copy(types = renameTypes(it.types, oldName, newName))
                }
            )
            SettingKind.BUYERS -> day.copy(
                sells = day.sells.map {
                    if (it.buyer == oldName) it.copy(buyer = newName) else it
                }
            )
        }

    private fun renameTypes(
        types: List<LedgerType>,
        oldName: String,
        newName: String
    ): List<LedgerType> {
        if (types.none { it.name == oldName }) return types
        val renamed = types.map { if (it.name == oldName) it.copy(name = newName) else it }
        val merged = mutableListOf<LedgerType>()
        renamed.forEach { type ->
            val existing = merged.indexOfFirst { it.name == type.name }
            if (existing >= 0) {
                merged[existing] = merged[existing].copy(items = merged[existing].items + type.items)
            } else {
                merged.add(type)
            }
        }
        return merged
    }

    fun clearAllData() {
        openSheet(
            Sheet.Confirm(
                title = "清空所有记账数据",
                message = "类型和收购商设置会保留，此操作不可撤销。",
                confirmLabel = "清空"
            ) {
                data = data.copy(days = emptyMap())
                revealedEntryId = null
                persist()
                sheet = null
                showToast("已清空")
            }
        )
    }

    private fun pruneEmptyType(entryId: String, typeId: String) {
        val entry = findEntry(entryId) ?: return
        val type = entry.types.firstOrNull { it.id == typeId } ?: return
        if (type.items.isNotEmpty()) return
        val day = currentDay
        val affectedBuy = day.buys.any { it.id == entryId }
        val affectedSell = day.sells.any { it.id == entryId }
        if (!affectedBuy && !affectedSell) return
        updateDay { current ->
            current.copy(
                buys = current.buys.map {
                    if (it.id == entryId) it.copy(types = it.types.filterNot { t -> t.id == typeId }) else it
                },
                sells = current.sells.map {
                    if (it.id == entryId) it.copy(types = it.types.filterNot { t -> t.id == typeId }) else it
                }
            )
        }
    }

    private fun pruneAllEmptyTypes() {
        val cleaned = data.days.mapValues { (_, day) ->
            day.copy(
                buys = day.buys.map { entry ->
                    entry.copy(types = entry.types.filter { it.items.isNotEmpty() })
                },
                sells = day.sells.map { entry ->
                    entry.copy(types = entry.types.filter { it.items.isNotEmpty() })
                }
            )
        }
        if (cleaned != data.days) {
            data = data.copy(days = cleaned)
            persist()
        }
    }

    private fun updateDay(transform: (DayData) -> DayData) {
        val key = currentDate.toString()
        val existing = data.days[key] ?: DayData()
        data = data.copy(days = data.days + (key to transform(existing)))
        persist()
    }

    private fun updateEntry(entryId: String, transform: (LedgerEntry) -> LedgerEntry) {
        updateDay { day ->
            day.copy(
                buys = day.buys.map { if (it.id == entryId) transform(it) else it },
                sells = day.sells.map { if (it.id == entryId) transform(it) else it }
            )
        }
    }

    private fun persist() {
        LedgerRepository.save(context, data)
        dataRevision++
    }
}
