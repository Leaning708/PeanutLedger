package com.peanut.ledger.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.peanut.ledger.data.DayData
import com.peanut.ledger.data.DayStats
import com.peanut.ledger.data.LedgerData
import com.peanut.ledger.data.LedgerEntry
import com.peanut.ledger.data.LedgerItem
import com.peanut.ledger.data.LedgerRepository
import com.peanut.ledger.data.LedgerSettings
import com.peanut.ledger.data.LedgerTab
import com.peanut.ledger.data.LedgerType
import com.peanut.ledger.data.newId
import com.peanut.ledger.data.nextFarmerName
import com.peanut.ledger.data.round2
import java.time.LocalDate
import kotlin.math.abs

enum class AppScreen { MAIN, SETTINGS }

enum class CalendarMode { DAY, MONTH, YEAR }

enum class SettingKind(val label: String, val placeholder: String) {
    BUY_TYPES("采购花生类型", "例如：特大果"),
    SELL_TYPES("销售花生类型", "例如：统货米"),
    BUYERS("收购方", "例如：某某油厂")
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
    data class Confirm(
        val title: String,
        val message: String = "",
        val confirmLabel: String = "确定",
        val destructive: Boolean = true,
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

    var calendarMode by mutableStateOf(CalendarMode.DAY)
        private set

    var calendarAnchor by mutableStateOf(LocalDate.now())
        private set

    private var toastCounter = 0L

    val settings: LedgerSettings get() = data.settings

    val currentDay: DayData get() = data.day(currentDate)

    val currentEntries: List<LedgerEntry>
        get() = if (tab == LedgerTab.BUY) currentDay.buys else currentDay.sells

    val privacyMode: Boolean get() = data.settings.privacyMode

    fun isRevealed(entryId: String): Boolean = !privacyMode || revealedEntryId == entryId

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
        showToast("请先双击该户卡片，显示后再操作")
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
        sheet = null
    }

    fun openSettings() {
        sheet = null
        screen = AppScreen.SETTINGS
    }

    fun closeSettings() {
        screen = AppScreen.MAIN
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

    fun nextFarmerName(): String = nextFarmerName(currentDay.buys)

    fun addEntry(name: String, buyer: String, newBuyer: Boolean = false) {
        if (tab == LedgerTab.BUY) {
            val finalName = name.trim().ifBlank { nextFarmerName(currentDay.buys) }
            val entry = LedgerEntry(
                id = newId(),
                name = finalName,
                createdAt = System.currentTimeMillis()
            )
            updateDay { it.copy(buys = it.buys + entry) }
            showToast("已添加 $finalName")
        } else {
            val finalBuyer = buyer.trim()
            if (finalBuyer.isBlank()) {
                showToast("请选择收购方")
                return
            }
            if (newBuyer && !data.settings.buyers.contains(finalBuyer)) {
                data = data.copy(settings = data.settings.copy(buyers = data.settings.buyers + finalBuyer))
            }
            val entry = LedgerEntry(
                id = newId(),
                buyer = finalBuyer,
                settled = false,
                createdAt = System.currentTimeMillis()
            )
            updateDay { it.copy(sells = it.sells + entry) }
            showToast("已添加 $finalBuyer")
        }
        persist()
        sheet = null
    }

    fun renameEntry(entryId: String, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) {
            showToast("请输入名称")
            return
        }
        updateEntry(entryId) { entry ->
            if (tab == LedgerTab.BUY) entry.copy(name = clean) else entry.copy(buyer = clean)
        }
        sheet = null
        showToast("已修改")
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
        val label = entry.title(tab)
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

    fun openItemForm(entryId: String, typeId: String, itemId: String?) {
        if (!canEditSensitive(entryId)) return
        sheet = Sheet.ItemForm(entryId, typeId, itemId)
    }

    fun saveItem(
        entryId: String,
        typeId: String,
        itemId: String?,
        weight: Double,
        price: Double,
        amount: Double?,
        keepOpen: Boolean
    ) {
        val auto = round2(weight * price)
        val finalAmount = amount ?: auto
        if (weight <= 0.0 && price <= 0.0 && finalAmount <= 0.0) {
            showToast("请填写重量和单价")
            return
        }
        val manualAmount = if (abs(finalAmount - auto) < 0.005) null else round2(finalAmount)
        updateEntry(entryId) { entry ->
            entry.copy(
                types = entry.types.map { type ->
                    if (type.id != typeId) return@map type
                    val items = if (itemId != null) {
                        type.items.map { item ->
                            if (item.id == itemId) {
                                item.copy(weight = weight, price = price, manualAmount = manualAmount)
                            } else {
                                item
                            }
                        }
                    } else {
                        type.items + LedgerItem(
                            id = newId(),
                            weight = weight,
                            price = price,
                            manualAmount = manualAmount
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
        val key = if (tab == LedgerTab.BUY) {
            if (data.settings.buyTypes.contains(name)) return
            data.settings.buyTypes + name
        } else {
            if (data.settings.sellTypes.contains(name)) return
            data.settings.sellTypes + name
        }
        data = data.copy(
            settings = if (tab == LedgerTab.BUY) {
                data.settings.copy(buyTypes = key)
            } else {
                data.settings.copy(sellTypes = key)
            }
        )
        persist()
    }

    fun addSetting(kind: SettingKind, name: String) {
        val clean = name.trim()
        if (clean.isBlank()) {
            showToast("请输入名称")
            return
        }
        val list = settingsList(kind)
        if (list.contains(clean)) {
            showToast("已存在")
            return
        }
        updateSettingsList(kind, list + clean)
        showToast("已添加")
    }

    fun deleteSetting(kind: SettingKind, name: String) {
        val list = settingsList(kind)
        updateSettingsList(kind, list.filterNot { it == name })
        showToast("已删除")
    }

    fun settingsList(kind: SettingKind): List<String> = when (kind) {
        SettingKind.BUY_TYPES -> data.settings.buyTypes
        SettingKind.SELL_TYPES -> data.settings.sellTypes
        SettingKind.BUYERS -> data.settings.buyers
    }

    private fun updateSettingsList(kind: SettingKind, list: List<String>) {
        val next = when (kind) {
            SettingKind.BUY_TYPES -> data.settings.copy(buyTypes = list)
            SettingKind.SELL_TYPES -> data.settings.copy(sellTypes = list)
            SettingKind.BUYERS -> data.settings.copy(buyers = list)
        }
        data = data.copy(settings = next)
        persist()
    }

    fun clearAllData() {
        openSheet(
            Sheet.Confirm(
                title = "清空所有记账数据",
                message = "类型和收购方设置会保留，此操作不可撤销。",
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
    }
}
