package com.guitartuner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
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
            Toast.makeText(
                this,
                "Microphone permission is required for tuning",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.state.collectAsState()

            GuitarTunerTheme(darkTheme = state.isDarkMode) {
                TunerScreen(
                    state = state,
                    onStartListening = { startTuning() },
                    onStopListening = { viewModel.stopListening() },
                    onStringSelected = { viewModel.selectString(it) },
                    onCalibrationChanged = { viewModel.setCalibration(it) },
                    onToggleDarkMode = { viewModel.toggleDarkMode() }
                )
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
