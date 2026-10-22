package com.peanut.ledger.ui

import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peanut.ledger.data.ChartData
import com.peanut.ledger.data.CreditStats
import com.peanut.ledger.data.PartyStat
import com.peanut.ledger.data.StatsIndex
import com.peanut.ledger.data.StatsPeriod
import com.peanut.ledger.data.StatsRange
import com.peanut.ledger.data.StatsSummary
import com.peanut.ledger.data.fmtMoney
import com.peanut.ledger.data.fmtNum
import com.peanut.ledger.data.fmtSigned
import com.peanut.ledger.data.formatCompactAxis
import com.peanut.ledger.ui.theme.AvatarSell
import com.peanut.ledger.ui.theme.Bg
import com.peanut.ledger.ui.theme.CardBorder
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.GoldBg
import com.peanut.ledger.ui.theme.Green
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.MutedLight
import com.peanut.ledger.ui.theme.Placeholder
import com.peanut.ledger.ui.theme.Primary
import com.peanut.ledger.ui.theme.Red
import com.peanut.ledger.ui.theme.SurfaceSoft
import com.peanut.ledger.ui.theme.TextSoft
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(state: LedgerState) {
    BackHandler { state.closeStats() }
    val index = remember(state.dataRevision) { StatsIndex(state.data) }
    var period by remember { mutableStateOf(StatsPeriod.WEEK) }
    var customStart by remember { mutableStateOf(LocalDate.now().minusDays(6)) }
    var customEnd by remember { mutableStateOf(LocalDate.now()) }
    var weightFilter by remember { mutableStateOf<String?>(null) }
    var priceFilter by remember { mutableStateOf<String?>(null) }
    val range = remember(period, customStart, customEnd) {
        StatsRange.of(period, customStart, customEnd)
    }
    val summary = remember(index, range) { index.summary(range) }
    val credit = remember(index, range) { index.credit(range) }
    val ranking = remember(index, range) { index.salesRanking(range) }
    val charts = remember(index, range, weightFilter, priceFilter) {
        index.charts(range, weightFilter, priceFilter)
    }
    val weightLabel = weightFilter ?: "全部类型"
    val priceLabel = priceFilter ?: "全部类型"

    Column(Modifier.fillMaxSize().background(Bg)) {
        StatsHeader(state)
        if (state.privacyMode) {
            PrivacyGate(state)
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PeriodCard(
                    period = period,
                    range = range,
                    customStart = customStart,
                    customEnd = customEnd,
                    onPeriod = { period = it },
                    onCustomStart = {
                        customStart = it
                        if (customEnd.isBefore(it)) customEnd = it
                    },
                    onCustomEnd = {
                        customEnd = it
                        if (customStart.isAfter(it)) customStart = it
                    }
                )
            }
            item { OverviewCard(summary, credit, range) }
            item { CreditCard(credit, range) }
            item {
                StatsChartCard(
                    title = "收支趋势",
                    subtitle = "金额（元）· 采购支出 / 销售收入 / 净收支",
                    data = charts.income,
                    allowNegative = true,
                    startAtZero = true,
                    tooltipFormatter = { value ->
                        if (value < 0) fmtSigned(value) else fmtMoney(value)
                    }
                )
            }
            item {
                StatsChartCard(
                    title = "采购 / 销售斤数",
                    subtitle = "单位：斤 · $weightLabel",
                    data = charts.weight,
                    allowNegative = false,
                    startAtZero = true,
                    typeOptions = index.typeNames,
                    selectedType = weightFilter,
                    onTypeSelect = { weightFilter = it },
                    tooltipFormatter = { "${fmtNum(it)} 斤" }
                )
            }
            item {
                StatsChartCard(
                    title = "采购 / 销售均价",
                    subtitle = "单位：元/斤 · $priceLabel · 成交金额 ÷ 重量",
                    data = charts.price,
                    allowNegative = false,
                    startAtZero = false,
                    typeOptions = index.typeNames,
                    selectedType = priceFilter,
                    onTypeSelect = { priceFilter = it },
                    tooltipFormatter = { "¥${fmtNum(it)}/斤" }
                )
            }
            item { PartyRankCard(ranking, range) }
            item {
                Text(
                    text = "统计基于本机记录；金额按实际成交金额计算。",
                    color = Placeholder,
                    fontSize = 10.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PrivacyGate(state: LedgerState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        LedgerCard(contentPadding = PaddingValues(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = AppIcon.EyeOff,
                    tint = Red,
                    background = GoldBg,
                    size = 38.dp,
                    iconSize = 18.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "统计需要显示完整金额",
                        color = TextSoft,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "隐私模式开启时无法查看统计，关闭后可继续查看收支、赊账和趋势。",
                        color = Muted,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = "关闭隐私模式并查看统计",
                onClick = { state.togglePrivacy() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatsHeader(state: LedgerState) {
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
                onClick = { state.closeStats() },
                tint = CardWhite,
                background = CardWhite.copy(alpha = 0.14f)
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("统计", color = CardWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "收支 · 赊账 · 斤数 · 均价",
                    color = CardWhite.copy(alpha = 0.7f),
                    fontSize = 11.5.sp
                )
            }
            Spacer(Modifier.width(40.dp))
        }
    }
}

@Composable
private fun PeriodCard(
    period: StatsPeriod,
    range: StatsRange,
    customStart: LocalDate,
    customEnd: LocalDate,
    onPeriod: (StatsPeriod) -> Unit,
    onCustomStart: (LocalDate) -> Unit,
    onCustomEnd: (LocalDate) -> Unit
) {
    LedgerCard(contentPadding = PaddingValues(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("统计周期", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(range.label, color = Muted, fontSize = 11.5.sp)
            }
            Text(
                text = formatDate(range.start) + " – " + formatDate(range.endInclusive),
                color = Placeholder,
                fontSize = 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatsPeriod.entries.forEach { item ->
                val selected = item == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Primary else SurfaceSoft)
                        .border(
                            1.dp,
                            if (selected) Primary else Line,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onPeriod(item) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.label,
                        color = if (selected) CardWhite else TextSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
        if (period == StatsPeriod.CUSTOM) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateField(
                    label = "开始日期",
                    date = customStart,
                    modifier = Modifier.weight(1f),
                    onPick = onCustomStart
                )
                DateField(
                    label = "结束日期",
                    date = customEnd,
                    modifier = Modifier.weight(1f),
                    onPick = onCustomEnd
                )
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    date: LocalDate,
    modifier: Modifier = Modifier,
    onPick: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceSoft)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .clickable {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onPick(LocalDate.of(year, month + 1, day))
                    },
                    date.year,
                    date.monthValue - 1,
                    date.dayOfMonth
                ).show()
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = Muted, fontSize = 9.5.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = formatDate(date),
            color = TextSoft,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("yyyy/M/d"))

@Composable
private fun OverviewCard(summary: StatsSummary, credit: CreditStats, range: StatsRange) {
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Text("概览", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text("当前统计周期 · ${range.label}", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        MetricRow(
            Metric("采购支出", fmtMoney(summary.buyAmount), Primary),
            Metric("销售收入", fmtMoney(summary.sellAmount), AvatarSell)
        )
        Spacer(Modifier.height(11.dp))
        MetricRow(
            Metric(
                "净收支",
                fmtSigned(summary.profit),
                if (summary.profit >= 0) Primary else Green
            ),
            Metric(
                "赊账余额",
                fmtMoney(credit.outstanding),
                if (credit.count > 0) Red else Primary
            )
        )
        Spacer(Modifier.height(11.dp))
        MetricRow(
            Metric("采购斤数", "${fmtNum(summary.buyWeight)} 斤", Primary),
            Metric("销售斤数", "${fmtNum(summary.sellWeight)} 斤", AvatarSell)
        )
    }
}

private data class Metric(val label: String, val value: String, val color: Color)

@Composable
private fun MetricRow(left: Metric, right: Metric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCell(left, Modifier.weight(1f))
        MetricCell(right, Modifier.weight(1f))
    }
}

@Composable
private fun MetricCell(metric: Metric, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(metric.label, color = Muted, fontSize = 10.5.sp, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            text = metric.value,
            color = metric.color,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CreditCard(credit: CreditStats, range: StatsRange) {
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("赊账统计", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            CounterTag("${credit.count} 笔")
        }
        Spacer(Modifier.height(3.dp))
        Text("当前周期未结清销售 · ${range.label}", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("未结清总额", color = Muted, fontSize = 10.5.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = fmtMoney(credit.outstanding),
                    color = if (credit.count > 0) Red else Primary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("占周期销售", color = Muted, fontSize = 10.5.sp)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = String.format(Locale.US, "%.1f%%", credit.ratio * 100),
                    color = TextSoft,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (credit.count > 0) {
                "共 ${credit.count} 笔 · 最长账龄 ${credit.oldestDays} 天 · 周期销售 ${fmtMoney(credit.salesTotal)}"
            } else {
                "当前周期没有未结清销售"
            },
            color = Muted,
            fontSize = 10.5.sp,
            lineHeight = 16.sp
        )
        if (credit.topDebtors.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("欠款排行", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(5.dp))
            credit.topDebtors.forEachIndexed { index, debtor ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        color = MutedLight,
                        fontSize = 10.sp,
                        modifier = Modifier.width(16.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = debtor.name,
                            color = TextSoft,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = "最长账龄 ${debtor.oldestDays} 天 · ${debtor.count} 笔",
                            color = Muted,
                            fontSize = 9.5.sp,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = fmtMoney(debtor.amount),
                        color = Red,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsChartCard(
    title: String,
    subtitle: String,
    data: ChartData,
    allowNegative: Boolean,
    startAtZero: Boolean,
    tooltipFormatter: (Double) -> String,
    typeOptions: List<String>? = null,
    selectedType: String? = null,
    onTypeSelect: ((String?) -> Unit)? = null
) {
    val series = data.series.map { item ->
        LineSeriesUi(
            name = item.name,
            color = chartColor(item.name),
            values = item.values
        )
    }
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Text(title, color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, color = Muted, fontSize = 10.5.sp, lineHeight = 15.sp)
        if (typeOptions != null && onTypeSelect != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("类型筛选", color = Muted, fontSize = 10.5.sp)
                Spacer(Modifier.weight(1f))
                CompactTypeDropdown(
                    options = typeOptions,
                    selected = selectedType,
                    onSelect = onTypeSelect
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            series.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(item.color)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(item.name, color = Muted, fontSize = 10.5.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LineChart(
            labels = data.labels,
            series = series,
            allowNegative = allowNegative,
            startAtZero = startAtZero,
            axisFormatter = { formatCompactAxis(it) },
            tooltipFormatter = tooltipFormatter
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "长按图表查看详细数值",
            color = Placeholder,
            fontSize = 9.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CompactTypeDropdown(
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .widthIn(max = 170.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceSoft)
                .border(1.dp, Line, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected ?: "全部类型",
                color = TextSoft,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(4.dp))
            AppIconView(
                icon = AppIcon.ChevronDown,
                tint = Muted,
                modifier = Modifier.size(10.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(220.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = CardWhite,
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                Modifier
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                TypeFilterItem(
                    text = "全部类型",
                    selected = selected == null,
                    onClick = {
                        expanded = false
                        onSelect(null)
                    }
                )
                options.forEach { name ->
                    TypeFilterItem(
                        text = name,
                        selected = selected == name,
                        onClick = {
                            expanded = false
                            onSelect(name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeFilterItem(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = if (selected) Primary else TextSoft,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            AppIconView(AppIcon.Check, tint = Primary, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun PartyRankCard(ranking: List<PartyStat>, range: StatsRange) {
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Text("往来排行", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text("商品收购方 · ${range.label}", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        if (ranking.isEmpty()) {
            Text("当前周期暂无商品收购方", color = Placeholder, fontSize = 11.sp)
        } else {
            ranking.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        color = MutedLight,
                        fontSize = 10.sp,
                        modifier = Modifier.width(18.dp)
                    )
                    Text(
                        text = item.name,
                        color = TextSoft,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = fmtMoney(item.amount),
                        color = AvatarSell,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private val ChartBuy = Color(0xFF2F73B8)
private val ChartSell = Color(0xFFE07A2F)
private val ChartNet = Color(0xFF7B5CD6)

private fun chartColor(name: String): Color = when (name) {
    "采购支出", "采购斤数", "采购均价" -> ChartBuy
    "销售收入", "销售斤数", "销售均价" -> ChartSell
    "净收支" -> ChartNet
    else -> Muted
}
