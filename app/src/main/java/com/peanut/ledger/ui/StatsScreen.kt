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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peanut.ledger.data.AgingBucket
import com.peanut.ledger.data.ChartData
import com.peanut.ledger.data.CreditStats
import com.peanut.ledger.data.PartyStat
import com.peanut.ledger.data.StatsIndex
import com.peanut.ledger.data.StatsPeriod
import com.peanut.ledger.data.StatsSummary
import com.peanut.ledger.data.fmtMoney
import com.peanut.ledger.data.fmtNum
import com.peanut.ledger.data.fmtSigned
import com.peanut.ledger.data.formatCompactAxis
import com.peanut.ledger.ui.theme.AvatarSell
import com.peanut.ledger.ui.theme.Bg
import com.peanut.ledger.ui.theme.Brown
import com.peanut.ledger.ui.theme.CardBorder
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.GoldBg
import com.peanut.ledger.ui.theme.Green
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.LineStrong
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.MutedLight
import com.peanut.ledger.ui.theme.Placeholder
import com.peanut.ledger.ui.theme.Primary
import com.peanut.ledger.ui.theme.PrimaryDark
import com.peanut.ledger.ui.theme.Red
import com.peanut.ledger.ui.theme.SurfaceSoft
import com.peanut.ledger.ui.theme.TextMain
import com.peanut.ledger.ui.theme.TextSoft
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatsScreen(state: LedgerState) {
    BackHandler { state.closeStats() }
    val index = remember(state.dataRevision) { StatsIndex(state.data) }
    var period by remember { mutableStateOf(StatsPeriod.MONTH) }
    var typeFilter by remember { mutableStateOf<String?>(null) }
    val charts = remember(index, period, typeFilter) { index.charts(period, typeFilter) }
    val typeLabel = typeFilter ?: "全部类型"

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
            item { PeriodCard(period) { period = it } }
            item { OverviewCard(index.summary, index.credit) }
            item { CreditCard(index.credit) }
            item {
                StatsChartCard(
                    title = "收支趋势",
                    subtitle = "金额（元）· 采购支出 / 销售收入 / 净收支",
                    period = period,
                    data = charts.income,
                    allowNegative = true,
                    startAtZero = true
                )
            }
            item {
                TypeFilterCard(index.typeNames, typeFilter) { typeFilter = it }
            }
            item {
                StatsChartCard(
                    title = "采购 / 销售斤数",
                    subtitle = "单位：斤 · $typeLabel",
                    period = period,
                    data = charts.weight,
                    allowNegative = false,
                    startAtZero = true
                )
            }
            item {
                StatsChartCard(
                    title = "采购 / 销售均价",
                    subtitle = "单位：元/斤 · $typeLabel · 成交金额 ÷ 重量",
                    period = period,
                    data = charts.price,
                    allowNegative = false,
                    startAtZero = false
                )
            }
            item { PartyRankCard(index.topBuyers, index.topSellers) }
            item {
                Text(
                    text = "统计基于本机全部记录；手动金额按实际成交金额计算。",
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
private fun PeriodCard(period: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    LedgerCard(contentPadding = PaddingValues(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("统计周期", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(period.subtitle, color = Muted, fontSize = 11.5.sp)
            }
            CounterTag("本地数据")
        }
        Spacer(Modifier.height(11.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsPeriod.entries.forEach { item ->
                val selected = item == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (selected) Primary else SurfaceSoft)
                        .border(
                            1.dp,
                            if (selected) Primary else Line,
                            RoundedCornerShape(11.dp)
                        )
                        .clickable { onSelect(item) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.label,
                        color = if (selected) CardWhite else TextSoft,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(summary: StatsSummary, credit: CreditStats) {
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("累计概览", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            CounterTag("全部")
        }
        Spacer(Modifier.height(3.dp))
        Text("全部历史记录", color = Muted, fontSize = 11.sp)
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

private data class Metric(val label: String, val value: String, val color: Color)

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
private fun CreditCard(credit: CreditStats) {
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("赊账统计", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            CounterTag("${credit.count} 笔")
        }
        Spacer(Modifier.height(3.dp))
        Text("全部未结清销售", color = Muted, fontSize = 11.sp)
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
                Text("占累计销售", color = Muted, fontSize = 10.5.sp)
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
                "共 ${credit.count} 笔 · 最长账龄 ${credit.oldestDays} 天 · 累计销售 ${fmtMoney(credit.salesTotal)}"
            } else {
                "当前没有未结清销售"
            },
            color = Muted,
            fontSize = 10.5.sp,
            lineHeight = 16.sp
        )
        if (credit.count > 0) {
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                credit.aging.forEach { bucket ->
                    AgingRow(bucket, credit.outstanding)
                }
            }
            if (credit.topDebtors.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("欠款最多", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(5.dp))
                credit.topDebtors.forEachIndexed { index, debtor ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = MutedLight,
                            fontSize = 10.sp,
                            modifier = Modifier.width(16.dp)
                        )
                        Text(
                            text = debtor.name,
                            color = TextSoft,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
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
}

@Composable
private fun AgingRow(bucket: AgingBucket, outstanding: Double) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(bucket.label, color = TextSoft, fontSize = 11.sp, modifier = Modifier.width(64.dp))
            Text(
                text = "${bucket.count} 笔",
                color = Muted,
                fontSize = 10.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = fmtMoney(bucket.amount),
                color = if (bucket.amount > 0.005) Red else Placeholder,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Line)
        ) {
            val fraction = if (outstanding > 0.005) {
                (bucket.amount / outstanding).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(Red.copy(alpha = 0.7f))
                )
            }
        }
    }
}

@Composable
private fun StatsChartCard(
    title: String,
    subtitle: String,
    period: StatsPeriod,
    data: ChartData,
    allowNegative: Boolean,
    startAtZero: Boolean
) {
    val series = data.series.map { item ->
        LineSeriesUi(
            name = item.name,
            color = chartColor(item.name),
            values = item.values
        )
    }
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Muted, fontSize = 10.5.sp, lineHeight = 15.sp)
            }
            Spacer(Modifier.width(8.dp))
            CounterTag(period.subtitle)
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
            axisFormatter = { formatCompactAxis(it) }
        )
    }
}

private fun chartColor(name: String): Color = when (name) {
    "采购支出", "采购斤数", "采购均价" -> Primary
    "销售收入", "销售斤数", "销售均价" -> AvatarSell
    "净收支" -> Brown
    else -> Muted
}

@Composable
private fun TypeFilterCard(
    typeNames: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    LedgerCard(contentPadding = PaddingValues(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("类型筛选", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text("仅影响斤数与均价图", color = Muted, fontSize = 11.5.sp)
            }
            Spacer(Modifier.width(8.dp))
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(SurfaceSoft)
                        .border(1.dp, Line, RoundedCornerShape(9.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selected ?: "全部类型",
                        color = TextSoft,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(4.dp))
                    AppIconView(
                        icon = AppIcon.ChevronDown,
                        tint = Muted,
                        modifier = Modifier.size(11.dp)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(230.dp),
                    shape = RoundedCornerShape(14.dp),
                    containerColor = CardWhite,
                    tonalElevation = 0.dp,
                    shadowElevation = 10.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        Modifier
                            .heightIn(max = 320.dp)
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
                        typeNames.forEach { name ->
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
private fun PartyRankCard(buyers: List<PartyStat>, sellers: List<PartyStat>) {
    LedgerCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("往来排行", color = TextSoft, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            CounterTag("全部")
        }
        Spacer(Modifier.height(3.dp))
        Text("按累计金额", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        val stacked = LocalDensity.current.fontScale > 1.12f
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PartyColumn("采购农户", buyers, Primary, Modifier.fillMaxWidth())
                ChevronDivider()
                PartyColumn("销售收购方", sellers, AvatarSell, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PartyColumn("采购农户", buyers, Primary, Modifier.weight(1f))
                PartyColumn("销售收购方", sellers, AvatarSell, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PartyColumn(
    title: String,
    list: List<PartyStat>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(title, color = color, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        if (list.isEmpty()) {
            Text("暂无记录", color = Placeholder, fontSize = 10.5.sp)
        } else {
            list.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        color = MutedLight,
                        fontSize = 9.5.sp,
                        modifier = Modifier.width(13.dp)
                    )
                    Text(
                        text = item.name,
                        color = TextSoft,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = fmtMoney(item.amount),
                        color = Muted,
                        fontSize = 9.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
