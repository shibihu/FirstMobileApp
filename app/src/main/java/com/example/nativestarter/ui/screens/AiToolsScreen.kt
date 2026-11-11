package com.example.nativestarter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.nativestarter.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiToolsScreen(viewModel: AppViewModel) {
    val state = viewModel.uiState.collectAsState().value
    val canRun = state.backendBaseUrl.isNotBlank() && state.importedVideo != null && !state.backendBusy

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TopAppBar(title = { Text("AI Studio") }, modifier = Modifier.fillMaxWidth())
        Text(
            when {
                state.backendBaseUrl.isBlank() -> "Set a remote HTTPS or supported local loopback backend URL in Settings before running AI tools."
                state.importedVideo == null -> "Import a video before running backend analysis."
                else -> "The selected video is uploaded to your configured AIClipStudio server when an action runs."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ToolAction(
            title = "Automatic subtitles",
            detail = "Calls POST /api/captions and polls the returned render job.",
            icon = Icons.Filled.Mic,
            enabled = canRun,
            onClick = viewModel::generateAutomaticSubtitles,
        )
        ToolAction(
            title = "Voice clean-up",
            detail = "Requests voice clean-up through the reference POST /api/chat route.",
            icon = Icons.Filled.SmartToy,
            enabled = canRun,
            onClick = viewModel::cleanVoiceAudio,
        )
        ToolAction(
            title = "Scene detection",
            detail = "Calls POST /api/tool with the detect_scenes tool and adds returned markers to the timeline.",
            icon = Icons.Filled.AutoAwesome,
            enabled = canRun,
            onClick = viewModel::detectScenes,
        )
        if (state.backendBusy) {
            LinearProgressIndicator(
                progress = { state.backendProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.backendMessage?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = it,
                    modifier = Modifier.padding(16.dp),
                    color = if (state.backendConnected == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (state.editorTimeline.sceneMarkersMs.isNotEmpty()) {
            Text("Scene markers: ${state.editorTimeline.sceneMarkersMs.joinToString { formatTime(it) }}")
        }
    }
}

@Composable
private fun ToolAction(
    title: String,
    detail: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onClick, enabled = enabled) { Text("Run") }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val seconds = timeMs / 1000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
