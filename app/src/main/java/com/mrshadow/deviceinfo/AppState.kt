package com.mrshadow.deviceinfo

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

fun tr(bn: Boolean, en: String, bnText: String): String = if (bn) bnText else en

class AppSettings(private val sp: SharedPreferences) {
    var theme by mutableIntStateOf(sp.getInt("theme", 1)) // 0 system, 1 dark, 2 light
        private set
    var bn by mutableStateOf(sp.getBoolean("bn", true))
        private set
    var showUnavailable by mutableStateOf(sp.getBoolean("showUnavailable", true))
        private set
    var showAdvanced by mutableStateOf(sp.getBoolean("showAdvanced", true))
        private set
    var autoRefresh by mutableStateOf(sp.getBoolean("autoRefresh", false))
        private set

    fun updateTheme(v: Int) { theme = v; sp.edit().putInt("theme", v).apply() }
    fun updateBn(v: Boolean) { bn = v; sp.edit().putBoolean("bn", v).apply() }
    fun updateShowUnavailable(v: Boolean) { showUnavailable = v; sp.edit().putBoolean("showUnavailable", v).apply() }
    fun updateShowAdvanced(v: Boolean) { showAdvanced = v; sp.edit().putBoolean("showAdvanced", v).apply() }
    fun updateAutoRefresh(v: Boolean) { autoRefresh = v; sp.edit().putBoolean("autoRefresh", v).apply() }
}

class DeviceVm(app: Application) : AndroidViewModel(app) {
    var sections by mutableStateOf<List<Section>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            sections = withContext(Dispatchers.IO) { DeviceInfoCollector.collect(getApplication()) }
            loading = false
        }
    }
}

data class AppItem(
    val name: String,
    val description: String,
    val category: String,
    val downloadUrl: String,
    val websiteUrl: String,
    val version: String,
    val visible: Boolean
)

object AppsRepository {
    // Stage 1: local JSON (assets/apps.json). Remote config comes in a later stage.
    fun load(ctx: Context): List<AppItem> = try {
        val text = ctx.assets.open("apps.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            AppItem(
                o.optString("name"), o.optString("description"), o.optString("category"),
                o.optString("downloadUrl"), o.optString("websiteUrl"), o.optString("version"),
                o.optBoolean("visible", true)
            )
        }.filter { it.visible }
    } catch (e: Exception) { emptyList() }
}
