package com.peanut.ledger.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class AppTheme(val id: String, val label: String) {
    FOREST("forest", "松绿"),
    WARM("warm", "暖棕"),
    INK("ink", "黛蓝"),
    ROSE("rose", "绛紫"),
    JADE("jade", "青黛"),
    GRAPHITE("graphite", "墨灰"),
    OCEAN("ocean", "海盐"),
    SAKURA("sakura", "樱粉"),
    AMBER("amber", "琥珀");

    companion object {
        fun fromId(id: String): AppTheme = entries.firstOrNull { it.id == id } ?: FOREST
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
    val redBg: Color,
    val avatarBuy: Color,
    val avatarSell: Color
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
    redBg = Color(0xFFFBEDEA),
    avatarBuy = Color(0xFFC9853B),
    avatarSell = Color(0xFF5F9374)
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
    redBg = Color(0xFFFBEDEA),
    avatarBuy = Color(0xFF3F6F91),
    avatarSell = Color(0xFF5A7F78)
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
    redBg = Color(0xFFFBEDEA),
    avatarBuy = Color(0xFF8F5B70),
    avatarSell = Color(0xFF7B6582)
)

private val JadePalette = WarmPalette.copy(
    bg = Color(0xFFF2F7F6),
    bgOuter = Color(0xFFE6F0EE),
    surfaceSoft = Color(0xFFF6FAF9),
    surfaceWarm = Color(0xFFE7F0EE),
    cardBorder = Color(0xFFDEEAE7),
    primary = Color(0xFF3F7F7B),
    primaryDark = Color(0xFF2A5F5B),
    primaryLight = Color(0xFF6BA7A2),
    headerStart = Color(0xFF5E9A96),
    headerMid = Color(0xFF3F7F7B),
    headerEnd = Color(0xFF2A5F5B),
    textMain = Color(0xFF1D2A28),
    textSoft = Color(0xFF334441),
    muted = Color(0xFF6E817D),
    mutedLight = Color(0xFF8D9F9B),
    placeholder = Color(0xFFAEBFBB),
    iconMuted = Color(0xFFA4B6B1),
    line = Color(0xFFE1ECE9),
    lineStrong = Color(0xFFD1E0DC),
    fieldBg = Color(0xFFF7FBFA),
    fieldBorder = Color(0xFFDAE7E3),
    goldBg = Color(0xFFEDF5F3),
    todayBg = Color(0xFFEDF7F5),
    tagBg = Color(0xFFE4F2EF),
    chipBg = Color(0xFFF0F6F4),
    amber = Color(0xFFB08A4A),
    brown = Color(0xFF4E6F6A),
    avatarBuy = Color(0xFF3F7F7B),
    avatarSell = Color(0xFFB07A45)
)

private val ForestPalette = WarmPalette.copy(
    bg = Color(0xFFF5F7F0),
    bgOuter = Color(0xFFEAF0E1),
    surfaceSoft = Color(0xFFF8FAF4),
    surfaceWarm = Color(0xFFEAF0E0),
    cardBorder = Color(0xFFE1E8D7),
    primary = Color(0xFF5B7D45),
    primaryDark = Color(0xFF3F5C2E),
    primaryLight = Color(0xFF87A96C),
    headerStart = Color(0xFF7A9A63),
    headerMid = Color(0xFF5B7D45),
    headerEnd = Color(0xFF3F5C2E),
    textMain = Color(0xFF202A1B),
    textSoft = Color(0xFF37422F),
    muted = Color(0xFF77836D),
    mutedLight = Color(0xFF95A18B),
    placeholder = Color(0xFFB4BEAA),
    iconMuted = Color(0xFFABB79F),
    line = Color(0xFFE5EBDD),
    lineStrong = Color(0xFFD5DFC9),
    fieldBg = Color(0xFFF9FBF6),
    fieldBorder = Color(0xFFDFE7D5),
    goldBg = Color(0xFFF1F6EA),
    todayBg = Color(0xFFF1F6EA),
    tagBg = Color(0xFFEAF2E0),
    chipBg = Color(0xFFF3F7ED),
    amber = Color(0xFFB08A4A),
    brown = Color(0xFF5E6E45),
    avatarBuy = Color(0xFF5B7D45),
    avatarSell = Color(0xFF8A6A2F)
)

private val GraphitePalette = WarmPalette.copy(
    bg = Color(0xFFF5F5F4),
    bgOuter = Color(0xFFEBEBEA),
    surfaceSoft = Color(0xFFF9F9F8),
    surfaceWarm = Color(0xFFEAEAE8),
    cardBorder = Color(0xFFE2E2E0),
    primary = Color(0xFF3A3F45),
    primaryDark = Color(0xFF262A2E),
    primaryLight = Color(0xFF6E747A),
    headerStart = Color(0xFF555B62),
    headerMid = Color(0xFF3A3F45),
    headerEnd = Color(0xFF262A2E),
    textMain = Color(0xFF1E2022),
    textSoft = Color(0xFF34383C),
    muted = Color(0xFF717579),
    mutedLight = Color(0xFF909498),
    placeholder = Color(0xFFB0B4B8),
    iconMuted = Color(0xFFA6AAAE),
    line = Color(0xFFE5E5E3),
    lineStrong = Color(0xFFD6D6D3),
    fieldBg = Color(0xFFF8F8F7),
    fieldBorder = Color(0xFFDFDFDC),
    goldBg = Color(0xFFEFEFED),
    todayBg = Color(0xFFF0F0EE),
    tagBg = Color(0xFFEAEAE7),
    chipBg = Color(0xFFF2F2F0),
    amber = Color(0xFFA88A55),
    brown = Color(0xFF5B5F63),
    avatarBuy = Color(0xFF3A3F45),
    avatarSell = Color(0xFF8C6A50)
)

private val OceanPalette = WarmPalette.copy(
    bg = Color(0xFFF3F6FA),
    bgOuter = Color(0xFFE8EFF6),
    surfaceSoft = Color(0xFFF7FAFD),
    surfaceWarm = Color(0xFFE8EFF5),
    cardBorder = Color(0xFFDFE7F0),
    primary = Color(0xFF4C82A6),
    primaryDark = Color(0xFF2F6283),
    primaryLight = Color(0xFF7CA9C7),
    headerStart = Color(0xFF6A9CBB),
    headerMid = Color(0xFF4C82A6),
    headerEnd = Color(0xFF2F6283),
    textMain = Color(0xFF1D2A33),
    textSoft = Color(0xFF34444F),
    muted = Color(0xFF71818C),
    mutedLight = Color(0xFF909FAA),
    placeholder = Color(0xFFB0BEC8),
    iconMuted = Color(0xFFA6B4BE),
    line = Color(0xFFE2EAF2),
    lineStrong = Color(0xFFD2DEE9),
    fieldBg = Color(0xFFF8FBFD),
    fieldBorder = Color(0xFFDCE6EF),
    goldBg = Color(0xFFEDF4F9),
    todayBg = Color(0xFFEEF5FA),
    tagBg = Color(0xFFE7F0F7),
    chipBg = Color(0xFFF1F6FA),
    amber = Color(0xFFB08A4A),
    brown = Color(0xFF4B6B80),
    avatarBuy = Color(0xFF4C82A6),
    avatarSell = Color(0xFF6F8F7A)
)

private val SakuraPalette = WarmPalette.copy(
    bg = Color(0xFFFBF4F5),
    bgOuter = Color(0xFFF4E8EB),
    surfaceSoft = Color(0xFFFDF9FA),
    surfaceWarm = Color(0xFFF4E9EC),
    cardBorder = Color(0xFFEFDFE3),
    primary = Color(0xFFC06A83),
    primaryDark = Color(0xFF9A4B62),
    primaryLight = Color(0xFFD796A9),
    headerStart = Color(0xFFD48FA2),
    headerMid = Color(0xFFC06A83),
    headerEnd = Color(0xFF9A4B62),
    textMain = Color(0xFF2F2025),
    textSoft = Color(0xFF4A343C),
    muted = Color(0xFF8C767D),
    mutedLight = Color(0xFFA8959B),
    placeholder = Color(0xFFC2AEB4),
    iconMuted = Color(0xFFBBA7AD),
    line = Color(0xFFF1E4E7),
    lineStrong = Color(0xFFE6D3D8),
    fieldBg = Color(0xFFFCF8F9),
    fieldBorder = Color(0xFFECDBDF),
    goldBg = Color(0xFFFAF0F3),
    todayBg = Color(0xFFFAF0F3),
    tagBg = Color(0xFFF7E8ED),
    chipBg = Color(0xFFF9F1F3),
    amber = Color(0xFFB88A5B),
    brown = Color(0xFF825E69),
    avatarBuy = Color(0xFFC06A83),
    avatarSell = Color(0xFF8F7BA8)
)

private val AmberPalette = WarmPalette.copy(
    bg = Color(0xFFFBF7EC),
    bgOuter = Color(0xFFF4EDDD),
    surfaceSoft = Color(0xFFFDFAF3),
    surfaceWarm = Color(0xFFF4ECD9),
    cardBorder = Color(0xFFEDE3CF),
    primary = Color(0xFFC08A1E),
    primaryDark = Color(0xFF96650B),
    primaryLight = Color(0xFFDDB05A),
    headerStart = Color(0xFFDFAE3F),
    headerMid = Color(0xFFC08A1E),
    headerEnd = Color(0xFF96650B),
    textMain = Color(0xFF2E281C),
    textSoft = Color(0xFF473F2F),
    muted = Color(0xFF8A7E68),
    mutedLight = Color(0xFFA79A84),
    placeholder = Color(0xFFBEB29C),
    iconMuted = Color(0xFFB6A992),
    line = Color(0xFFF0E7D3),
    lineStrong = Color(0xFFE3D6BB),
    fieldBg = Color(0xFFFCF9F1),
    fieldBorder = Color(0xFFE8DCC4),
    goldBg = Color(0xFFFAF2DE),
    todayBg = Color(0xFFFAF3E0),
    tagBg = Color(0xFFF7EBD0),
    chipBg = Color(0xFFF8F2E3),
    amber = Color(0xFFC08A1E),
    brown = Color(0xFF8A6A2F),
    avatarBuy = Color(0xFFC08A1E),
    avatarSell = Color(0xFF6E8C6A)
)

private val palettes = mapOf(
    AppTheme.WARM to WarmPalette,
    AppTheme.INK to InkPalette,
    AppTheme.ROSE to RosePalette,
    AppTheme.JADE to JadePalette,
    AppTheme.FOREST to ForestPalette,
    AppTheme.GRAPHITE to GraphitePalette,
    AppTheme.OCEAN to OceanPalette,
    AppTheme.SAKURA to SakuraPalette,
    AppTheme.AMBER to AmberPalette
)

private var activeThemeState by mutableStateOf(AppTheme.FOREST)

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
val AvatarBuy: Color get() = palette.avatarBuy
val AvatarSell: Color get() = palette.avatarSell

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
