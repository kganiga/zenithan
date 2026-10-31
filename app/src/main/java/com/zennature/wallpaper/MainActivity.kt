package com.zennature.wallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zennature.wallpaper.data.ThemeMode
import com.zennature.wallpaper.ui.MainViewModel
import com.zennature.wallpaper.ui.ZenAppRoot
import com.zennature.wallpaper.ui.theme.ZenTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val main: MainViewModel = viewModel()
            val theme by main.store.theme.collectAsStateWithLifecycle()
            val dark = theme == ThemeMode.DARK
            DisposableEffect(dark) {
                val bar = if (dark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
                onDispose { }
            }
            ZenTheme(dark = dark) {
                ZenAppRoot(main)
            }
        }
    }
}
