package com.peanut.ledger.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.peanut.ledger.ui.theme.Bg
import kotlinx.coroutines.delay

@Composable
fun PeanutLedgerApp() {
    val context = LocalContext.current.applicationContext
    val state = remember { LedgerState(context) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            state.checkDayChange()
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        when (state.screen) {
            AppScreen.MAIN -> MainScreen(state)
            AppScreen.SETTINGS -> SettingsScreen(state)
        }
        SheetHost(state)
        ToastHost(state)
    }
}

@Composable
private fun ToastHost(state: LedgerState) {
    val toast = state.toast
    var displayedToast by remember { mutableStateOf(toast) }
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            displayedToast = toast
            delay(1600)
            state.clearToast(toast.id)
        }
    }
    Box(
        modifier = Modifier.fillMaxSize().zIndex(100f),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 },
            exit = fadeOut(tween(160))
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 124.dp)
                    .navigationBarsPadding()
                    .shadow(8.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xF22C251E))
                    .padding(horizontal = 18.dp, vertical = 11.dp)
            ) {
                Text(
                    text = displayedToast?.text.orEmpty(),
                    color = Color.White,
                    fontSize = 13.5.sp,
                    maxLines = 2
                )
            }
        }
    }
}
