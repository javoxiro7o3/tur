package uz.uydanimabor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.uydanimabor.ui.AppRoot
import uz.uydanimabor.ui.theme.UydaTheme
import uz.uydanimabor.ui.theme.isDark

/** Yagona Activity (MVVM, Compose Navigation). */
class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val s = settings ?: return@setContent // DataStore oʻqilguncha (bir necha ms)
            val dark = isDark(s.theme, isSystemInDarkTheme())
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            UydaTheme(s.theme) {
                AppRoot(vm, s)
            }
        }
    }
}
