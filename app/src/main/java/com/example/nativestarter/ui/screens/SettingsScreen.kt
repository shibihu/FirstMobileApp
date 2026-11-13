package com.example.nativestarter.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.nativestarter.R
import com.example.nativestarter.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val state = viewModel.uiState.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            modifier = Modifier.fillMaxWidth(),
        )
        AppearanceSection(
            isDarkTheme = state.isDarkTheme,
            onDarkThemeChange = viewModel::setDarkTheme,
        )
        BackendSection(
            baseUrl = state.backendBaseUrl,
            tokenConfigured = state.backendTokenConfigured,
            connected = state.backendConnected,
            busy = state.backendBusy,
            message = state.backendMessage,
            onSave = viewModel::saveBackendSettings,
            onClearToken = viewModel::clearBackendToken,
            onTest = viewModel::testBackendConnection,
        )
        AboutSection()
    }
}

@Composable
private fun AppearanceSection(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
) {
    SectionCard(title = stringResource(R.string.appearance_title)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.dark_mode))
            Switch(checked = isDarkTheme, onCheckedChange = onDarkThemeChange)
        }
    }
}

@Composable
private fun BackendSection(
    baseUrl: String,
    tokenConfigured: Boolean,
    connected: Boolean?,
    busy: Boolean,
    message: String?,
    onSave: (String, String) -> Unit,
    onClearToken: () -> Unit,
    onTest: () -> Unit,
) {
    var urlInput by remember(baseUrl) { mutableStateOf(baseUrl) }
    var tokenInput by remember { mutableStateOf("") }
    var confirmClearToken by remember { mutableStateOf(false) }
    SectionCard(title = "AIClipStudio backend") {
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Backend URL") },
            placeholder = { Text("https://studio.example.com or http://localhost:3000") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            label = { Text(if (tokenConfigured) "Replace server access token" else "Server access token (optional)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Text(
            if (tokenConfigured) "Access token stored encrypted with Android Keystore." else "No access token stored.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(urlInput, tokenInput); tokenInput = "" }) { Text("Save") }
            OutlinedButton(onClick = onTest, enabled = !busy && baseUrl.isNotBlank()) {
                Text(if (busy) "Checking…" else "Test connection")
            }
            if (tokenConfigured) {
                TextButton(onClick = { confirmClearToken = true }) { Text("Clear token") }
            }
        }
        message?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (connected == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "Remote backends must use HTTPS. Local HTTP is limited to http://localhost:3000, http://127.0.0.1:3000, http://localhost:10000, and http://127.0.0.1:10000.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "The backend needs its own AI provider configuration. Provider API keys are never stored in this app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (confirmClearToken) {
        AlertDialog(
            onDismissRequest = { confirmClearToken = false },
            title = { Text("Clear access token?") },
            text = {
                Text(
                    "This deletes the saved server access token from this phone. " +
                        "Protected backends will reject requests until you enter the token again.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearToken = false
                        onClearToken()
                    },
                ) { Text("Clear token") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearToken = false }) { Text("Keep token") }
            },
        )
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val versionName by remember { mutableStateOf(resolveVersionName(context)) }

    SectionCard(title = stringResource(R.string.about_title)) {
        LabeledRow(label = stringResource(R.string.app_name))
        LabeledRow(label = stringResource(R.string.version_label), value = versionName)
    }
}

@Composable
private fun LabeledRow(label: String, value: String? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun resolveVersionName(context: Context): String {
    val unknown = context.getString(R.string.version_unknown)
    return try {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName ?: unknown
    } catch (_: PackageManager.NameNotFoundException) {
        unknown
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
