package com.example.nativestarter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.nativestarter.R

/**
 * Increment / decrement / reset controls, separated from display so each
 * screen can reuse them.
 */
@Composable
fun CounterControls(
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onDecrement,
                modifier = Modifier.widthIn(min = 96.dp),
            ) {
                Text(stringResource(R.string.decrement))
            }
            FilledTonalButton(
                onClick = onIncrement,
                modifier = Modifier.widthIn(min = 96.dp),
            ) {
                Text(stringResource(R.string.increment))
            }
        }
        Button(
            onClick = onReset,
            modifier = Modifier.widthIn(min = 204.dp),
        ) {
            Text(stringResource(R.string.reset))
        }
    }
}
