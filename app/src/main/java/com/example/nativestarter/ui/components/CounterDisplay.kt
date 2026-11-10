package com.example.nativestarter.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Large numeric display for the current count.
 */
@Composable
fun CounterDisplay(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            fontSize = 72.sp,
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
        )
    }
}
