package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.SamWakeWordService
import com.example.ui.SamScreen
import com.example.ui.SamViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var viewModelInstance: SamViewModel? = null

    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SamWakeWordService.ACTION_WAKE_WORD_TRIGGERED) {
                val keyword = intent.getStringExtra(SamWakeWordService.EXTRA_DETECTED_KEYWORD) ?: "Hey Arushi"
                viewModelInstance?.startSession(keyword)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: SamViewModel = viewModel()
            viewModelInstance = vm
            val uiState by vm.uiState.collectAsState()

            MyApplicationTheme(
                accentPrimary = uiState.themeAccent.primary,
                accentSecondary = uiState.themeAccent.secondary,
                accentTertiary = uiState.themeAccent.tertiary,
                accentGlow = uiState.themeAccent.glow
            ) {
                SamScreen(viewModel = vm)
            }
        }
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(SamWakeWordService.ACTION_WAKE_WORD_TRIGGERED)
        ContextCompat.registerReceiver(
            this,
            wakeWordReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(wakeWordReceiver)
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("trigger_listening", false) == true) {
            val keyword = intent.getStringExtra(SamWakeWordService.EXTRA_DETECTED_KEYWORD)
            viewModelInstance?.startSession(keyword)
        }
        if (intent?.getBooleanExtra("trigger_morning_briefing", false) == true) {
            viewModelInstance?.deliverMorningBriefing()
        }
        if (intent?.getBooleanExtra("trigger_battery_alert", false) == true) {
            val pct = intent.getIntExtra("battery_pct", 15)
            viewModelInstance?.deliverBatteryAlert(pct)
        }
    }
}

