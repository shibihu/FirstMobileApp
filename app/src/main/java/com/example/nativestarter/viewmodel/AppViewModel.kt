package com.example.nativestarter.viewmodel

import android.app.Application
import android.net.Uri
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nativestarter.data.ImportedVideo
import com.example.nativestarter.data.MediaRepository
import com.example.nativestarter.data.ThemePreferences
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Immutable snapshot exposed to the UI. */
data class AppUiState(
    val isDarkTheme: Boolean = true,
    val importedVideo: ImportedVideo? = null,
    val isImporting: Boolean = false,
    val importError: String? = null,
)

/**
 * Single source of truth for app state for the editor workspace and theme.
 * Theme changes persist immediately; the task count reflects the current AI queue.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaRepository(application)
    private val mediaPreferences = application.getSharedPreferences("media_workspace", Application.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        AppUiState(isDarkTheme = ThemePreferences.load(application)),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        mediaPreferences.getString(KEY_IMPORTED_VIDEO_URI, null)?.let { savedUri ->
            importVideo(Uri.parse(savedUri))
        }
    }

    fun importVideo(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        _uiState.update { it.copy(isImporting = true, importError = null) }
        viewModelScope.launch {
            runCatching { mediaRepository.loadVideo(uri) }
                .onSuccess { video ->
                    mediaPreferences.edit().putString(KEY_IMPORTED_VIDEO_URI, uri.toString()).apply()
                    _uiState.update { it.copy(importedVideo = video, isImporting = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importError = error.message ?: "Unable to open this video.",
                        )
                    }
                }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        _uiState.update { it.copy(isDarkTheme = enabled) }
        ThemePreferences.save(getApplication(), enabled)
    }

    private companion object {
        const val KEY_IMPORTED_VIDEO_URI = "imported_video_uri"
    }
}
