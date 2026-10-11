package com.capypomodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.capypomodoro.ui.settings.SettingsRoute
import com.capypomodoro.ui.settings.SettingsViewModel
import com.capypomodoro.ui.theme.CapyColors
import com.capypomodoro.ui.theme.CapyTheme
import com.capypomodoro.ui.timer.TimerRoute
import com.capypomodoro.ui.timer.TimerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scrim = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(scrim, CapyColors.Ink.toArgb()),
            navigationBarStyle = SystemBarStyle.light(scrim, CapyColors.Ink.toArgb()),
        )
        setContent {
            CapyTheme { CapyNavHost() }
        }
    }
}

private object Routes {
    const val TIMER = "timer"
    const val SETTINGS = "settings"
}

@Composable
private fun CapyNavHost() {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = Routes.TIMER,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        composable(Routes.TIMER) {
            TimerRoute(
                viewModel = viewModel(factory = TimerViewModel.Factory),
                onOpenSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(
                viewModel = viewModel(factory = SettingsViewModel.Factory),
                onBack = { nav.popBackStack() },
            )
        }
    }
}
