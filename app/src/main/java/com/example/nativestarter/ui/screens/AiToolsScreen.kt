package com.example.nativestarter.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nativestarter.domain.assistant.AiSkill
import com.example.nativestarter.domain.assistant.AiSkillRegistry
import com.example.nativestarter.domain.assistant.AssistantMessage
import com.example.nativestarter.domain.assistant.AssistantMessageRole
import com.example.nativestarter.domain.assistant.SkillExecutionState
import com.example.nativestarter.domain.assistant.SkillIcon
import com.example.nativestarter.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiToolsScreen(viewModel: AppViewModel) {
    val state = viewModel.uiState.collectAsState().value
    val listState = rememberLazyListState()
    val canSend = !state.assistantBusy && !state.backendBusy &&
        state.backendBaseUrl.isNotBlank() && state.importedVideo != null &&
        (state.assistantDraft.isNotBlank() || state.selectedSkillIds.isNotEmpty())

    LaunchedEffect(state.assistantMessages.size, state.assistantMessages.lastOrNull()?.content) {
        if (state.assistantMessages.isNotEmpty()) listState.animateScrollToItem(state.assistantMessages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("AI Studio")
                    Text(
                        text = when {
                            state.backendBaseUrl.isBlank() -> "Configure a backend in Settings"
                            state.importedVideo == null -> "Ready for a video"
                            else -> "${state.importedVideo.displayName} · ${formatDuration(state.importedVideo.durationMs)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            },
            // The app shell already applies the status bar inset to this content.
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
        if (state.backendBusy) {
            LinearProgressIndicator(
                progress = { state.backendProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.assistantMessages.isEmpty()) {
                item(key = "empty") {
                    EmptyConversation(
                        backendConfigured = state.backendBaseUrl.isNotBlank(),
                        hasVideo = state.importedVideo != null,
                    )
                }
            }
            items(state.assistantMessages, key = AssistantMessage::id) { message ->
                ChatMessageBubble(
                    message = message,
                    onRetry = { viewModel.retryAssistantTurn(message.id) },
                )
            }
            if (state.assistantBusy && state.assistantMessages.none { it.executionState == SkillExecutionState.RUNNING }) {
                item(key = "thinking") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Working…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        ChatComposer(
            draft = state.assistantDraft,
            selectedSkills = AiSkillRegistry.resolve(state.selectedSkillIds),
            busy = state.assistantBusy || state.backendBusy,
            enabled = canSend,
            skillPanelExpanded = state.skillPanelExpanded,
            onDraftChange = viewModel::setAssistantDraft,
            onToggleSkills = viewModel::toggleSkillPanel,
            onRemoveSkill = viewModel::toggleAssistantSkill,
            onSend = { viewModel.sendAssistantMessage() },
        )
    }
}

@Composable
private fun EmptyConversation(backendConfigured: Boolean, hasVideo: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 42.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
        Text("What would you like to do?", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text = when {
                !backendConfigured -> "Set the AIClipStudio backend URL in Settings to begin."
                !hasVideo -> "Import a video first, then choose a Skill or ask a question."
                else -> "Choose Skills below, then describe what you want in your own words."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (hasVideo) {
            Text("Try: “Clean up the voice audio and explain what changed.”", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: AssistantMessage,
    onRetry: () -> Unit,
) {
    val user = message.role == AssistantMessageRole.USER
    val tool = message.role == AssistantMessageRole.TOOL
    val alignment = if (user) Alignment.End else Alignment.Start
    val background = when {
        user -> MaterialTheme.colorScheme.primaryContainer
        message.executionState == SkillExecutionState.FAILED -> MaterialTheme.colorScheme.errorContainer
        tool -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Surface(
            color = background,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(if (user) 0.88f else 0.96f),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                if (tool) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        message.skillId?.let(AiSkillRegistry::find)?.let { Icon(skillIcon(it.icon), contentDescription = null, modifier = Modifier.size(16.dp)) }
                        Text(
                            message.skillId?.let(AiSkillRegistry::find)?.displayName ?: "Tool",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        if (message.executionState == SkillExecutionState.RUNNING) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else if (message.executionState == SkillExecutionState.COMPLETED) {
                            Icon(Icons.Filled.Check, contentDescription = "Completed", modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(message.content, style = MaterialTheme.typography.bodyMedium)
                if (message.retryText != null && message.executionState == SkillExecutionState.FAILED) {
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatComposer(
    draft: String,
    selectedSkills: List<AiSkill>,
    busy: Boolean,
    enabled: Boolean,
    skillPanelExpanded: Boolean,
    onDraftChange: (String) -> Unit,
    onToggleSkills: () -> Unit,
    onRemoveSkill: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Skills", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = onToggleSkills) {
                    Text(if (skillPanelExpanded) "Hide" else "Choose")
                    Icon(if (skillPanelExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess, contentDescription = null)
                }
            }
            AnimatedVisibility(visible = skillPanelExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AiSkillRegistry.skills.forEach { skill ->
                        val selected = selectedSkills.any { it.id == skill.id }
                        SkillSelectionRow(skill = skill, selected = selected, onClick = { onRemoveSkill(skill.id) })
                    }
                }
            }
            if (selectedSkills.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    selectedSkills.forEach { skill ->
                        SelectedSkillChip(
                            label = skill.displayName,
                            icon = skillIcon(skill.icon),
                            onRemove = { onRemoveSkill(skill.id) },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { value ->
                        onDraftChange(
                            if (selectedSkills.any { it.id == "automatic_subtitles" }) value.take(1000)
                            else value,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (selectedSkills.any { it.id == "automatic_subtitles" }) {
                                "Optional subtitle instructions (up to 1,000 characters)…"
                            } else {
                                "Ask about your video…"
                            },
                        )
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                )
                IconButton(onClick = onSend, enabled = enabled, modifier = Modifier.size(52.dp)) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SelectedSkillChip(
    label: String,
    icon: ImageVector,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(
                    onClickLabel = "Remove $label",
                    role = Role.Button,
                    onClick = onRemove,
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
            Icon(
                Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SkillSelectionRow(
    skill: AiSkill,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(34.dp).background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(skillIcon(skill.icon), contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(skill.displayName, style = MaterialTheme.typography.bodyMedium)
                Text(skill.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                skill.limitation?.let { limitation ->
                    Text(limitation, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun skillIcon(icon: SkillIcon): ImageVector = when (icon) {
    SkillIcon.MICROPHONE -> Icons.Filled.Mic
    SkillIcon.VOICE -> Icons.Filled.SmartToy
    SkillIcon.SPARKLE -> Icons.Filled.AutoAwesome
}

private fun formatDuration(durationMs: Long?): String {
    if (durationMs == null) return "Duration unknown"
    val seconds = durationMs / 1000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
