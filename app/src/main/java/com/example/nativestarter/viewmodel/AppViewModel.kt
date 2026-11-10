package com.example.nativestarter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.nativestarter.data.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Immutable snapshot exposed to the UI. */
data class AppUiState(
    val counter: Int = 0,
    val isDarkTheme: Boolean = false,
)

/**
 * Single source of truth for app state (counter + theme) across both screens.
 * Theme changes persist immediately; the counter is session-scoped.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AppUiState(isDarkTheme = ThemePreferences.load(application)))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun increment() {
        if (_uiState.value.counter < Int.MAX_VALUE) {
            _uiState.update { it.copy(counter = it.counter + 1) }
        }
    }

    fun decrement() {
        if (_uiState.value.counter > Int.MIN_VALUE) {
            _uiState.update { it.copy(counter = it.counter - 1) }
        }
    }

    fun reset() {
        _uiState.update { it.copy(counter = 0) }
    }

    fun setDarkTheme(enabled: Boolean) {
        _uiState.update { it.copy(isDarkTheme = enabled) }
        ThemePreferences.save(getApplication(), enabled)
    }
}
