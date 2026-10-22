package com.peanut.ledger.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

enum class StatsPeriod(val label: String, val subtitle: String) {
    DAY("日", "近30天"),
    MONTH("月", "近12个月"),
    YEAR("年", "近5年")
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
    val count: Int
)

data class AgingBucket(
    val label: String,
    val amount: Double,
    val count: Int
)

data class CreditStats(
    val outstanding: Double,
    val count: Int,
    val salesTotal: Double,
    val ratio: Double,
    val oldestDays: Long,
    val aging: List<AgingBucket>,
    val topDebtors: List<PartyStat>
)

data class StatsSummary(
    val buyAmount: Double,
    val sellAmount: Double,
    val profit: Double,
    val buyWeight: Double,
    val sellWeight: Double,
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
 * The persisted ledger is still a day map, but statistics repeatedly need to scan every
 * item with filters. Flattening once avoids walking the nested day/type/item tree for every
 * chart and makes future migration to a database-backed query layer straightforward.
 */
class StatsIndex(val data: LedgerData) {
    val records: List<StatRecord>
    val entries: List<StatEntry>
    val typeNames: List<String>
    val summary: StatsSummary
    val credit: CreditStats
    val topBuyers: List<PartyStat>
    val topSellers: List<PartyStat>

    private val chartCache = mutableMapOf<Triple<StatsPeriod, String?, LocalDate>, StatsCharts>()

    init {
        val recordBuilder = mutableListOf<StatRecord>()
        val entryBuilder = mutableListOf<StatEntry>()

        for ((dateKey, day) in data.days) {
            val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: continue
            for (tab in LedgerTab.entries) {
                for (entry in day.entries(tab)) {
                    val party = (if (tab == LedgerTab.BUY) entry.name else entry.buyer)
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

        val buyAmount = round2(entries.filter { it.tab == LedgerTab.BUY }.sumOf { it.amount })
        val sellAmount = round2(entries.filter { it.tab == LedgerTab.SELL }.sumOf { it.amount })
        summary = StatsSummary(
            buyAmount = buyAmount,
            sellAmount = sellAmount,
            profit = round2(sellAmount - buyAmount),
            buyWeight = round2(entries.filter { it.tab == LedgerTab.BUY }.sumOf { it.weight }),
            sellWeight = round2(entries.filter { it.tab == LedgerTab.SELL }.sumOf { it.weight }),
            entryCount = entries.size
        )

        val today = LocalDate.now()
        val sales = entries.filter { it.tab == LedgerTab.SELL }
        val outstandingEntries = sales.filter { !it.settled && it.amount > 0.005 }
        val outstanding = round2(outstandingEntries.sumOf { it.amount })
        val salesTotal = round2(sales.sumOf { it.amount })
        val aging = listOf(
            AgingBucket("0–7天", 0.0, 0),
            AgingBucket("8–30天", 0.0, 0),
            AgingBucket("31–90天", 0.0, 0),
            AgingBucket("90天以上", 0.0, 0)
        ).toMutableList()
        var oldest = 0L
        for (entry in outstandingEntries) {
            val days = ChronoUnit.DAYS.between(entry.date, today).coerceAtLeast(0L)
            if (days > oldest) oldest = days
            val index = when {
                days <= 7 -> 0
                days <= 30 -> 1
                days <= 90 -> 2
                else -> 3
            }
            aging[index] = aging[index].copy(
                amount = round2(aging[index].amount + entry.amount),
                count = aging[index].count + 1
            )
        }
        credit = CreditStats(
            outstanding = outstanding,
            count = outstandingEntries.size,
            salesTotal = salesTotal,
            ratio = if (salesTotal > 0.005) outstanding / salesTotal else 0.0,
            oldestDays = oldest,
            aging = aging,
            topDebtors = outstandingEntries
                .groupBy { it.party }
                .map { (name, list) ->
                    PartyStat(
                        name = name,
                        amount = round2(list.sumOf { it.amount }),
                        count = list.size
                    )
                }
                .filter { it.amount > 0.005 }
                .sortedByDescending { it.amount }
                .take(5)
        )

        topBuyers = partyRanking(LedgerTab.BUY)
        topSellers = partyRanking(LedgerTab.SELL)
    }

    fun charts(
        period: StatsPeriod,
        typeFilter: String?,
        today: LocalDate = LocalDate.now()
    ): StatsCharts {
        val key = Triple(period, typeFilter, today)
        return chartCache.getOrPut(key) { buildCharts(period, typeFilter, today) }
    }

    private fun partyRanking(tab: LedgerTab): List<PartyStat> =
        entries
            .filter { it.tab == tab && it.amount > 0.005 }
            .groupBy { it.party }
            .map { (name, list) ->
                PartyStat(
                    name = name,
                    amount = round2(list.sumOf { it.amount }),
                    count = list.size
                )
            }
            .sortedByDescending { it.amount }
            .take(3)

    private fun buildCharts(
        period: StatsPeriod,
        typeFilter: String?,
        today: LocalDate
    ): StatsCharts {
        val buckets = buildBuckets(period, today)
        val size = buckets.size
        val buyAmount = DoubleArray(size)
        val sellAmount = DoubleArray(size)
        val buyWeight = DoubleArray(size)
        val sellWeight = DoubleArray(size)
        val buyPriceAmount = DoubleArray(size)
        val sellPriceAmount = DoubleArray(size)
        val buyPriceWeight = DoubleArray(size)
        val sellPriceWeight = DoubleArray(size)

        for (entry in entries) {
            val index = bucketIndex(period, buckets, entry.date) ?: continue
            when (entry.tab) {
                LedgerTab.BUY -> buyAmount[index] += entry.amount
                LedgerTab.SELL -> sellAmount[index] += entry.amount
            }
        }

        for (record in records) {
            if (typeFilter != null && record.typeName != typeFilter) continue
            val index = bucketIndex(period, buckets, record.date) ?: continue
            when (record.tab) {
                LedgerTab.BUY -> {
                    buyWeight[index] += record.weight
                    buyPriceAmount[index] += record.amount
                    buyPriceWeight[index] += record.weight
                }
                LedgerTab.SELL -> {
                    sellWeight[index] += record.weight
                    sellPriceAmount[index] += record.amount
                    sellPriceWeight[index] += record.weight
                }
            }
        }

        val labels = buckets.map { it.label }
        val income = ChartData(
            labels = labels,
            series = listOf(
                ChartSeries("采购支出", buyAmount.map { round2(it) }),
                ChartSeries("销售收入", sellAmount.map { round2(it) }),
                ChartSeries("净收支", List(size) { round2(sellAmount[it] - buyAmount[it]) })
            )
        )
        val weight = ChartData(
            labels = labels,
            series = listOf(
                ChartSeries("采购斤数", buyWeight.map { round2(it) }),
                ChartSeries("销售斤数", sellWeight.map { round2(it) })
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

    private fun buildBuckets(period: StatsPeriod, today: LocalDate): List<StatBucket> =
        when (period) {
            StatsPeriod.DAY -> {
                val start = today.minusDays(29)
                val formatter = DateTimeFormatter.ofPattern("M/d")
                List(30) { offset ->
                    val date = start.plusDays(offset.toLong())
                    StatBucket(date, date.plusDays(1), date.format(formatter))
                }
            }
            StatsPeriod.MONTH -> {
                val startMonth = YearMonth.from(today).minusMonths(11)
                List(12) { offset ->
                    val month = startMonth.plusMonths(offset.toLong())
                    val start = month.atDay(1)
                    StatBucket(start, start.plusMonths(1), "${month.monthValue}月")
                }
            }
            StatsPeriod.YEAR -> {
                val startYear = today.year - 4
                List(5) { offset ->
                    val year = startYear + offset
                    val start = LocalDate.of(year, 1, 1)
                    StatBucket(start, start.plusYears(1), year.toString())
                }
            }
        }

    private fun bucketIndex(
        period: StatsPeriod,
        buckets: List<StatBucket>,
        date: LocalDate
    ): Int? {
        val index = when (period) {
            StatsPeriod.DAY -> ChronoUnit.DAYS.between(buckets.first().start, date).toInt()
            StatsPeriod.MONTH -> ChronoUnit.MONTHS.between(
                YearMonth.from(buckets.first().start),
                YearMonth.from(date)
            ).toInt()
            StatsPeriod.YEAR -> date.year - buckets.first().start.year
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
