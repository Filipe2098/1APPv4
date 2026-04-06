package com.guitartuner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.guitartuner.i18n.StringKey
import com.guitartuner.i18n.Strings
import com.guitartuner.ui.SettingsScreen
import com.guitartuner.ui.TunerScreen
import com.guitartuner.ui.theme.GuitarTunerTheme
import com.guitartuner.viewmodel.TunerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TunerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            val msg = Strings.get(StringKey.MIC_PERMISSION, viewModel.state.value.language)
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.state.collectAsState()
            var showSettings by remember { mutableStateOf(false) }

            GuitarTunerTheme(darkTheme = state.isDarkMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    if (showSettings) {
                        SettingsScreen(
                            state = state,
                            onBack = { showSettings = false },
                            onTunerModeChanged = { viewModel.setTunerMode(it) },
                            onDarkModeChanged = { viewModel.setDarkMode(it) },
                            onLanguageChanged = { viewModel.setLanguage(it) },
                            onCalibrationChanged = { viewModel.setCalibration(it) },
                            onVibrationChanged = { viewModel.setVibrationEnabled(it) }
                        )
                    } else {
                        TunerScreen(
                            state = state,
                            onStartListening = { startTuning() },
                            onStopListening = { viewModel.stopListening() },
                            onOpenSettings = { showSettings = true }
                        )
                    }
                }
            }
        }
    }

    private fun startTuning() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.startListening()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopListening()
    }
}
