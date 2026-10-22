package com.peanut.ledger.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

enum class StatsPeriod(val label: String) {
    WEEK("近7天"),
    DAY30("近30天"),
    YEAR("近1年"),
    CUSTOM("自定义")
}

enum class StatsGranularity { DAY, MONTH, YEAR }

data class StatsRange(
    val start: LocalDate,
    val endInclusive: LocalDate,
    val label: String,
    val granularity: StatsGranularity
) {
    fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(endInclusive)

    companion object {
        fun of(
            period: StatsPeriod,
            customStart: LocalDate,
            customEnd: LocalDate,
            today: LocalDate = LocalDate.now()
        ): StatsRange {
            val rawStart: LocalDate
            val rawEnd: LocalDate
            when (period) {
                StatsPeriod.WEEK -> {
                    rawStart = today.minusDays(6)
                    rawEnd = today
                }
                StatsPeriod.DAY30 -> {
                    rawStart = today.minusDays(29)
                    rawEnd = today
                }
                StatsPeriod.YEAR -> {
                    rawStart = YearMonth.from(today).minusMonths(11).atDay(1)
                    rawEnd = today
                }
                StatsPeriod.CUSTOM -> {
                    rawStart = customStart
                    rawEnd = customEnd
                }
            }
            val start = minOf(rawStart, rawEnd)
            val end = maxOf(rawStart, rawEnd)
            val days = ChronoUnit.DAYS.between(start, end)
            val granularity = when {
                period == StatsPeriod.YEAR -> StatsGranularity.MONTH
                days <= 31 -> StatsGranularity.DAY
                days <= 370 -> StatsGranularity.MONTH
                else -> StatsGranularity.YEAR
            }
            val label = when (period) {
                StatsPeriod.WEEK -> "近7天"
                StatsPeriod.DAY30 -> "近30天"
                StatsPeriod.YEAR -> "近1年"
                StatsPeriod.CUSTOM -> {
                    val pattern = if (start.year == end.year) "M/d" else "yy/M/d"
                    val formatter = DateTimeFormatter.ofPattern(pattern)
                    "${start.format(formatter)} – ${end.format(formatter)}"
                }
            }
            return StatsRange(start, end, label, granularity)
        }
    }
}

data class StatRecord(
    val date: LocalDate,
    val tab: LedgerTab,
    val entryId: String,
    val party: String,
    val settled: Boolean,
    val typeName: String,
    val weight: Double,
    val amount: Double,
    val createdAt: Long
)

data class StatEntry(
    val date: LocalDate,
    val tab: LedgerTab,
    val id: String,
    val party: String,
    val settled: Boolean,
    val amount: Double,
    val weight: Double,
    val createdAt: Long
)

data class PartyStat(
    val name: String,
    val amount: Double,
    val count: Int,
    val oldestDays: Long = 0L
)

data class CreditStats(
    val outstanding: Double,
    val count: Int,
    val salesTotal: Double,
    val ratio: Double,
    val oldestDays: Long,
    val topDebtors: List<PartyStat>
)

data class StatsSummary(
    val purchaseAmount: Double,
    val saleAmount: Double,
    val profit: Double,
    val purchaseWeight: Double,
    val saleWeight: Double,
    val entryCount: Int
)

data class ChartSeries(val name: String, val values: List<Double?>)

data class ChartData(val labels: List<String>, val series: List<ChartSeries>)

data class StatsCharts(
    val income: ChartData,
    val weight: ChartData,
    val price: ChartData
)

private data class StatBucket(
    val start: LocalDate,
    val endExclusive: LocalDate,
    val label: String
)

/**
 * Flat, immutable index used by the statistics screen.
 *
 * The persisted ledger is still a day map, but statistics repeatedly need to scan every item
 * with filters. Flattening once avoids walking the nested day/type/item tree for every chart.
 * Aggregations are cached by selected range and filter, so switching charts stays cheap.
 */
class StatsIndex(val data: LedgerData) {
    val records: List<StatRecord>
    val entries: List<StatEntry>
    val typeNames: List<String>

    private val summaryCache = mutableMapOf<StatsRange, StatsSummary>()
    private val creditCache = mutableMapOf<StatsRange, CreditStats>()
    private val rankingCache = mutableMapOf<StatsRange, List<PartyStat>>()
    private val chartCache = mutableMapOf<ChartKey, StatsCharts>()

    private data class ChartKey(
        val range: StatsRange,
        val weightFilter: String?,
        val priceFilter: String?
    )

    init {
        val recordBuilder = mutableListOf<StatRecord>()
        val entryBuilder = mutableListOf<StatEntry>()

        for ((dateKey, day) in data.days) {
            val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: continue
            for (tab in LedgerTab.entries) {
                for (entry in day.entries(tab)) {
                    val party = (if (tab == LedgerTab.PURCHASE) entry.supplierName else entry.buyerName)
                        .trim()
                        .ifBlank { "未命名" }
                    var entryWeight = 0.0
                    for (type in entry.types) {
                        val typeName = type.name.trim().ifBlank { "未命名类型" }
                        for (item in type.items) {
                            entryWeight += item.weight
                            recordBuilder += StatRecord(
                                date = date,
                                tab = tab,
                                entryId = entry.id,
                                party = party,
                                settled = entry.settled,
                                typeName = typeName,
                                weight = round2(item.weight),
                                amount = itemAmount(item),
                                createdAt = entry.createdAt
                            )
                        }
                    }
                    entryBuilder += StatEntry(
                        date = date,
                        tab = tab,
                        id = entry.id,
                        party = party,
                        settled = entry.settled,
                        amount = entryFinal(entry),
                        weight = round2(entryWeight),
                        createdAt = entry.createdAt
                    )
                }
            }
        }

        records = recordBuilder
        entries = entryBuilder
        typeNames = records.map { it.typeName }.distinct().sorted()
    }

    fun summary(range: StatsRange): StatsSummary = summaryCache.getOrPut(range) {
        val inRange = entries.filter { range.contains(it.date) }
        val purchaseAmount = round2(inRange.filter { it.tab == LedgerTab.PURCHASE }.sumOf { it.amount })
        val saleAmount = round2(inRange.filter { it.tab == LedgerTab.SALE }.sumOf { it.amount })
        StatsSummary(
            purchaseAmount = purchaseAmount,
            saleAmount = saleAmount,
            profit = round2(saleAmount - purchaseAmount),
            purchaseWeight = round2(inRange.filter { it.tab == LedgerTab.PURCHASE }.sumOf { it.weight }),
            saleWeight = round2(inRange.filter { it.tab == LedgerTab.SALE }.sumOf { it.weight }),
            entryCount = inRange.size
        )
    }

    fun credit(range: StatsRange): CreditStats = creditCache.getOrPut(range) {
        val today = LocalDate.now()
        val sales = entries.filter { it.tab == LedgerTab.SALE && range.contains(it.date) }
        val outstandingEntries = sales.filter { !it.settled && it.amount > 0.005 }
        val outstanding = round2(outstandingEntries.sumOf { it.amount })
        val salesTotal = round2(sales.sumOf { it.amount })
        val debtors = outstandingEntries
            .groupBy { it.party }
            .map { (name, list) ->
                PartyStat(
                    name = name,
                    amount = round2(list.sumOf { it.amount }),
                    count = list.size,
                    oldestDays = list.maxOf {
                        ChronoUnit.DAYS.between(it.date, today).coerceAtLeast(0L)
                    }
                )
            }
            .filter { it.amount > 0.005 }
            .sortedByDescending { it.amount }
            .take(5)
        CreditStats(
            outstanding = outstanding,
            count = outstandingEntries.size,
            salesTotal = salesTotal,
            ratio = if (salesTotal > 0.005) outstanding / salesTotal else 0.0,
            oldestDays = debtors.maxOfOrNull { it.oldestDays } ?: 0L,
            topDebtors = debtors
        )
    }

    fun salesRanking(range: StatsRange): List<PartyStat> = rankingCache.getOrPut(range) {
        entries
            .filter { it.tab == LedgerTab.SALE && range.contains(it.date) && it.amount > 0.005 }
            .groupBy { it.party }
            .map { (name, list) ->
                PartyStat(
                    name = name,
                    amount = round2(list.sumOf { it.amount }),
                    count = list.size
                )
            }
            .sortedByDescending { it.amount }
            .take(5)
    }

    fun charts(
        range: StatsRange,
        weightFilter: String?,
        priceFilter: String?
    ): StatsCharts = chartCache.getOrPut(ChartKey(range, weightFilter, priceFilter)) {
        buildCharts(range, weightFilter, priceFilter)
    }

    private fun buildCharts(
        range: StatsRange,
        weightFilter: String?,
        priceFilter: String?
    ): StatsCharts {
        val buckets = buildBuckets(range)
        val size = buckets.size
        val purchaseAmount = DoubleArray(size)
        val saleAmount = DoubleArray(size)
        val purchaseWeight = DoubleArray(size)
        val saleWeight = DoubleArray(size)
        val buyPriceAmount = DoubleArray(size)
        val sellPriceAmount = DoubleArray(size)
        val buyPriceWeight = DoubleArray(size)
        val sellPriceWeight = DoubleArray(size)

        for (entry in entries) {
            if (!range.contains(entry.date)) continue
            val index = bucketIndex(range.granularity, buckets, entry.date, range) ?: continue
            when (entry.tab) {
                LedgerTab.PURCHASE -> purchaseAmount[index] += entry.amount
                LedgerTab.SALE -> saleAmount[index] += entry.amount
            }
        }

        for (record in records) {
            if (!range.contains(record.date)) continue
            val index = bucketIndex(range.granularity, buckets, record.date, range) ?: continue
            val matchesWeight = weightFilter == null || record.typeName == weightFilter
            val matchesPrice = priceFilter == null || record.typeName == priceFilter
            if (!matchesWeight && !matchesPrice) continue
            when (record.tab) {
                LedgerTab.PURCHASE -> {
                    if (matchesWeight) purchaseWeight[index] += record.weight
                    if (matchesPrice) {
                        buyPriceAmount[index] += record.amount
                        buyPriceWeight[index] += record.weight
                    }
                }
                LedgerTab.SALE -> {
                    if (matchesWeight) saleWeight[index] += record.weight
                    if (matchesPrice) {
                        sellPriceAmount[index] += record.amount
                        sellPriceWeight[index] += record.weight
                    }
                }
            }
        }

        val labels = buckets.map { it.label }
        val income = ChartData(
            labels = labels,
            series = listOf(
                ChartSeries("采购支出", purchaseAmount.map { round2(it) }),
                ChartSeries("销售收入", saleAmount.map { round2(it) }),
                ChartSeries("净收支", List(size) { round2(saleAmount[it] - purchaseAmount[it]) })
            )
        )
        val weight = ChartData(
            labels = labels,
            series = listOf(
                ChartSeries("采购斤数", purchaseWeight.map { round2(it) }),
                ChartSeries("销售斤数", saleWeight.map { round2(it) })
            )
        )
        val price = ChartData(
            labels = labels,
            series = listOf(
                ChartSeries(
                    "采购均价",
                    List(size) {
                        if (buyPriceWeight[it] > 0.0001) {
                            round2(buyPriceAmount[it] / buyPriceWeight[it])
                        } else {
                            null
                        }
                    }
                ),
                ChartSeries(
                    "销售均价",
                    List(size) {
                        if (sellPriceWeight[it] > 0.0001) {
                            round2(sellPriceAmount[it] / sellPriceWeight[it])
                        } else {
                            null
                        }
                    }
                )
            )
        )
        return StatsCharts(income, weight, price)
    }

    private fun buildBuckets(range: StatsRange): List<StatBucket> =
        when (range.granularity) {
            StatsGranularity.DAY -> {
                val formatter = DateTimeFormatter.ofPattern("M/d")
                val cells = mutableListOf<StatBucket>()
                var date = range.start
                while (!date.isAfter(range.endInclusive)) {
                    cells += StatBucket(date, date.plusDays(1), date.format(formatter))
                    date = date.plusDays(1)
                }
                cells
            }
            StatsGranularity.MONTH -> {
                val startMonth = YearMonth.from(range.start)
                val endMonth = YearMonth.from(range.endInclusive)
                val formatter = if (startMonth.year == endMonth.year) {
                    DateTimeFormatter.ofPattern("M月")
                } else {
                    DateTimeFormatter.ofPattern("yy/M")
                }
                val cells = mutableListOf<StatBucket>()
                var month = startMonth
                while (!month.isAfter(endMonth)) {
                    val start = month.atDay(1)
                    cells += StatBucket(start, start.plusMonths(1), start.format(formatter))
                    month = month.plusMonths(1)
                }
                cells
            }
            StatsGranularity.YEAR -> {
                val cells = mutableListOf<StatBucket>()
                var year = range.start.year
                while (year <= range.endInclusive.year) {
                    val start = LocalDate.of(year, 1, 1)
                    cells += StatBucket(start, start.plusYears(1), year.toString())
                    year++
                }
                cells
            }
        }

    private fun bucketIndex(
        granularity: StatsGranularity,
        buckets: List<StatBucket>,
        date: LocalDate,
        range: StatsRange
    ): Int? {
        val index = when (granularity) {
            StatsGranularity.DAY -> ChronoUnit.DAYS.between(range.start, date).toInt()
            StatsGranularity.MONTH -> ChronoUnit.MONTHS.between(
                YearMonth.from(range.start),
                YearMonth.from(date)
            ).toInt()
            StatsGranularity.YEAR -> date.year - range.start.year
        }
        return index.takeIf { it in buckets.indices }
    }
}

fun formatCompactAxis(value: Double): String {
    val absolute = abs(value)
    return when {
        absolute >= 10_000 -> "${trimZeros(value / 10_000)}万"
        absolute >= 1_000 -> trimZeros(value / 1_000) + "千"
        else -> trimZeros(value)
    }
}

private fun trimZeros(value: Double): String {
    val rounded = round2(value)
    return if (abs(rounded - kotlin.math.round(rounded)) < 0.0001) {
        kotlin.math.round(rounded).toLong().toString()
    } else {
        java.math.BigDecimal.valueOf(rounded).stripTrailingZeros().toPlainString()
    }
}
