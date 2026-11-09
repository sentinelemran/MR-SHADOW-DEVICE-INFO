package com.mrshadow.deviceinfo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settings = AppSettings(getSharedPreferences("settings", MODE_PRIVATE))
        setContent {
            AppTheme(settings.theme) {
                MainApp(settings)
            }
        }
    }
}

@Composable
fun MainApp(st: AppSettings) {
    val vm: DeviceVm = viewModel()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val items = listOf(
        Triple(Icons.Default.Home, "Home", "হোম"),
        Triple(Icons.Default.Apps, "Apps", "অ্যাপস"),
        Triple(Icons.Default.Person, "Developer", "ডেভেলপার"),
        Triple(Icons.Default.Settings, "Settings", "সেটিংস")
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { i, item ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(item.first, contentDescription = null) },
                        label = { Text(tr(st.bn, item.second, item.third)) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            Crossfade(targetState = tab, label = "tab") { t ->
                when (t) {
                    0 -> HomeScreen(vm, st)
                    1 -> AppsScreen(st)
                    2 -> DeveloperScreen(vm, st)
                    else -> SettingsScreen(st)
                }
            }
        }
    }
}
