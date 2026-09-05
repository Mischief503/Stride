package com.richie.stride

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.richie.stride.data.ThemeMode
import com.richie.stride.ui.MainViewModel
import com.richie.stride.ui.navigation.StrideNavHost
import com.richie.stride.ui.theme.StrideTheme
import com.richie.stride.util.SimpleViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        SimpleViewModelFactory { app -> MainViewModel(app) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsState()
            val darkTheme = when (state.settings.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            StrideTheme(accent = state.settings.accent, darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StrideNavHost(viewModel = viewModel)
                }
            }
        }
    }
}
