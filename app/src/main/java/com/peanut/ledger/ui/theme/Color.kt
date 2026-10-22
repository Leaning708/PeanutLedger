package com.peanut.ledger.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AppTheme(val id: String, val label: String) {
    WARM("warm", "暖棕"),
    INK("ink", "黛蓝"),
    ROSE("rose", "绛紫");

    companion object {
        fun fromId(id: String): AppTheme = entries.firstOrNull { it.id == id } ?: WARM
    }
}

data class LedgerPalette(
    val bg: Color,
    val bgOuter: Color,
    val surfaceSoft: Color,
    val surfaceWarm: Color,
    val cardBorder: Color,
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val headerStart: Color,
    val headerMid: Color,
    val headerEnd: Color,
    val textMain: Color,
    val textSoft: Color,
    val muted: Color,
    val mutedLight: Color,
    val placeholder: Color,
    val iconMuted: Color,
    val line: Color,
    val lineStrong: Color,
    val fieldBg: Color,
    val fieldBorder: Color,
    val goldBg: Color,
    val todayBg: Color,
    val tagBg: Color,
    val chipBg: Color,
    val amber: Color,
    val brown: Color,
    val green: Color,
    val greenBg: Color,
    val red: Color,
    val redBg: Color
)

private val WarmPalette = LedgerPalette(
    bg = Color(0xFFF7F2E9),
    bgOuter = Color(0xFFF1EADD),
    surfaceSoft = Color(0xFFFBF7EF),
    surfaceWarm = Color(0xFFF5EDDF),
    cardBorder = Color(0xFFF0E7DA),
    primary = Color(0xFFB86A22),
    primaryDark = Color(0xFF8E4D12),
    primaryLight = Color(0xFFD7924D),
    headerStart = Color(0xFFD49452),
    headerMid = Color(0xFFBE7327),
    headerEnd = Color(0xFFA85A1A),
    textMain = Color(0xFF2C251E),
    textSoft = Color(0xFF473D33),
    muted = Color(0xFF8C8072),
    mutedLight = Color(0xFFA99D8E),
    placeholder = Color(0xFFBCB0A0),
    iconMuted = Color(0xFFB2A696),
    line = Color(0xFFEEE5D7),
    lineStrong = Color(0xFFE2D6C3),
    fieldBg = Color(0xFFFBF8F2),
    fieldBorder = Color(0xFFE7DCCB),
    goldBg = Color(0xFFFBF0DF),
    todayBg = Color(0xFFFBF3E5),
    tagBg = Color(0xFFFBEDD7),
    chipBg = Color(0xFFF7F1E7),
    amber = Color(0xFFC28A3C),
    brown = Color(0xFF8A6136),
    green = Color(0xFF4F8B69),
    greenBg = Color(0xFFEAF4EE),
    red = Color(0xFFC74C3B),
    redBg = Color(0xFFFBEDEA)
)

private val InkPalette = LedgerPalette(
    bg = Color(0xFFF3F5F8),
    bgOuter = Color(0xFFE9EDF2),
    surfaceSoft = Color(0xFFF7F9FC),
    surfaceWarm = Color(0xFFE9EEF4),
    cardBorder = Color(0xFFE2E8EF),
    primary = Color(0xFF3F6F91),
    primaryDark = Color(0xFF2A5270),
    primaryLight = Color(0xFF6F9AB7),
    headerStart = Color(0xFF5C86A5),
    headerMid = Color(0xFF3F6F91),
    headerEnd = Color(0xFF2A5270),
    textMain = Color(0xFF1E2832),
    textSoft = Color(0xFF34414D),
    muted = Color(0xFF6F7D8A),
    mutedLight = Color(0xFF8E9BA7),
    placeholder = Color(0xFFAFBAC5),
    iconMuted = Color(0xFFA5B1BD),
    line = Color(0xFFE3E9F0),
    lineStrong = Color(0xFFD5DEE7),
    fieldBg = Color(0xFFF8FAFC),
    fieldBorder = Color(0xFFDDE5ED),
    goldBg = Color(0xFFEEF4F9),
    todayBg = Color(0xFFEEF4FA),
    tagBg = Color(0xFFE7EFF7),
    chipBg = Color(0xFFF1F5F9),
    amber = Color(0xFFB0884A),
    brown = Color(0xFF47627A),
    green = Color(0xFF3F8A68),
    greenBg = Color(0xFFE8F4EE),
    red = Color(0xFFC05243),
    redBg = Color(0xFFFBEDEA)
)

private val RosePalette = LedgerPalette(
    bg = Color(0xFFF8F3F4),
    bgOuter = Color(0xFFF1E8EA),
    surfaceSoft = Color(0xFFFCF8F9),
    surfaceWarm = Color(0xFFF2E9EB),
    cardBorder = Color(0xFFEDE1E4),
    primary = Color(0xFF8F5B70),
    primaryDark = Color(0xFF6E4054),
    primaryLight = Color(0xFFB78699),
    headerStart = Color(0xFFA87588),
    headerMid = Color(0xFF8F5B70),
    headerEnd = Color(0xFF6E4054),
    textMain = Color(0xFF2D2227),
    textSoft = Color(0xFF46363D),
    muted = Color(0xFF8A777D),
    mutedLight = Color(0xFFA69298),
    placeholder = Color(0xFFBDAAB0),
    iconMuted = Color(0xFFB8A5AB),
    line = Color(0xFFEFE3E6),
    lineStrong = Color(0xFFE3D3D7),
    fieldBg = Color(0xFFFBF7F8),
    fieldBorder = Color(0xFFE9DADD),
    goldBg = Color(0xFFF8EFF1),
    todayBg = Color(0xFFF9EFF2),
    tagBg = Color(0xFFF5E7EB),
    chipBg = Color(0xFFF5EEF0),
    amber = Color(0xFFB88A5B),
    brown = Color(0xFF7E5A66),
    green = Color(0xFF4F8B69),
    greenBg = Color(0xFFEAF4EE),
    red = Color(0xFFC74C3B),
    redBg = Color(0xFFFBEDEA)
)

private val palettes = mapOf(
    AppTheme.WARM to WarmPalette,
    AppTheme.INK to InkPalette,
    AppTheme.ROSE to RosePalette
)

private var activeThemeState by mutableStateOf(AppTheme.WARM)

val activeTheme: AppTheme get() = activeThemeState

fun setActiveTheme(theme: AppTheme) {
    activeThemeState = theme
}

fun themePalette(theme: AppTheme): LedgerPalette = palettes.getValue(theme)

fun themeHeaderColors(theme: AppTheme): List<Color> = themePalette(theme).let {
    listOf(it.headerStart, it.headerMid, it.headerEnd)
}

private val palette: LedgerPalette get() = themePalette(activeThemeState)

val Bg: Color get() = palette.bg
val BgOuter: Color get() = palette.bgOuter
val BgDeep: Color get() = palette.bgOuter

val CardWhite: Color = Color(0xFFFFFFFF)
val SurfaceSoft: Color get() = palette.surfaceSoft
val SurfaceWarm: Color get() = palette.surfaceWarm
val CardBorder: Color get() = palette.cardBorder

val Primary: Color get() = palette.primary
val PrimaryDark: Color get() = palette.primaryDark
val PrimaryLight: Color get() = palette.primaryLight
val HeaderStart: Color get() = palette.headerStart
val HeaderMid: Color get() = palette.headerMid
val HeaderEnd: Color get() = palette.headerEnd
val GoldBg: Color get() = palette.goldBg
val TagBg: Color get() = palette.tagBg
val Amber: Color get() = palette.amber
val Brown: Color get() = palette.brown

val TextMain: Color get() = palette.textMain
val TextSoft: Color get() = palette.textSoft
val Muted: Color get() = palette.muted
val MutedLight: Color get() = palette.mutedLight
val Placeholder: Color get() = palette.placeholder
val IconMuted: Color get() = palette.iconMuted

val Line: Color get() = palette.line
val LineStrong: Color get() = palette.lineStrong
val FieldBg: Color get() = palette.fieldBg
val FieldBorder: Color get() = palette.fieldBorder

val Green: Color get() = palette.green
val GreenBg: Color get() = palette.greenBg
val Red: Color get() = palette.red
val RedBg: Color get() = palette.redBg
val TodayBg: Color get() = palette.todayBg
val ChipBg: Color get() = palette.chipBg

// Backwards-compatible aliases used by existing components
val SubBg: Color get() = SurfaceSoft
val SegBg: Color get() = SurfaceWarm
val DashedLine: Color get() = LineStrong
val DashedText: Color get() = Brown
