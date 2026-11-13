package com.example.nativestarter.viewmodel

import android.app.Application
import android.net.Uri
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nativestarter.export.ExportRepository
import com.example.nativestarter.data.ImportedVideo
import com.example.nativestarter.data.AiClipStudioApi
import com.example.nativestarter.data.BackendSettings
import com.example.nativestarter.data.MediaRepository
import com.example.nativestarter.data.ThemePreferences
import com.example.nativestarter.domain.editor.AudioSegment
import com.example.nativestarter.domain.editor.EditorTimeline
import com.example.nativestarter.domain.editor.EditorTimelineState
import com.example.nativestarter.domain.editor.SubtitleCue
import com.example.nativestarter.domain.assistant.AiSkillRegistry
import com.example.nativestarter.domain.assistant.AssistantMessage
import com.example.nativestarter.domain.assistant.AssistantMessageRole
import com.example.nativestarter.domain.assistant.BackendSkillOperation
import com.example.nativestarter.domain.assistant.SkillExecutionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

/** Immutable snapshot exposed to the UI. */
data class AppUiState(
    val isDarkTheme: Boolean = true,
    val importedVideo: ImportedVideo? = null,
    val isImporting: Boolean = false,
    val importError: String? = null,
    val playerError: String? = null,
    val previewReady: Boolean = false,
    val playheadMs: Long = 0L,
    val isPlaying: Boolean = false,
    val seekRequestId: Long = 0L,
    val editorTimeline: EditorTimelineState = EditorTimelineState(),
    val editorMessage: String? = null,
    val isExporting: Boolean = false,
    val exportProgress: Int = 0,
    val exportError: String? = null,
    val backendBaseUrl: String = "",
    val backendTokenConfigured: Boolean = false,
    val backendConnected: Boolean? = null,
    val backendBusy: Boolean = false,
    val backendProgress: Int = 0,
    val backendMessage: String? = null,
    val assistantMessages: List<AssistantMessage> = emptyList(),
    val selectedSkillIds: Set<String> = emptySet(),
    val assistantDraft: String = "",
    val skillPanelExpanded: Boolean = true,
    val assistantBusy: Boolean = false,
)

/**
 * Single source of truth for app state for the editor workspace and theme.
 * Theme changes persist immediately; the task count reflects the current AI queue.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val KEY_IMPORTED_VIDEO_URI = "imported_video_uri"
        const val TAG = "NativeEditorMedia"
    }

    private val mediaRepository = MediaRepository(application)
    private val backendSettings = BackendSettings(application)
    private val aiApi = AiClipStudioApi(application, backendSettings)
    private val exportRepository = ExportRepository(application)
    private var exportJob: Job? = null
    private var assistantJob: Job? = null
    private val mediaPreferences = application.getSharedPreferences("media_workspace", Application.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        AppUiState(
            isDarkTheme = ThemePreferences.load(application),
            backendBaseUrl = backendSettings.baseUrl(),
            backendTokenConfigured = !backendSettings.authToken().isNullOrEmpty(),
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        mediaPreferences.getString(KEY_IMPORTED_VIDEO_URI, null)?.let { savedUri ->
            importVideo(Uri.parse(savedUri), restoring = true)
        }
    }

    fun importVideo(uri: Uri, restoring: Boolean = false) {
        val persistedGrant = runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            true
        }.getOrElse { error ->
            Log.e(TAG, "Could not persist media URI grant for $uri", error)
            false
        }
        _uiState.update { it.copy(isImporting = true, importError = null) }
        viewModelScope.launch {
            runCatching { mediaRepository.loadVideo(uri, persistedGrant) }
                .onSuccess { video ->
                    mediaPreferences.edit().putString(KEY_IMPORTED_VIDEO_URI, uri.toString()).apply()
                    _uiState.update {
                        it.copy(
                            importedVideo = video,
                            isImporting = false,
                            editorTimeline = EditorTimeline.fromSource(video.durationMs ?: 0L),
                            playheadMs = 0L,
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Could not inspect media URI $uri", error)
                    if (restoring) mediaPreferences.edit().remove(KEY_IMPORTED_VIDEO_URI).apply()
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

    fun onPreviewPrepared() {
        _uiState.update { it.copy(playerError = null, previewReady = false) }
    }

    fun onPreviewRendered() {
        _uiState.update { it.copy(playerError = null, previewReady = true) }
    }

    fun onPreviewError(message: String) {
        _uiState.update { it.copy(playerError = message, previewReady = false, isPlaying = false) }
    }

    fun updatePlayback(positionMs: Long, playing: Boolean) {
        _uiState.update {
            val timeline = EditorTimeline.seek(it.editorTimeline, positionMs)
            it.copy(playheadMs = timeline.playheadMs, editorTimeline = timeline, isPlaying = playing)
        }
    }

    fun seekTimeline(positionMs: Long) {
        _uiState.update {
            val timeline = EditorTimeline.seek(it.editorTimeline, positionMs)
            it.copy(
                playheadMs = timeline.playheadMs,
                editorTimeline = timeline,
                seekRequestId = it.seekRequestId + 1,
            )
        }
    }

    fun selectSegment(id: String) = updateTimeline { EditorTimeline.select(it, id) }

    fun trimSelectedSegment(startMs: Long, endMs: Long) =
        updateTimeline { EditorTimeline.trimSelected(it, startMs, endMs) }

    fun splitAtPlayhead() = updateTimeline(EditorTimeline::splitAtPlayhead)

    fun deleteSelectedSegment() = updateTimeline(EditorTimeline::deleteSelected)

    fun importAudio(uri: Uri) {
        viewModelScope.launch {
            runCatching { mediaRepository.loadAudio(uri) }
                .onSuccess { audio ->
                    val takePersisted = runCatching {
                        getApplication<Application>().contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }.isSuccess
                    if (!takePersisted) Log.w(TAG, "Audio URI grant is not persistent: $uri")
                    updateTimeline { state ->
                        state.copy(audioSegments = state.audioSegments + audio.copy(timelineStartMs = state.playheadMs))
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Could not inspect audio URI $uri", error)
                    _uiState.update { it.copy(editorMessage = error.message ?: "Unable to import audio.") }
                }
        }
    }

    fun setAudioVolume(id: String, volume: Float) = updateTimeline { timeline ->
        timeline.copy(audioSegments = timeline.audioSegments.map { clip ->
            if (clip.id == id) clip.copy(volume = volume.coerceIn(0f, 4f)) else clip
        })
    }

    fun deleteAudio(id: String) = updateTimeline { timeline ->
        timeline.copy(audioSegments = timeline.audioSegments.filterNot { it.id == id })
    }

    fun addSubtitle(text: String) {
        val timeline = _uiState.value.editorTimeline
        val start = timeline.playheadMs
        val end = minOf(timeline.durationMs, start + 2_000L)
        if (text.isBlank() || end <= start) return
        updateTimeline { it.copy(subtitleCues = it.subtitleCues + SubtitleCue(startMs = start, endMs = end, text = text.trim())) }
    }

    fun updateSubtitle(cue: SubtitleCue) = updateTimeline { timeline ->
        val start = cue.startMs.coerceIn(0L, timeline.durationMs)
        val end = cue.endMs.coerceIn(start + 1L, timeline.durationMs.coerceAtLeast(start + 1L))
        timeline.copy(
            subtitleCues = timeline.subtitleCues.map {
                if (it.id == cue.id) cue.copy(startMs = start, endMs = end) else it
            },
        )
    }

    fun deleteSubtitle(id: String) = updateTimeline { timeline ->
        timeline.copy(subtitleCues = timeline.subtitleCues.filterNot { it.id == id })
    }

    fun importSrt(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val content = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Cannot read the selected subtitle file.")
                }
                EditorTimeline.parseSrt(content)
            }.onSuccess { cues ->
                updateTimeline { it.copy(subtitleCues = cues) }
                _uiState.update { it.copy(editorMessage = "Imported ${cues.size} subtitle cues.") }
            }.onFailure { error ->
                _uiState.update { it.copy(editorMessage = error.message ?: "Unable to import SRT.") }
            }
        }
    }

    fun exportSrt(uri: Uri) {
        val content = EditorTimeline.toSrt(_uiState.value.editorTimeline.subtitleCues)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(content)
                    } ?: error("Cannot write the selected subtitle file.")
                }
            }.onSuccess {
                _uiState.update { it.copy(editorMessage = "SRT exported.") }
            }.onFailure { error ->
                _uiState.update { it.copy(editorMessage = error.message ?: "Unable to export SRT.") }
            }
        }
    }

    fun exportVideo(destinationUri: Uri) {
        val snapshot = _uiState.value
        val video = snapshot.importedVideo
        if (video == null) {
            _uiState.update { it.copy(exportError = "Import a video before exporting.") }
            return
        }
        if (snapshot.editorTimeline.audioSegments.isNotEmpty()) {
            _uiState.update {
                it.copy(exportError = "MP4 export does not yet mix separately imported audio tracks. Remove them or export after audio mixing is supported.")
            }
            return
        }
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportProgress = 0, exportError = null) }
            try {
                exportRepository.exportVideo(
                    sourceUri = video.uri,
                    segments = snapshot.editorTimeline.videoSegments,
                    destinationUri = destinationUri,
                ) { progress ->
                    _uiState.update { it.copy(exportProgress = progress) }
                }
                _uiState.update { it.copy(isExporting = false, editorMessage = "MP4 export completed.") }
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(isExporting = false, editorMessage = "MP4 export cancelled.") }
                throw cancelled
            } catch (error: Exception) {
                Log.e(TAG, "MP4 export failed", error)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportError = error.message ?: "MP4 export failed.",
                    )
                }
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
    }

    fun clearExportError() {
        _uiState.update { it.copy(exportError = null) }
    }

    fun saveBackendSettings(baseUrl: String, authToken: String) {
        runCatching {
            val normalized = backendSettings.saveBaseUrl(baseUrl)
            if (authToken.isNotBlank()) backendSettings.saveAuthToken(authToken)
            _uiState.update {
                it.copy(
                    backendBaseUrl = normalized,
                    backendTokenConfigured = !backendSettings.authToken().isNullOrEmpty(),
                    backendConnected = null,
                    backendMessage = "Backend settings saved.",
                )
            }
        }.onFailure { error ->
            _uiState.update { it.copy(backendConnected = false, backendMessage = error.message) }
        }
    }

    fun clearBackendToken() {
        backendSettings.saveAuthToken("")
        _uiState.update {
            it.copy(backendTokenConfigured = false, backendConnected = null, backendMessage = "Backend token cleared.")
        }
    }

    fun setAssistantDraft(value: String) {
        _uiState.update { it.copy(assistantDraft = value) }
    }

    fun toggleSkillPanel() {
        _uiState.update { it.copy(skillPanelExpanded = !it.skillPanelExpanded) }
    }

    fun toggleAssistantSkill(skillId: String) {
        if (AiSkillRegistry.find(skillId) == null) return
        _uiState.update { state ->
            val selected = state.selectedSkillIds
            state.copy(
                selectedSkillIds = if (skillId in selected) selected - skillId else selected + skillId,
            )
        }
    }

    fun retryAssistantTurn(messageId: String) {
        val failed = _uiState.value.assistantMessages.firstOrNull { it.id == messageId } ?: return
        val text = failed.retryText ?: return
        sendAssistantMessage(text, failed.retrySkillIds)
    }

    fun sendAssistantMessage(
        text: String = _uiState.value.assistantDraft,
        skillIds: Set<String>? = null,
    ) {
        val state = _uiState.value
        if (state.assistantBusy || state.backendBusy) return
        val message = text.trim()
        val requestedSkills = skillIds ?: state.selectedSkillIds
        val skills = AiSkillRegistry.resolve(requestedSkills)
        if (message.isBlank() && skills.isEmpty()) return
        val conversationContext = state.assistantMessages.takeLast(12)

        val retrySkills = skills.map { it.id }.toSet()
        val userMessage = message.takeIf(String::isNotBlank)?.let {
            AssistantMessage(id = UUID.randomUUID().toString(), role = AssistantMessageRole.USER, content = it)
        }
        _uiState.update { current ->
            current.copy(
                assistantDraft = "",
                assistantMessages = current.assistantMessages + listOfNotNull(userMessage),
            )
        }

        assistantJob = viewModelScope.launch {
            _uiState.update {
                it.copy(assistantBusy = true, backendBusy = true, backendProgress = 0, backendMessage = null)
            }
            try {
                if (skills.isNotEmpty()) requireSkillApiCapabilities(skills.map { it.id }.toSet())
                val jobId = ensureBackendJob()
                if (skills.isEmpty()) {
                    runChatTurn(jobId, message, retrySkills, conversationContext)
                } else {
                    val results = mutableListOf<String>()
                    val errors = mutableListOf<String>()
                    val failedSkillIds = mutableSetOf<String>()
                    for (skill in skills) {
                        val statusId = appendToolMessage(skill.id, "${skill.displayName}: starting…")
                        try {
                            val result = executeSkill(
                                skill.id,
                                skill.backendOperation,
                                skill.executionInstructions,
                                jobId,
                                message,
                                conversationContext,
                            )
                            results += "${skill.displayName}: $result"
                            updateAssistantMessage(
                                statusId,
                                content = "${skill.displayName}: $result",
                                executionState = SkillExecutionState.COMPLETED,
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            val detail = readableBackendError(error)
                            errors += "${skill.displayName}: $detail"
                            failedSkillIds += skill.id
                            updateAssistantMessage(
                                statusId,
                                content = "${skill.displayName} failed: $detail",
                                executionState = SkillExecutionState.FAILED,
                                retryText = message,
                                retrySkillIds = setOf(skill.id),
                            )
                        }
                    }
                    val summary = buildString {
                        if (results.isNotEmpty()) append("Completed: ").append(results.joinToString("; "))
                        if (errors.isNotEmpty()) {
                            if (isNotEmpty()) append("\n")
                            append("Not completed: ").append(errors.joinToString("; "))
                        }
                    }
                    appendAssistantMessage(
                        content = summary.ifBlank { "No selected Skills could be executed." },
                        retryText = message.takeIf { errors.isNotEmpty() },
                        retrySkillIds = failedSkillIds,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "AI assistant turn failed", error)
                appendAssistantMessage(
                    content = readableBackendError(error),
                    retryText = message,
                    retrySkillIds = retrySkills,
                )
            } finally {
                _uiState.update { it.copy(assistantBusy = false, backendBusy = false) }
            }
        }
    }

    private suspend fun runChatTurn(
        jobId: String,
        message: String,
        selectedSkillIds: Set<String>,
        conversationContext: List<AssistantMessage>,
    ) {
        val statusId = appendAssistantMessage("Thinking…", running = true)
        try {
            val response = aiApi.chat(jobId, message, selectedSkillIds, conversationContext)
            val reply = if (response.has("renderId")) {
                awaitBackendRender(response, jobId)
            } else {
                response.optString("reply").ifBlank { "The backend returned no assistant message." }
            }
            updateAssistantMessage(statusId, reply, SkillExecutionState.COMPLETED)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            updateAssistantMessage(
                statusId,
                readableBackendError(error),
                SkillExecutionState.FAILED,
                message,
                selectedSkillIds,
            )
        }
    }

    private suspend fun executeSkill(
        skillId: String,
        operation: BackendSkillOperation,
        instructions: String,
        jobId: String,
        userMessage: String,
        conversationContext: List<AssistantMessage>,
    ): String = when (operation) {
        BackendSkillOperation.AUTOMATIC_SUBTITLES -> {
            val response = aiApi.automaticSubtitles(jobId, userMessage.take(1000))
            if (!response.has("renderId")) {
                error(response.optString("reply").ifBlank { "The subtitle service did not start a render." })
            }
            val renderReply = awaitBackendRender(response, jobId)
            buildString {
                append(renderReply)
                if (userMessage.isNotBlank()) append(" Subtitle instructions were sent to the configured transcription model.")
            }
        }
        BackendSkillOperation.VOICE_CLEANUP_CHAT -> {
            val prompt = buildString {
                append(instructions)
                if (userMessage.isNotBlank()) append("\n\nUser instruction: ").append(userMessage)
            }
            val response = aiApi.chat(jobId, prompt, setOf(skillId), conversationContext)
            if (!response.has("renderId")) {
                error(response.optString("reply").ifBlank { "The AI assistant did not start an audio render." })
            }
            awaitBackendRender(response, jobId)
        }
        BackendSkillOperation.SCENE_DETECTION -> {
            val threshold = sceneThresholdFromInstruction(userMessage)
            val response = aiApi.detectScenes(jobId, threshold)
            val result = response.optJSONObject("result") ?: error("Scene detection returned no tool result.")
            val scenes = result.optJSONArray("scenes") ?: error("Scene detection returned no scene list.")
            val times = (0 until scenes.length()).mapNotNull { index ->
                scenes.optJSONObject(index)?.optDouble("time")?.takeIf(Double::isFinite)?.let { (it * 1000).toLong() }
            }
            updateTimeline { it.copy(sceneMarkersMs = times) }
            val timeList = times.joinToString { "%02d:%02d".format(it / 60_000, it / 1_000 % 60) }
            buildString {
                append("Detected ${times.size} visual scene cuts")
                if (timeList.isNotBlank()) append(" at $timeList")
                append(" and added them to the timeline. Semantic highlight ranking is not available in this backend tool.")
                if (threshold != null) append(" Used cut sensitivity $threshold.")
                if (userMessage.isNotBlank() && threshold == null) append(" The backend supports cut sensitivity, not semantic moment selection; no semantic filtering was applied.")
            }
        }
    }

    private fun appendToolMessage(skillId: String, content: String): String {
        val message = AssistantMessage(
            id = UUID.randomUUID().toString(),
            role = AssistantMessageRole.TOOL,
            skillId = skillId,
            content = content,
            executionState = SkillExecutionState.RUNNING,
        )
        _uiState.update { it.copy(assistantMessages = it.assistantMessages + message) }
        return message.id
    }

    private fun appendAssistantMessage(
        content: String,
        retryText: String? = null,
        retrySkillIds: Set<String> = emptySet(),
        running: Boolean = false,
    ): String {
        val message = AssistantMessage(
            id = UUID.randomUUID().toString(),
            role = AssistantMessageRole.ASSISTANT,
            content = content,
            executionState = if (running) SkillExecutionState.RUNNING else null,
            retryText = retryText,
            retrySkillIds = retrySkillIds,
        )
        _uiState.update { it.copy(assistantMessages = it.assistantMessages + message) }
        return message.id
    }

    private fun updateAssistantMessage(
        id: String,
        content: String,
        executionState: SkillExecutionState,
        retryText: String? = null,
        retrySkillIds: Set<String> = emptySet(),
    ) {
        _uiState.update { current ->
            current.copy(assistantMessages = current.assistantMessages.map { message ->
                if (message.id == id) {
                    message.copy(
                        content = content,
                        executionState = executionState,
                        retryText = retryText,
                        retrySkillIds = retrySkillIds,
                    )
                } else message
            })
        }
    }

    private fun readableBackendError(error: Throwable): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("401") || raw.contains("unauthorized", ignoreCase = true) -> "The backend rejected the access token. Check Settings."
            raw.contains("Unable to resolve host", ignoreCase = true) || raw.contains("failed to connect", ignoreCase = true) -> "Could not reach the backend. Check its URL and that the server is running."
            raw.contains("timed out", ignoreCase = true) -> "The backend request timed out. Check the server and retry."
            else -> raw.ifBlank { "The backend request failed. Check the server and retry." }.take(500)
        }
    }

    private fun sceneThresholdFromInstruction(instruction: String): Double? {
        val explicit = Regex("(?:threshold|sensitivity)\\s*(?:of|=|:)?\\s*(0?\\.\\d+|1(?:\\.0+)?)", RegexOption.IGNORE_CASE)
            .find(instruction)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        return explicit?.takeIf { it in 0.05..0.95 }
    }

    fun testBackendConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(backendBusy = true, backendMessage = "Checking backend…") }
            runCatching { aiApi.health() }
                .onSuccess { health ->
                    val ffmpeg = health.optBoolean("ffmpegOk", false)
                    val modules = health.optJSONObject("python")?.optJSONObject("modules")
                    val aiKey = health.optBoolean("aiKey", false)
                    val info = buildList {
                        add("Connected")
                        add(if (ffmpeg) "FFmpeg ready" else "FFmpeg unavailable")
                        if (modules != null) add("Python analysis service available")
                        add(if (aiKey) "AI provider configured on server" else "No AI provider key on server")
                    }.joinToString(" • ")
                    _uiState.update { it.copy(backendBusy = false, backendConnected = true, backendMessage = info) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(backendBusy = false, backendConnected = false, backendMessage = error.message ?: "Backend connection failed.")
                    }
                }
        }
    }

    private suspend fun ensureBackendJob(): String {
        val video = _uiState.value.importedVideo ?: error("Import a video before using AI tools.")
        backendSettings.jobId()?.let { jobId ->
            if (backendSettings.jobSourceUri() == video.uri.toString()) return jobId
        }
        _uiState.update { it.copy(backendMessage = "Uploading video to AIClipStudio…") }
        val response = aiApi.uploadVideo(video.uri, video.displayName, video.videoMimeType)
        val jobId = response.optString("id").takeIf(String::isNotBlank)
            ?: error("Backend upload response did not contain a job id.")
        backendSettings.saveJobId(jobId)
        backendSettings.saveJobSourceUri(video.uri.toString())
        return jobId
    }

    private suspend fun requireSkillApiCapabilities(skillIds: Set<String>) {
        val capabilities = aiApi.health().optJSONObject("capabilities")
        if (capabilities?.optBoolean("androidSkills") != true) {
            error("This AIClipStudio backend does not support selectable Skills yet. Update the server to the version documented in AIClipStudio-skills-api.md.")
        }
        val subtitlesSelected = "automatic_subtitles" in skillIds
        if (subtitlesSelected && capabilities.optBoolean("captionInstructions") != true) {
            error("This AIClipStudio backend does not support subtitle instructions yet. Update the server to the version documented in AIClipStudio-skills-api.md.")
        }
    }

    private suspend fun awaitBackendRender(response: org.json.JSONObject, jobId: String): String {
        val renderId = response.optString("renderId").takeIf(String::isNotBlank)
        if (renderId == null) {
            val reply = response.optString("reply").ifBlank { "Backend returned no render job." }
            _uiState.update { it.copy(backendMessage = reply) }
            return reply
        }
        repeat(900) {
            val result = aiApi.render(jobId, renderId)
            val status = result.optString("status")
            val progress = result.optInt("progress", 0).coerceIn(0, 100)
            _uiState.update { it.copy(backendProgress = progress, backendMessage = "AIClipStudio render: $status ($progress%)") }
            when (status) {
                "done" -> {
                    val reply = result.optString("reply").ifBlank { result.optString("summary").ifBlank { "Backend render completed." } }
                    val media = result.optJSONObject("media")
                    val relativeUrl = media?.optString("url")?.takeIf(String::isNotBlank)
                    if (relativeUrl != null) {
                        val outputUri = aiApi.downloadRenderedMedia(
                            relativeUrl,
                            media.optString("name").ifBlank { "AIClipStudio output.mp4" },
                        )
                        val imported = mediaRepository.loadVideo(outputUri, persistedGrant = false)
                        _uiState.update {
                            it.copy(
                                importedVideo = imported,
                                editorTimeline = EditorTimeline.fromSource(imported.durationMs ?: 0L),
                                playheadMs = 0L,
                                backendMessage = reply,
                            )
                        }
                        mediaPreferences.edit().putString(KEY_IMPORTED_VIDEO_URI, outputUri.toString()).apply()
                    } else {
                        _uiState.update { it.copy(backendMessage = reply) }
                    }
                    return reply
                }
                "failed", "lost" -> error(result.optString("reply").ifBlank { "AIClipStudio render $status." })
            }
            kotlinx.coroutines.delay(1_000L)
        }
        error("AIClipStudio render timed out while polling status.")
    }

    fun dismissEditorMessage() {
        _uiState.update { it.copy(editorMessage = null) }
    }

    /** Restores a timeline snapshot captured before a destructive edit, for Undo. */
    fun restoreTimeline(snapshot: EditorTimelineState) {
        _uiState.update {
            it.copy(
                editorTimeline = snapshot,
                playheadMs = snapshot.playheadMs,
                seekRequestId = it.seekRequestId + 1,
            )
        }
    }

    private fun updateTimeline(transform: (EditorTimelineState) -> EditorTimelineState) {
        _uiState.update { current ->
            val timeline = transform(current.editorTimeline)
            current.copy(editorTimeline = timeline, playheadMs = timeline.playheadMs)
        }
    }
}
