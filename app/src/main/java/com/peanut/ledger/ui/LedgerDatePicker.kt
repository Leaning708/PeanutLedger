package com.peanut.ledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.peanut.ledger.ui.theme.AvatarBuy
import com.peanut.ledger.ui.theme.CardBorder
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.Placeholder
import com.peanut.ledger.ui.theme.Primary
import com.peanut.ledger.ui.theme.SurfaceSoft
import com.peanut.ledger.ui.theme.TextMain
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

@Composable
fun LedgerDatePickerDialog(
    title: String,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selected by remember { mutableStateOf(initialDate) }
    val today = LocalDate.now()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp)
                .shadow(24.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = CardWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TopGradient)
                        .padding(horizontal = 14.dp, vertical = 15.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = title,
                            color = CardWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MonthNavButton(AppIcon.ChevronLeft, "上个月") {
                                month = month.minusMonths(1)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "${month.year}年${month.monthValue}月",
                                color = CardWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(12.dp))
                            MonthNavButton(AppIcon.ChevronRight, "下个月") {
                                month = month.plusMonths(1)
                            }
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    WeekHeaderRow()
                    Spacer(Modifier.height(4.dp))
                    val firstDay = month.atDay(1)
                    val leading = firstDay.dayOfWeek.value % 7
                    val daysInMonth = month.lengthOfMonth()
                    val rows = ceil((leading + daysInMonth) / 7.0).toInt()
                    for (row in 0 until rows) {
                        Row(Modifier.fillMaxWidth()) {
                            for (column in 0 until 7) {
                                val index = row * 7 + column
                                val day = index - leading + 1
                                Box(
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day in 1..daysInMonth) {
                                        val date = month.atDay(day)
                                        val isSelected = date == selected
                                        val isToday = date == today
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) Primary else Color.Transparent)
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Primary else if (isToday) Primary.copy(alpha = 0.35f) else Color.Transparent,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable { selected = date },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day.toString(),
                                                color = if (isSelected) CardWhite else if (isToday) Primary else TextMain,
                                                fontSize = 13.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "已选 ${selected.year}/${selected.monthValue}/${selected.dayOfMonth}",
                        color = Muted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GhostButton("取消", { onDismiss() }, Modifier.weight(1f))
                        PrimaryButton("确定", { onConfirm(selected) }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthNavButton(icon: AppIcon, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(CardWhite.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppIconView(icon = icon, tint = CardWhite, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun WeekHeaderRow() {
    Row(Modifier.fillMaxWidth()) {
        listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
            Text(
                text = day,
                color = Muted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
