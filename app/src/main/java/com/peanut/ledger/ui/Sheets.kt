package com.peanut.ledger.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.peanut.ledger.data.EntryLocation
import com.peanut.ledger.data.LedgerTab
import com.peanut.ledger.data.entryFinal
import com.peanut.ledger.data.entryTotal
import com.peanut.ledger.data.fmtMoney
import com.peanut.ledger.data.fmtNum
import com.peanut.ledger.data.itemAmount
import com.peanut.ledger.data.nextRoundUp
import com.peanut.ledger.data.round2
import com.peanut.ledger.ui.theme.CardBorder
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.FieldBg
import com.peanut.ledger.ui.theme.FieldBorder
import com.peanut.ledger.ui.theme.GoldBg
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.LineStrong
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.Placeholder
import com.peanut.ledger.ui.theme.Primary
import com.peanut.ledger.ui.theme.PrimaryDark
import com.peanut.ledger.ui.theme.Red
import com.peanut.ledger.ui.theme.RedBg
import com.peanut.ledger.ui.theme.SurfaceSoft
import com.peanut.ledger.ui.theme.TextMain
import com.peanut.ledger.ui.theme.TextSoft
import com.peanut.ledger.location.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

@Composable
fun SheetHost(state: LedgerState) {
    val sheet = state.sheet ?: return
    BackHandler { state.closeSheet() }
    BoxWithConstraints(Modifier.fillMaxSize().zIndex(55f)) {
        val maxSheetHeight = maxHeight * 0.9f
        var appeared by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { appeared = true }
        val scrimAlpha by animateFloatAsState(
            targetValue = if (appeared) 0.4f else 0f,
            animationSpec = tween(240),
            label = "sheetScrim"
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .pointerInput(Unit) {
                    detectTapGestures { state.closeSheet() }
                }
        )
        AnimatedVisibility(
            visible = appeared,
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(60f),
            enter = slideInVertically(animationSpec = tween(280)) { it } + fadeIn(tween(180))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .imePadding(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = CardWhite,
                shadowElevation = 24.dp
            ) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp, bottom = 22.dp)
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SheetHandle()
                    }
                    Spacer(Modifier.height(16.dp))
                    SheetContent(state, sheet)
                }
            }
        }
    }
}

@Composable
private fun SheetContent(state: LedgerState, sheet: Sheet) {
    when (sheet) {
        Sheet.More -> MoreSheet(state)
        Sheet.Calendar -> CalendarSheet(state)
        Sheet.DayRemark -> DayRemarkSheet(state)
        Sheet.AddEntry -> AddEntrySheet(state)
        is Sheet.AddType -> AddTypeSheet(state, sheet.entryId)
        is Sheet.ItemForm -> ItemFormSheet(state, sheet)
        is Sheet.EntryMenu -> EntryMenuSheet(state, sheet.entryId)
        is Sheet.RenameEntry -> RenameEntrySheet(state, sheet.entryId)
        is Sheet.EntryRemark -> EntryRemarkSheet(state, sheet.entryId)
        is Sheet.FinalAmount -> FinalAmountSheet(state, sheet.entryId)
        is Sheet.SettingActions -> SettingActionsSheet(state, sheet.kind, sheet.name)
        is Sheet.RenameSetting -> RenameSettingSheet(state, sheet.kind, sheet.oldName)
        is Sheet.Confirm -> ConfirmSheet(state, sheet)
    }
}

@Composable
private fun SheetActionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

@Composable
private fun MoreSheet(state: LedgerState) {
    val hasRemark = state.currentDay.remark.isNotBlank()
    val remarkLabel = if (state.currentDate == LocalDate.now()) "今日备注" else "当日备注"
    Column {
        SheetTitle("更多", "备注、隐私与偏好设置")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetActionItem(
                text = if (hasRemark) "编辑$remarkLabel" else "添加$remarkLabel",
                icon = AppIcon.Note,
                onClick = { state.openSheet(Sheet.DayRemark) }
            )
            SheetActionItem(
                text = if (state.privacyMode) "关闭隐私模式" else "开启隐私模式",
                icon = if (state.privacyMode) AppIcon.EyeOff else AppIcon.Eye,
                onClick = { state.togglePrivacy() }
            )
            SheetActionItem(
                text = "设置",
                icon = AppIcon.Settings,
                onClick = { state.openSettings() }
            )
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceSoft)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "隐私模式下，双击或长按卡片可临时显示金额",
                    color = Muted,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "数据仅保存在本机，无需登录",
                    color = Placeholder,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        GhostButton("关闭", { state.closeSheet() }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun AddEntrySheet(state: LedgerState) {
    if (state.tab == LedgerTab.BUY) {
        AddFarmerSheet(state)
    } else {
        AddBuyerEntrySheet(state)
    }
}

private enum class LocationStatus { REQUESTING, LOCATING, FOUND, DENIED, UNAVAILABLE }

@Composable
private fun AddFarmerSheet(state: LedgerState) {
    var name by remember { mutableStateOf(state.nextFarmerName()) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locationStatus by remember {
        mutableStateOf(
            if (LocationHelper.hasPermission(context)) LocationStatus.LOCATING
            else LocationStatus.REQUESTING
        )
    }
    var entryLocation by remember { mutableStateOf<EntryLocation?>(null) }

    fun fetchLocation() {
        locationStatus = LocationStatus.LOCATING
        scope.launch {
            val result = LocationHelper.currentLocation(context)
            entryLocation = result
            locationStatus = if (result != null) LocationStatus.FOUND else LocationStatus.UNAVAILABLE
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it } || LocationHelper.hasPermission(context)) {
            fetchLocation()
        } else {
            locationStatus = LocationStatus.DENIED
        }
    }

    fun retryLocation() {
        if (LocationHelper.hasPermission(context)) {
            fetchLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
        if (LocationHelper.hasPermission(context)) {
            fetchLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val statusText = when (locationStatus) {
        LocationStatus.REQUESTING -> "正在请求定位权限…"
        LocationStatus.LOCATING -> "正在获取当前位置…"
        LocationStatus.FOUND -> entryLocation?.displayText() ?: "已获取当前位置"
        LocationStatus.DENIED -> "未授权定位，本次将不记录位置"
        LocationStatus.UNAVAILABLE -> "暂时无法获取位置，本次将不记录位置"
    }

    Column {
        SheetTitle("新增农户", "记录农户和当前位置，稍后添加花生类型")
        FieldLabel("农户名称")
        LedgerTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            placeholder = "输入农户名称"
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceSoft)
                .border(1.dp, Line, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = AppIcon.Pin,
                tint = Primary,
                background = GoldBg,
                size = 30.dp,
                iconSize = 15.dp
            )
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("当前位置", color = Muted, fontSize = 10.5.sp)
                Spacer(Modifier.height(1.dp))
                Text(
                    text = statusText,
                    color = if (locationStatus == LocationStatus.FOUND) TextSoft else Muted,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (locationStatus == LocationStatus.FOUND) {
                AppIconView(
                    icon = AppIcon.Check,
                    tint = Primary,
                    modifier = Modifier.size(14.dp)
                )
            } else if (locationStatus == LocationStatus.DENIED ||
                locationStatus == LocationStatus.UNAVAILABLE
            ) {
                Text(
                    text = "重试",
                    color = Primary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { retryLocation() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        SheetActionRow {
            GhostButton("取消", { state.closeSheet() }, Modifier.weight(1f))
            PrimaryButton(
                text = "保存",
                onClick = { state.addEntry(name, "", location = entryLocation) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AddBuyerEntrySheet(state: LedgerState) {
    var selectedBuyer by remember { mutableStateOf("") }
    var showNewBuyer by remember { mutableStateOf(false) }
    var newBuyerName by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }

    Column {
        SheetTitle("新增收购记录", "选择收购方，稍后添加花生类型和称重")
        FieldLabel("收购方")
        BuyerSelector(
            state = state,
            selected = if (showNewBuyer) "＋ 新增收购方..." else selectedBuyer,
            expanded = menuExpanded,
            onExpandedChange = { menuExpanded = it },
            onPick = { buyer ->
                selectedBuyer = buyer
                showNewBuyer = false
                menuExpanded = false
            },
            onPickNew = {
                showNewBuyer = true
                menuExpanded = false
            }
        )
        if (showNewBuyer) {
            Spacer(Modifier.height(14.dp))
            FieldLabel("新收购方名称")
            LedgerTextField(
                value = newBuyerName,
                onValueChange = { newBuyerName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "输入收购方名称"
            )
        }
        Spacer(Modifier.height(20.dp))
        SheetActionRow {
            GhostButton("取消", { state.closeSheet() }, Modifier.weight(1f))
            PrimaryButton(
                text = "保存",
                onClick = {
                    if (showNewBuyer) {
                        state.addEntry("", newBuyerName, newBuyer = true)
                    } else {
                        if (selectedBuyer.isBlank()) {
                            state.showToast("请选择收购方")
                        } else {
                            state.addEntry("", selectedBuyer)
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BuyerSelector(
    state: LedgerState,
    selected: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPick: (String) -> Unit,
    onPickNew: () -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(FieldBg)
                .border(1.dp, FieldBorder, RoundedCornerShape(14.dp))
                .clickable { onExpandedChange(true) }
                .padding(horizontal = 14.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selected.ifBlank { "请选择收购方" },
                color = if (selected.isBlank()) Placeholder else TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            AppIconView(
                icon = AppIcon.ChevronDown,
                tint = Placeholder,
                modifier = Modifier.size(15.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.width(280.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = CardWhite,
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, CardBorder)
        ) {
            state.activeBuyers().forEach { buyer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(buyer) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(SurfaceSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            buyer.take(1),
                            color = Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(buyer, fontSize = 15.sp, color = TextMain, modifier = Modifier.weight(1f))
                    if (buyer == selected) {
                        AppIconView(AppIcon.Check, tint = Primary, modifier = Modifier.size(15.dp))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPickNew)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconView(AppIcon.Plus, tint = Primary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(11.dp))
                Text("新增收购方", fontSize = 15.sp, color = Primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AddTypeSheet(state: LedgerState, entryId: String) {
    val presets = if (state.tab == LedgerTab.BUY) {
        state.activeBuyTypes()
    } else {
        state.activeSellTypes()
    }
    val lastType = state.lastUsedType(state.tab)?.takeIf { it in presets }
    var selectedName by remember(entryId, state.tab, lastType) {
        mutableStateOf(lastType ?: presets.firstOrNull().orEmpty())
    }
    var customName by remember(entryId, state.tab) { mutableStateOf("") }
    Column {
        SheetTitle(
            text = "选择花生类型",
            subtitle = if (lastType != null && selectedName == lastType) {
                "已默认选择上次使用的「$lastType」"
            } else {
                "选择预设，或输入一个新的类型"
            }
        )
        if (presets.isEmpty()) {
            Text(
                text = "还没有可用类型，可在下方输入新类型",
                color = Placeholder,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
            ) {
                presets.forEach { name ->
                    TypeChoiceChip(
                        name = name,
                        selected = customName.isBlank() && selectedName == name,
                        isLastUsed = name == lastType,
                        onClick = {
                            selectedName = name
                            customName = ""
                        }
                    )
                }
            }
        }
        FieldLabel("新增自定义类型")
        LedgerTextField(
            value = customName,
            onValueChange = {
                customName = it
                if (it.isBlank()) {
                    selectedName = lastType ?: presets.firstOrNull().orEmpty()
                } else {
                    selectedName = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = "输入类型名称"
        )
        Spacer(Modifier.height(20.dp))
        SheetActionRow {
            GhostButton("取消", { state.closeSheet() }, Modifier.weight(1f))
            PrimaryButton(
                text = "下一步",
                onClick = {
                    val custom = customName.trim()
                    when {
                        custom.isNotEmpty() -> state.addTypeAndOpen(
                            entryId,
                            custom,
                            saveAsPreset = true
                        )
                        selectedName.isNotBlank() -> state.addTypeAndOpen(
                            entryId,
                            selectedName,
                            saveAsPreset = false
                        )
                        else -> state.showToast("请选择或输入类型")
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TypeChoiceChip(
    name: String,
    selected: Boolean,
    isLastUsed: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Primary else SurfaceSoft)
            .border(1.dp, if (selected) Primary else LineStrong, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            AppIconView(AppIcon.Check, tint = CardWhite, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = name,
            color = if (selected) CardWhite else TextSoft,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (isLastUsed) {
            Spacer(Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (selected) CardWhite.copy(alpha = 0.22f) else Primary.copy(alpha = 0.1f)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "上次",
                    color = if (selected) CardWhite else Primary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ItemFormSheet(state: LedgerState, sheet: Sheet.ItemForm) {
    val entry = state.findEntry(sheet.entryId)
    val type = entry?.types?.firstOrNull { it.id == sheet.typeId }
    if (entry == null || type == null) {
        LaunchedEffect(Unit) { state.closeSheet() }
        return
    }
    val item = sheet.itemId?.let { id -> type.items.firstOrNull { it.id == id } }
    val isEdit = item != null
    val lastPrice = remember(sheet.formKey) {
        if (!isEdit) state.findLastPrice(sheet.entryId, sheet.typeId) else null
    }
    val focusRequester = remember(sheet.formKey) { FocusRequester() }
    LaunchedEffect(sheet.formKey) {
        delay(320)
        focusRequester.requestFocus()
    }

    fun autoAmount(weightText: String, priceText: String): String {
        val value = round2(
            (weightText.toDoubleOrNull() ?: 0.0) * (priceText.toDoubleOrNull() ?: 0.0)
        )
        return String.format(Locale.US, "%.2f", value)
    }

    var weightText by remember(sheet.formKey) {
        mutableStateOf(item?.weight?.let { fmtNum(it) } ?: "")
    }

    var priceText by remember(sheet.formKey) {
        mutableStateOf(
            item?.price?.let { fmtNum(it) }
                ?: lastPrice?.let { fmtNum(it) }
                ?: ""
        )
    }
    val amountText = autoAmount(weightText, priceText)

    fun save(keepOpen: Boolean) {
        val weight = round2(weightText.toDoubleOrNull() ?: 0.0)
        val price = round2(priceText.toDoubleOrNull() ?: 0.0)
        if (weight <= 0.0) {
            state.closeSheet()
            return
        }
        val saved = state.saveItem(
            entryId = sheet.entryId,
            typeId = sheet.typeId,
            itemId = sheet.itemId,
            weight = weight,
            price = price,
            keepOpen = keepOpen
        )
        if (!saved) {
            state.closeSheet()
        }
    }

    Column {
        SheetTitle(
            text = if (isEdit) "编辑称" else "新增称",
            subtitle = "${type.name} · 输入重量和单价，金额自动计算"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel("重量（斤）")
                LedgerTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = "0",
                    keyboardType = KeyboardType.Decimal
                )
            }
            Column(Modifier.weight(1f)) {
                FieldLabel("单价（元/斤）")
                LedgerTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "0",
                    keyboardType = KeyboardType.Decimal
                )
            }
        }
        if (!isEdit && lastPrice != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceSoft)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconView(AppIcon.Tag, tint = Primary, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "上次单价 ¥${String.format(Locale.US, "%.2f", round2(lastPrice))}",
                    color = PrimaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel("金额（元）", Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceSoft)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "自动计算",
                    color = Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceSoft)
                .border(1.dp, Line, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¥", color = Primary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(4.dp))
            Text(
                text = amountText,
                color = TextMain,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "不可编辑",
                color = Placeholder,
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isEdit) {
                DangerGhostButton(
                    text = "删除",
                    onClick = {
                        state.deleteItem(sheet.entryId, sheet.typeId, item!!.id)
                    },
                    modifier = Modifier.weight(0.9f)
                )
            }
            GhostButton("取消", { state.closeSheet() }, Modifier.weight(1f))
            if (!isEdit) {
                GhostButton(
                    text = "再记一称",
                    onClick = { save(keepOpen = true) },
                    modifier = Modifier.weight(1.35f)
                )
            }
            PrimaryButton(
                text = "保存",
                onClick = { save(keepOpen = false) },
                modifier = Modifier.weight(1.1f)
            )
        }
    }
}

@Composable
private fun EntryMenuSheet(state: LedgerState, entryId: String) {
    val entry = state.findEntry(entryId) ?: return
    val isBuy = state.tab == LedgerTab.BUY
    Column {
        SheetTitle(state.displayTitle(entry, state.tab), "选择要进行的操作")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isBuy) {
                SheetActionItem(
                    text = if (entry.settled) "标记为赊账" else "标记为已结清",
                    icon = AppIcon.Check,
                    onClick = { state.toggleSettled(entryId) }
                )
            }
            SheetActionItem(
                text = "修改${if (isBuy) "实付" else "实收"}金额",
                icon = AppIcon.Cash,
                onClick = {
                    if (state.canEditSensitive(entryId)) {
                        state.openSheet(Sheet.FinalAmount(entryId))
                    }
                }
            )
            SheetActionItem(
                text = if (entry.remark.isNotBlank()) "编辑备注" else "添加备注",
                icon = AppIcon.Note,
                onClick = { state.openSheet(Sheet.EntryRemark(entryId)) }
            )
            SheetActionItem(
                text = "重命名${if (isBuy) "农户" else "收购方"}",
                icon = AppIcon.Tag,
                onClick = {
                    if (isBuy || state.canEditSensitive(entryId)) {
                        state.openSheet(Sheet.RenameEntry(entryId))
                    }
                }
            )
            SheetActionItem(
                text = "删除这条记录",
                icon = AppIcon.Trash,
                danger = true,
                onClick = { state.deleteEntry(entryId) }
            )
        }
        Spacer(Modifier.height(14.dp))
        GhostButton("取消", { state.closeSheet() }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun RenameEntrySheet(state: LedgerState, entryId: String) {
    val entry = state.findEntry(entryId) ?: return
    val isBuy = state.tab == LedgerTab.BUY
    var name by remember(entryId) { mutableStateOf(entry.title(state.tab)) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(entryId) {
        delay(280)
        focusRequester.requestFocus()
    }
    Column {
        SheetTitle("重命名${if (isBuy) "农户" else "收购方"}")
        FieldLabel("名称")
        LedgerTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )
        Spacer(Modifier.height(20.dp))
        SheetActionRow {
            GhostButton("取消", { state.closeSheet() }, Modifier.weight(1f))
            PrimaryButton(
                text = "保存",
                onClick = { state.renameEntry(entryId, name) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EntryRemarkSheet(state: LedgerState, entryId: String) {
    val entry = state.findEntry(entryId) ?: return
    var remark by remember(entryId) { mutableStateOf(entry.remark) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(entryId) {
        delay(280)
        focusRequester.requestFocus()
    }
    Column {
        SheetTitle(if (entry.remark.isNotBlank()) "编辑备注" else "添加备注")
        FieldLabel("备注")
        LedgerTextField(
            value = remark,
            onValueChange = { remark = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            placeholder = "输入备注",
            singleLine = false,
            minLines = 5,
            maxLines = 8,
            minHeight = 156.dp
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (entry.remark.isNotBlank()) {
                DangerGhostButton(
                    text = "清除",
                    onClick = { state.clearEntryRemark(entryId) },
                    modifier = Modifier.weight(0.9f)
                )
            }
            GhostButton("取消", { state.closeSheet() }, Modifier.weight(1f))
            PrimaryButton(
                text = "保存",
                onClick = { state.saveEntryRemark(entryId, remark) },
                modifier = Modifier.weight(1.1f)
            )
        }
    }
}

@Composable
private fun DayRemarkSheet(state: LedgerState) {
    val current = state.currentDay.remark
    var remark by remember(state.currentDate) { mutableStateOf(current) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(state.currentDate) {
        delay(280)
        focusRequester.requestFocus()
    }
    Column {
        SheetTitle(if (current.isNotBlank()) "编辑备注" else "添加备注")
        FieldLabel("备注")
        LedgerTextField(
            value = remark,
            onValueChange = { remark = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            placeholder = "例如：下雨，只收半天",
            singleLine = false,
            minLines = 5,
            maxLines = 8,
            minHeight = 156.dp
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (current.isNotBlank()) {
                DangerGhostButton(
                    text = "清除",
                    onClick = { state.clearDayRemark() },
                    modifier = Modifier.weight(0.9f)
                )
            }
            GhostButton("取消", { state.closeSheet() }, Modifier.weight(1f))
            PrimaryButton(
                text = "保存",
                onClick = { state.saveDayRemark(remark) },
                modifier = Modifier.weight(1.1f)
            )
        }
    }
}

@Composable
private fun FinalAmountSheet(state: LedgerState, entryId: String) {
    val entry = state.findEntry(entryId) ?: return
    val isBuy = state.tab == LedgerTab.BUY
    val finalLabel = if (isBuy) "实付" else "实收"
    val totalLabel = if (isBuy) "应付合计" else "应收合计"
    val total = entryTotal(entry)
    val final = entryFinal(entry)

    var amountText by remember(entryId) {
        mutableStateOf(String.format(Locale.US, "%.2f", final))
    }
    val currentValue = amountText.toDoubleOrNull() ?: 0.0
    val diff = round2(currentValue - total)
    val suggestion = nextRoundUp(currentValue)

    Column {
        SheetTitle("修改$finalLabel", "$totalLabel ${fmtMoney(total)}")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
        ) {
            if (suggestion != null) {
                PillChip(
                    text = "取整到 ${fmtNum(suggestion.value)}",
                    onClick = {
                        amountText = String.format(Locale.US, "%.2f", suggestion.value)
                    }
                )
            }
            PillChip(
                text = "恢复原价",
                onClick = { amountText = String.format(Locale.US, "%.2f", total) }
            )
        }
        FieldLabel("$finalLabel（元）")
        LedgerTextField(
            value = amountText,
            onValueChange = { amountText = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Decimal
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (abs(diff) < 0.005) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceSoft)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text("金额未调整", color = Muted, fontSize = 12.sp)
                }
            } else {
                val label = when {
                    diff > 0 && isBuy -> "多付"
                    diff > 0 -> "多收"
                    isBuy -> "少付"
                    else -> "少收"
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoldBg)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$label ", color = Muted, fontSize = 12.sp)
                    Text(
                        text = String.format(Locale.US, "%.2f", abs(diff)),
                        color = Primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        SheetActionRow {
            GhostButton("取消", { state.closeSheet() }, Modifier.weight(1f))
            PrimaryButton(
                text = "保存",
                onClick = {
                    val value = amountText.toDoubleOrNull()
                    if (value == null) {
                        state.showToast("请输入金额")
                    } else {
                        state.saveFinalAmount(entryId, value)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SettingActionsSheet(state: LedgerState, kind: SettingKind, name: String) {
    val option = state.settingsList(kind).firstOrNull { it.name == name } ?: return
    val usage = state.settingUsage(kind, name)
    Column {
        SheetTitle(
            text = name,
            subtitle = when {
                !option.active -> "已失效 · 记账时不会显示"
                usage > 0 -> "已被 $usage 条记录使用"
                else -> "尚未被记录使用"
            }
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetActionItem(
                text = "重命名",
                icon = AppIcon.Edit,
                onClick = { state.openSheet(Sheet.RenameSetting(kind, name)) }
            )
            SheetActionItem(
                text = if (option.active) "设为失效" else "恢复使用",
                icon = if (option.active) AppIcon.EyeOff else AppIcon.Eye,
                onClick = {
                    state.setSettingActive(kind, name, !option.active)
                    state.closeSheet()
                }
            )
            SheetActionItem(
                text = if (usage > 0) "删除（已被使用，不可删除）" else "删除",
                icon = AppIcon.Trash,
                danger = true,
                onClick = { state.requestDeleteSetting(kind, name) }
            )
        }
        Spacer(Modifier.height(14.dp))
        GhostButton("取消", { state.closeSheet() }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun RenameSettingSheet(state: LedgerState, kind: SettingKind, oldName: String) {
    state.settingsList(kind).firstOrNull { it.name == oldName } ?: return
    var name by remember(oldName) { mutableStateOf(oldName) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(oldName) {
        delay(280)
        focusRequester.requestFocus()
    }
    val category = when (kind) {
        SettingKind.BUY_TYPES -> "采购类型"
        SettingKind.SELL_TYPES -> "销售类型"
        SettingKind.BUYERS -> "收购方"
    }
    Column {
        SheetTitle("重命名$category", "已有记账中的同名项会一起更新")
        FieldLabel("名称")
        LedgerTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "重命名后，历史记录中的名称会同步更新。",
            color = Muted,
            fontSize = 11.5.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(20.dp))
        SheetActionRow {
            GhostButton("取消", { state.closeSheet() }, Modifier.weight(1f))
            PrimaryButton(
                text = "保存",
                onClick = {
                    if (!state.renameSetting(kind, oldName, name)) {
                        focusRequester.requestFocus()
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConfirmSheet(state: LedgerState, sheet: Sheet.Confirm) {
    Column {
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(if (sheet.destructive) RedBg else GoldBg),
                contentAlignment = Alignment.Center
            ) {
                val confirmIcon = sheet.icon ?: if (sheet.destructive) AppIcon.Trash else AppIcon.Check
                AppIconView(
                    icon = confirmIcon,
                    tint = if (sheet.destructive) Red else Primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = sheet.title,
            color = TextMain,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (sheet.message.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(
                text = sheet.message,
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(22.dp))
        SheetActionRow {
            GhostButton("取消", { state.closeSheet() }, Modifier.weight(1f))
            if (sheet.destructive) {
                DangerButton(
                    text = sheet.confirmLabel,
                    onClick = {
                        state.closeSheet()
                        sheet.onConfirm()
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                PrimaryButton(
                    text = sheet.confirmLabel,
                    onClick = {
                        state.closeSheet()
                        sheet.onConfirm()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
