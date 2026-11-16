package ee.vattson

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import ee.vattson.data.dataStore
import ee.vattson.data.repository.EleringRepository
import ee.vattson.data.repository.SettingsRepository
import ee.vattson.ui.screens.MainScreen
import ee.vattson.ui.screens.MainViewModelFactory
import ee.vattson.ui.theme.VattsonTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()

        val viewModel = ViewModelProvider(this,
            MainViewModelFactory(
                application = application,
                eleringRepository = EleringRepository(this),
                settingsRepository = SettingsRepository(dataStore)
            )
        )[ee.vattson.ui.viewmodel.MainViewModel::class.java]

        splashScreen.setKeepOnScreenCondition {
            !viewModel.uiState.value.isSettingsLoaded
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val isDarkTheme = when (uiState.appTheme) {
                ee.vattson.data.model.AppTheme.LIGHT -> false
                ee.vattson.data.model.AppTheme.DARK -> true
                ee.vattson.data.model.AppTheme.SYSTEM,
                ee.vattson.data.model.AppTheme.DYNAMIC -> isSystemInDarkTheme()
            }

            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
            }

            VattsonTheme(appTheme = uiState.appTheme) {
                MainScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
