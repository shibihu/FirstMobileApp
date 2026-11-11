package com.example.nativestarter.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nativestarter.R
import com.example.nativestarter.domain.editor.SubtitleCue
import com.example.nativestarter.ui.components.VideoPreview
import com.example.nativestarter.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(viewModel: AppViewModel) {
    val state = viewModel.uiState.collectAsState().value
    val video = state.importedVideo
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importAudio)
    }
    val srtPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importSrt)
    }
    val srtSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { uri ->
        uri?.let(viewModel::exportSrt)
    }
    val mp4Saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
        uri?.let(viewModel::exportVideo)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopAppBar(title = { Text("Editor") }, modifier = Modifier.fillMaxWidth())

        if (video == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("No video selected", style = MaterialTheme.typography.titleMedium)
                    Text("Import a video from Media to create a timeline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            VideoPreview(
                video = video,
                viewModel = viewModel,
                playheadMs = state.playheadMs,
                seekRequestId = state.seekRequestId,
                timeline = state.editorTimeline,
                isPlaying = state.isPlaying,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { mp4Saver.launch("edited-video.mp4") },
                    enabled = !state.isExporting && state.editorTimeline.videoSegments.isNotEmpty(),
                ) {
                    Text(if (state.isExporting) "Exporting…" else "Export MP4")
                }
                if (state.isExporting) {
                    OutlinedButton(onClick = viewModel::cancelExport) { Text("Cancel") }
                }
            }
            if (state.isExporting) {
                LinearProgressIndicator(
                    progress = { state.exportProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Rendering ${state.exportProgress}%", style = MaterialTheme.typography.labelMedium)
            }
            state.exportError?.let { error ->
                TextButton(onClick = viewModel::clearExportError) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                text = "${video.displayName}  •  ${video.videoMimeType ?: "video codec unknown"}${video.audioMimeTypes.joinToString(prefix = if (video.audioMimeTypes.isEmpty()) "" else "  •  ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TimelinePanel(
                state = state.editorTimeline,
                onSeek = viewModel::seekTimeline,
                onSelect = viewModel::selectSegment,
                onTrim = viewModel::trimSelectedSegment,
                onSplit = viewModel::splitAtPlayhead,
                onDelete = viewModel::deleteSelectedSegment,
            )
            if (state.editorTimeline.sceneMarkersMs.isNotEmpty()) {
                Text(
                    "Scene markers  ${state.editorTimeline.sceneMarkersMs.joinToString("  •  ") { formatDuration(it) }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            AudioPanel(
                clips = state.editorTimeline.audioSegments,
                onImport = { audioPicker.launch(arrayOf("audio/*")) },
                onVolume = viewModel::setAudioVolume,
                onDelete = viewModel::deleteAudio,
            )
            SubtitlePanel(
                cues = state.editorTimeline.subtitleCues,
                durationMs = state.editorTimeline.durationMs,
                playheadMs = state.playheadMs,
                onAdd = viewModel::addSubtitle,
                onUpdate = viewModel::updateSubtitle,
                onDelete = viewModel::deleteSubtitle,
                onImport = { srtPicker.launch(arrayOf("application/x-subrip", "text/plain", "application/octet-stream")) },
                onExport = { srtSaver.launch("subtitles.srt") },
            )
            state.editorMessage?.let { message ->
                TextButton(onClick = viewModel::dismissEditorMessage) { Text(message) }
            }
        }
    }
}

@Composable
private fun TimelinePanel(
    state: com.example.nativestarter.domain.editor.EditorTimelineState,
    onSeek: (Long) -> Unit,
    onSelect: (String) -> Unit,
    onTrim: (Long, Long) -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
) {
    val duration = state.durationMs
    val selected = state.selectedSegment
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Video timeline", style = MaterialTheme.typography.titleMedium)
                Text("${formatDuration(state.playheadMs)} / ${formatDuration(duration)}")
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.videoSegments.forEachIndexed { index, segment ->
                    val isSelected = segment.id == state.selectedSegmentId
                    Button(
                        onClick = { onSelect(segment.id) },
                        modifier = Modifier.size(width = 112.dp, height = 48.dp),
                    ) {
                        Text("${index + 1} • ${formatDuration(segment.durationMs)}")
                    }
                }
            }
            TimeRuler(durationMs = duration)
            if (duration > 0) {
                Slider(
                    value = state.playheadMs.toFloat().coerceIn(0f, duration.toFloat()),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..duration.toFloat(),
                )
            }
            if (selected != null) {
                var trimStart by remember(selected.id, selected.sourceStartMs) {
                    mutableFloatStateOf(selected.sourceStartMs.toFloat())
                }
                var trimEnd by remember(selected.id, selected.sourceEndMs) {
                    mutableFloatStateOf(selected.sourceEndMs.toFloat())
                }
                Text("Selected clip • source range ${formatDuration(selected.sourceStartMs)}–${formatDuration(selected.sourceEndMs)}")
                Text("Trim start ${formatDuration(trimStart.toLong())}", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = trimStart.coerceIn(0f, (trimEnd - 100f).coerceAtLeast(0f)),
                    onValueChange = { trimStart = it },
                    valueRange = 0f..state.sourceDurationMs.coerceAtLeast(100L).toFloat(),
                    onValueChangeFinished = { onTrim(trimStart.toLong(), trimEnd.toLong()) },
                )
                Text("Trim end ${formatDuration(trimEnd.toLong())}", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = trimEnd.coerceIn((trimStart + 100f).coerceAtMost(state.sourceDurationMs.toFloat()), state.sourceDurationMs.toFloat()),
                    onValueChange = { trimEnd = it },
                    valueRange = 100f..state.sourceDurationMs.coerceAtLeast(100L).toFloat(),
                    onValueChangeFinished = { onTrim(trimStart.toLong(), trimEnd.toLong()) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onSplit, enabled = state.playheadMs > 0L && state.playheadMs < duration) {
                        Text("Split at playhead")
                    }
                    OutlinedButton(onClick = onDelete) { Text("Delete clip") }
                }
            }
        }
    }
}

@Composable
private fun TimeRuler(durationMs: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(0, 25, 50, 75, 100).forEach { percent ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(width = 1.dp, height = 8.dp).background(MaterialTheme.colorScheme.outline))
                Text(
                    formatDuration(durationMs * percent / 100),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AudioPanel(
    clips: List<com.example.nativestarter.domain.editor.AudioSegment>,
    onImport: () -> Unit,
    onVolume: (String, Float) -> Unit,
    onDelete: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Audio track", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onImport) { Text("Import audio") }
            }
            if (clips.isEmpty()) {
                Text("No audio clips", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            clips.forEach { clip ->
                Text("${clip.name} • ${formatDuration(clip.durationMs)} • starts ${formatDuration(clip.timelineStartMs)}")
                Slider(value = clip.volume, onValueChange = { onVolume(clip.id, it) }, valueRange = 0f..2f)
                TextButton(onClick = { onDelete(clip.id) }) { Text("Remove ${clip.name}") }
            }
        }
    }
}

@Composable
private fun SubtitlePanel(
    cues: List<SubtitleCue>,
    durationMs: Long,
    playheadMs: Long,
    onAdd: (String) -> Unit,
    onUpdate: (SubtitleCue) -> Unit,
    onDelete: (String) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    var newText by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Subtitle track", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onImport) { Text("Import SRT") }
                OutlinedButton(onClick = onExport, enabled = cues.isNotEmpty()) { Text("Export SRT") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = newText,
                    onValueChange = { newText = it },
                    label = { Text("Subtitle text") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Button(onClick = { onAdd(newText); newText = "" }, enabled = newText.isNotBlank() && playheadMs < durationMs) {
                    Text("Add")
                }
            }
            cues.forEach { cue ->
                SubtitleCueEditor(cue = cue, durationMs = durationMs, onUpdate = onUpdate, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun SubtitleCueEditor(
    cue: SubtitleCue,
    durationMs: Long,
    onUpdate: (SubtitleCue) -> Unit,
    onDelete: (String) -> Unit,
) {
    var start by remember(cue.id, cue.startMs) { mutableFloatStateOf(cue.startMs.toFloat()) }
    var end by remember(cue.id, cue.endMs) { mutableFloatStateOf(cue.endMs.toFloat()) }
    TextField(
        value = cue.text,
        onValueChange = { onUpdate(cue.copy(text = it)) },
        label = { Text("Cue ${formatDuration(cue.startMs)}–${formatDuration(cue.endMs)}") },
        modifier = Modifier.fillMaxWidth(),
    )
    Slider(
        value = start.coerceIn(0f, (end - 1f).coerceAtLeast(0f)),
        onValueChange = { start = it },
        valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
        onValueChangeFinished = { onUpdate(cue.copy(startMs = start.toLong(), endMs = end.toLong())) },
    )
    Slider(
        value = end.coerceIn((start + 1f).coerceAtMost(durationMs.toFloat()), durationMs.coerceAtLeast(1L).toFloat()),
        onValueChange = { end = it },
        valueRange = 1f..durationMs.coerceAtLeast(1L).toFloat(),
        onValueChangeFinished = { onUpdate(cue.copy(startMs = start.toLong(), endMs = end.toLong())) },
    )
    TextButton(onClick = { onDelete(cue.id) }) { Text("Delete cue") }
}

private fun formatDuration(timeMs: Long): String {
    val totalSeconds = timeMs.coerceAtLeast(0L) / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
