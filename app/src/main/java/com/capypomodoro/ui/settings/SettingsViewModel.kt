package com.capypomodoro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.capypomodoro.AppContainer
import com.capypomodoro.appContainer
import com.capypomodoro.data.AppSettings
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settings

    /** Persists straight to DataStore (on the app scope, so leaving the screen never loses a change). */
    fun update(transform: (AppSettings) -> AppSettings) = container.updateSettings(transform)

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(checkNotNull(this[APPLICATION_KEY]).appContainer) }
        }
    }
}
