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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        WorkflowSection()
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
private fun WorkflowSection() {
    SectionCard(title = "Editor workflow") {
        LabeledRow(label = "Video export", value = "Not available yet")
        LabeledRow(label = "AI provider", value = "Not configured")
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
