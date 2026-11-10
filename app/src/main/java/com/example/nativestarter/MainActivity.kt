package com.example.nativestarter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nativestarter.navigation.AppRoot
import com.example.nativestarter.theme.NativeStarterTheme
import com.example.nativestarter.viewmodel.AppViewModel

/**
 * Single-activity entry point. All UI is Compose; navigation lives in [AppRoot].
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            val state = viewModel.uiState.collectAsState().value

            NativeStarterTheme(darkTheme = state.isDarkTheme) {
                AppRoot(viewModel = viewModel)
            }
        }
    }
}
