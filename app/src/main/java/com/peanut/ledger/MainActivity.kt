package com.peanut.ledger

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.peanut.ledger.ui.LedgerState
import com.peanut.ledger.ui.PeanutLedgerApp
import com.peanut.ledger.ui.theme.PeanutLedgerTheme

class MainActivity : ComponentActivity() {
    private var ledgerState: LedgerState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            val context = LocalContext.current.applicationContext
            val state = remember(context) { LedgerState(context) }
            SideEffect { ledgerState = state }
            PeanutLedgerTheme {
                PeanutLedgerApp(state)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        ledgerState?.maybeAutoBackup()
    }
}
