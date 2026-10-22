package com.peanut.ledger.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.round

enum class LedgerTab(val label: String) {
    BUY("采购"),
    SELL("销售");

    companion object {
        fun of(ordinal: Int): LedgerTab = entries.first { it.ordinal == ordinal }
    }
}

data class LedgerItem(
    val id: String = newId(),
    val weight: Double = 0.0,
    val price: Double = 0.0,
    val manualAmount: Double? = null
)

data class LedgerType(
    val id: String = newId(),
    val name: String = "",
    val items: List<LedgerItem> = emptyList()
)

data class LedgerEntry(
    val id: String = newId(),
    val name: String = "",
    val buyer: String = "",
    val remark: String = "",
    val settled: Boolean = false,
    val adjust: Double = 0.0,
    val finalOverride: Double? = null,
    val createdAt: Long = 0L,
    val types: List<LedgerType> = emptyList()
) {
    fun title(tab: LedgerTab): String = if (tab == LedgerTab.BUY) name else buyer
}

data class DayData(
    val buys: List<LedgerEntry> = emptyList(),
    val sells: List<LedgerEntry> = emptyList(),
    val remark: String = ""
) {
    fun entries(tab: LedgerTab): List<LedgerEntry> = if (tab == LedgerTab.BUY) buys else sells
    fun isEmpty(): Boolean = buys.isEmpty() && sells.isEmpty() && remark.isBlank()
}

data class LedgerSettings(
    val buyTypes: List<String> = listOf("花生米", "花生果", "油料米", "白沙米", "海花米", "种子米"),
    val sellTypes: List<String> = listOf("商品米", "商品果", "油料米", "下料米", "花生米"),
    val buyers: List<String> = listOf("鲁南油厂"),
    val privacyMode: Boolean = false
)

data class LedgerData(
    val days: Map<String, DayData> = emptyMap(),
    val settings: LedgerSettings = LedgerSettings()
) {
    fun day(date: LocalDate): DayData = days[date.toString()] ?: DayData()
}

data class DayStats(val buy: Double, val sell: Double) {
    val profit: Double get() = round2(sell - buy)
}

data class RoundSuggestion(val value: Double, val label: String)

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(16)

fun round2(value: Double): Double {
    if (value.isNaN() || value.isInfinite()) return 0.0
    return round(value * 100.0) / 100.0
}

fun itemAmount(item: LedgerItem): Double =
    round2(item.manualAmount ?: (item.weight * item.price))

fun typeSubtotal(type: LedgerType): Double =
    round2(type.items.sumOf { itemAmount(it) })

fun entryTotal(entry: LedgerEntry): Double =
    round2(entry.types.sumOf { typeSubtotal(it) })

fun entryFinal(entry: LedgerEntry): Double {
    entry.finalOverride?.let { return round2(it) }
    return round2(entryTotal(entry) + entry.adjust)
}

fun entryHasUnsettled(entry: LedgerEntry): Boolean = !entry.settled

fun nextFarmerName(entries: List<LedgerEntry>): String {
    val used = entries.map { it.name }.toSet()
    var index = 1
    while (used.contains("户$index")) index++
    return "户$index"
}

fun findLastPriceInEntry(entry: LedgerEntry, typeId: String): Double? {
    val type = entry.types.firstOrNull { it.id == typeId } ?: return null
    return type.items.lastOrNull()?.price
}

fun nextRoundUp(value: Double): RoundSuggestion? {
    if (!(value > 0)) return null
    val eps = 0.005
    if (abs(value - round(value)) > eps) {
        return RoundSuggestion(ceil(value), "取整小数")
    }
    val iv = round(value).toLong()
    if (Regex("^[1-9]0*$").matches(iv.toString())) return null
    return when {
        iv % 10L != 0L -> RoundSuggestion((ceil(iv / 10.0) * 10).toLong().toDouble(), "取整个位")
        iv >= 10 && (iv / 10L) % 10L != 0L ->
            RoundSuggestion((ceil(iv / 100.0) * 100).toLong().toDouble(), "取整十位")
        iv >= 100 && (iv / 100L) % 10L != 0L ->
            RoundSuggestion((ceil(iv / 1000.0) * 1000).toLong().toDouble(), "取整百位")
        iv >= 1000 && (iv / 1000L) % 10L != 0L ->
            RoundSuggestion((ceil(iv / 10000.0) * 10000).toLong().toDouble(), "取整千位")
        iv >= 10000 && (iv / 10000L) % 10L != 0L ->
            RoundSuggestion((ceil(iv / 100000.0) * 100000).toLong().toDouble(), "取整万位")
        else -> null
    }
}

fun fmtMoney(value: Double): String = "¥" + String.format(Locale.US, "%.2f", round2(value))

fun fmtSigned(value: Double): String {
    val v = round2(value)
    return when {
        v > 0 -> "+" + fmtMoney(v)
        v < 0 -> "-¥" + String.format(Locale.US, "%.2f", abs(v))
        else -> "¥0.00"
    }
}

fun fmtNum(value: Double): String {
    val rounded = round2(value)
    if (abs(rounded - round(rounded)) < 0.0001) return round(rounded).toLong().toString()
    return java.math.BigDecimal.valueOf(rounded).stripTrailingZeros().toPlainString()
}

fun fmtProfitShort(value: Double): String {
    if (abs(value) < 0.005) return ""
    val absValue = abs(value)
    val body = when {
        absValue >= 10000 -> trimZero(absValue / 10000) + "万"
        absValue >= 1000 -> trimZero(absValue / 1000) + "千"
        else -> round(absValue).toLong().toString()
    }
    return (if (value > 0) "+" else "-") + body
}

fun fmtProfitMoney(value: Double): String {
    if (abs(value) < 0.005) return "¥0.00"
    return (if (value > 0) "+¥" else "-¥") + String.format(Locale.US, "%.2f", abs(value))
}

private fun trimZero(value: Double): String =
    String.format(Locale.US, "%.1f", value).removeSuffix(".0")

fun dateLabel(date: LocalDate): String =
    "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

fun weekLabel(date: LocalDate): String {
    val names = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    return names[date.dayOfWeek.value - 1]
}

fun weekdayShort(date: LocalDate): String =
    when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "一"
        DayOfWeek.TUESDAY -> "二"
        DayOfWeek.WEDNESDAY -> "三"
        DayOfWeek.THURSDAY -> "四"
        DayOfWeek.FRIDAY -> "五"
        DayOfWeek.SATURDAY -> "六"
        DayOfWeek.SUNDAY -> "日"
    }

fun fmtHM(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val time = java.time.Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
    return String.format(Locale.US, "%02d:%02d", time.hour, time.minute)
}

fun timestampFor(date: LocalDate, hour: Int, minute: Int): Long =
    date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
