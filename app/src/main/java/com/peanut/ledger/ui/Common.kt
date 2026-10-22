package com.peanut.ledger.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peanut.ledger.ui.theme.Amber
import com.peanut.ledger.ui.theme.CardBorder
import com.peanut.ledger.ui.theme.CardWhite
import com.peanut.ledger.ui.theme.FieldBg
import com.peanut.ledger.ui.theme.FieldBorder
import com.peanut.ledger.ui.theme.HeaderEnd
import com.peanut.ledger.ui.theme.HeaderMid
import com.peanut.ledger.ui.theme.HeaderStart
import com.peanut.ledger.ui.theme.IconMuted
import com.peanut.ledger.ui.theme.Line
import com.peanut.ledger.ui.theme.LineStrong
import com.peanut.ledger.ui.theme.Muted
import com.peanut.ledger.ui.theme.MutedLight
import com.peanut.ledger.ui.theme.Placeholder
import com.peanut.ledger.ui.theme.Primary
import com.peanut.ledger.ui.theme.PrimaryDark
import com.peanut.ledger.ui.theme.Red
import com.peanut.ledger.ui.theme.RedBg
import com.peanut.ledger.ui.theme.SurfaceSoft
import com.peanut.ledger.ui.theme.SurfaceWarm
import com.peanut.ledger.ui.theme.TextMain
import com.peanut.ledger.ui.theme.TextSoft

val TopGradient = Brush.linearGradient(
    colors = listOf(HeaderStart, HeaderMid, HeaderEnd)
)

@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun LedgerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isManual: Boolean = false,
    singleLine: Boolean = true,
    minHeight: Dp = 50.dp
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val borderColor by animateColorAsState(
        targetValue = when {
            isManual -> Color(0xFFDDAE6B)
            focused -> Primary
            else -> FieldBorder
        },
        animationSpec = tween(150),
        label = "fieldBorder"
    )
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isManual -> Color(0xFFFFFBF3)
            focused -> CardWhite
            else -> FieldBg
        },
        animationSpec = tween(150),
        label = "fieldBackground"
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = TextStyle(
            fontSize = 16.sp,
            lineHeight = 22.sp,
            color = if (isManual) PrimaryDark else TextMain,
            fontWeight = if (isManual) FontWeight.SemiBold else FontWeight.Medium
        ),
        singleLine = singleLine,
        cursorBrush = SolidColor(Primary),
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .background(backgroundColor, shape)
                    .border(1.dp, borderColor, shape)
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = Placeholder,
                        fontSize = 15.5.sp,
                        maxLines = 1
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) TopGradient else Brush.linearGradient(listOf(Muted, Muted)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = CardWhite,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceWarm)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF7A6A55),
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun DangerGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(RedBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Red, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Red)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = CardWhite, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun PillChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceSoft)
            .border(1.dp, LineStrong, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextSoft, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun SoftAddButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceSoft)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconView(AppIcon.Plus, tint = Primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = Primary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun SheetActionItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: AppIcon? = null,
    danger: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (danger) RedBg.copy(alpha = 0.7f) else SurfaceSoft)
            .border(1.dp, if (danger) Red.copy(alpha = 0.12f) else Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (danger) RedBg else CardWhite.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                AppIconView(
                    icon = icon,
                    tint = if (danger) Red else Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            color = if (danger) Red else TextSoft,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        AppIconView(
            icon = AppIcon.ChevronRight,
            tint = IconMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun HeaderIconButton(
    icon: AppIcon,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (active) CardWhite else CardWhite.copy(alpha = 0.14f))
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppIconView(
            icon = icon,
            tint = if (active) Primary else CardWhite,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
fun CircleIconButton(
    icon: AppIcon,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CardWhite,
    background: Color = CardWhite.copy(alpha = 0.14f)
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppIconView(icon = icon, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun IconBadge(
    icon: AppIcon,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    iconSize: Dp = 17.dp,
    shape: RoundedCornerShape = RoundedCornerShape(11.dp)
) {
    Box(
        modifier = modifier.size(size).clip(shape).background(background),
        contentAlignment = Alignment.Center
    ) {
        AppIconView(icon = icon, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun SheetHandle() {
    Box(
        modifier = Modifier
            .width(38.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(LineStrong)
    )
}

@Composable
fun SheetTitle(text: String, subtitle: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = TextMain,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = subtitle,
                color = Muted,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Muted,
        fontSize = 12.5.sp,
        modifier = modifier.padding(bottom = 7.dp)
    )
}

@Composable
fun ModeTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Primary.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = Primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CounterTag(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(SurfaceSoft)
            .border(1.dp, Line, RoundedCornerShape(7.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = Muted, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TinyLabel(text: String, color: Color = MutedLight, modifier: Modifier = Modifier) {
    Text(text, color = color, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, modifier = modifier)
}

@Composable
fun ChevronDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Line)
    )
}

@Composable
fun AccentDot(color: Color = Amber, size: Dp = 6.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}
