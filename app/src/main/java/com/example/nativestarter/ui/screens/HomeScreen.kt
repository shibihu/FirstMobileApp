package com.example.nativestarter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nativestarter.R
import com.example.nativestarter.ui.components.CounterControls
import com.example.nativestarter.ui.components.CounterDisplay
import com.example.nativestarter.viewmodel.AppViewModel

/**
 * Home screen: welcome message, app title, interactive counter.
 * Composes [CounterDisplay] and [CounterControls] from the shared components.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val state = viewModel.uiState.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.welcome_message),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CounterDisplay(count = state.counter)
            CounterControls(
                onIncrement = viewModel::increment,
                onDecrement = viewModel::decrement,
                onReset = viewModel::reset,
            )
        }
    }
}
