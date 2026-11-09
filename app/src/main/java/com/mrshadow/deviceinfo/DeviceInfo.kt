package com.mrshadow.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import android.view.Display
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

data class Field(val label: String, val value: String?, val advanced: Boolean = false)

data class Section(
    val titleEn: String,
    val titleBn: String,
    val fields: List<Field>,
    val progress: Float? = null
)

object DeviceInfoCollector {

    fun collect(ctx: Context): List<Section> = listOf(
        safe("Basic Device", "মূল ডিভাইস তথ্য") { basic(ctx) },
        safe("Android & OS", "অ্যান্ড্রয়েড ও ওএস") { os() },
        safe("Processor", "প্রসেসর") { cpu() },
        safe("RAM", "র‍্যাম") { ram(ctx) },
        safe("Storage", "স্টোরেজ") { storage(ctx) },
        safe("Battery", "ব্যাটারি") { battery(ctx) },
        safe("Display", "ডিসপ্লে") { display(ctx) },
        safe("Camera", "ক্যামেরা") { camera(ctx) },
        safe("Network", "নেটওয়ার্ক") { network(ctx) }
    )

    private fun safe(en: String, bn: String, block: () -> Section): Section =
        try { block() } catch (e: Exception) { Section(en, bn, listOf(Field("Status", null))) }

    private fun s(v: String?): String? =
        v?.trim()?.takeIf { it.isNotEmpty() && !it.equals("unknown", true) }

    private fun prop(key: String): String? = try {
        val c = Class.forName("android.os.SystemProperties")
        (c.getMethod("get", String::class.java).invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
    } catch (e: Throwable) { null }

    private fun basic(ctx: Context) = Section("Basic Device", "মূল ডিভাইস তথ্য", listOf(
        Field("Manufacturer", s(Build.MANUFACTURER)),
        Field("Brand", s(Build.BRAND)),
        Field("Model", s(Build.MODEL)),
        Field("Device codename", s(Build.DEVICE)),
        Field("Product", s(Build.PRODUCT)),
        Field("Board", s(Build.BOARD)),
        Field("Hardware", s(Build.HARDWARE)),
        Field("Build number", s(Build.DISPLAY)),
        Field(
            "Device type (estimated)",
            if (ctx.resources.configuration.smallestScreenWidthDp >= 600) "Tablet" else "Phone"
        )
    ))

    private fun rootStatus(): String {
        val paths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/sbin/su",
            "/vendor/bin/su", "/su/bin/su", "/data/local/xbin/su", "/data/local/bin/su"
        )
        val found = paths.any { File(it).exists() }
        val testKeys = Build.TAGS?.contains("test-keys") == true
        return when {
            found -> "Root indicators found (su binary)"
            testKeys -> "Possible (test-keys build)"
            else -> "No root indicators found (basic check, may be hidden)"
        }
    }

    private fun customRom(): String? = when {
        prop("ro.lineage.version") != null -> "LineageOS " + prop("ro.lineage.version")
        prop("ro.miui.ui.version.name") != null -> "MIUI " + prop("ro.miui.ui.version.name")
        prop("ro.build.version.oneui") != null -> "One UI (Samsung)"
        prop("ro.crdroid.version") != null -> "crDroid " + prop("ro.crdroid.version")
        else -> null
    }

    private fun os() = Section("Android & OS", "অ্যান্ড্রয়েড ও ওএস", listOf(
        Field("Android version", s(Build.VERSION.RELEASE)),
        Field("SDK version", Build.VERSION.SDK_INT.toString()),
        Field("OS name", "Android"),
        Field("Custom ROM (if detectable)", customRom()),
        Field("Build ID", s(Build.ID)),
        Field("Build fingerprint", s(Build.FINGERPRINT), advanced = true),
        Field("Security patch", s(Build.VERSION.SECURITY_PATCH)),
        Field("Kernel version", s(System.getProperty("os.version"))),
        Field("Bootloader version", s(Build.BOOTLOADER)),
        Field("Baseband version", s(Build.getRadioVersion())),
        Field("CPU ABI", Build.SUPPORTED_ABIS.joinToString(", ").ifEmpty { null }),
        Field("Root status", rootStatus()),
        Field("Bootloader lock status", null)
    ))

    private fun maxCpuFreq(cores: Int): String? {
        var max = 0L
        for (i in 0 until cores) {
            try {
                val v = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                    .readText().trim().toLongOrNull() ?: continue
                if (v > max) max = v
            } catch (e: Exception) { /* not readable */ }
        }
        return if (max > 0) String.format(Locale.US, "%.2f GHz", max / 1_000_000.0) else null
    }

    private fun cpu(): Section {
        val cores = Runtime.getRuntime().availableProcessors()
        val socMan = if (Build.VERSION.SDK_INT >= 31) s(Build.SOC_MANUFACTURER) else null
        val socModel = if (Build.VERSION.SDK_INT >= 31) s(Build.SOC_MODEL) else null
        return Section("Processor", "প্রসেসর", listOf(
            Field("CPU manufacturer", socMan),
            Field("Chipset / SoC model", socModel ?: s(Build.HARDWARE)),
            Field("Architecture / ABI", Build.SUPPORTED_ABIS.firstOrNull()),
            Field("CPU cores", cores.toString()),
            Field("Max frequency", maxCpuFreq(cores)),
            Field("GPU", null)
        ))
    }

    private fun ram(ctx: Context): Section {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val total = mi.totalMem
        val avail = mi.availMem
        val used = total - avail
        val gb = total / 1_073_741_824.0
        val cls = when {
            gb < 3 -> "Entry level"
            gb < 6 -> "Mid range"
            gb < 10 -> "High"
            else -> "Flagship"
        }
        return Section("RAM", "র‍্যাম", listOf(
            Field("Total RAM (as seen by Android, slightly below physical)", Formatter.formatFileSize(ctx, total)),
            Field("Available RAM (free now)", Formatter.formatFileSize(ctx, avail)),
            Field("Used RAM", Formatter.formatFileSize(ctx, used)),
            Field("Memory class (estimated)", cls)
        ), progress = if (total > 0) used.toFloat() / total else null)
    }

    private fun storage(ctx: Context): Section {
        val st = StatFs(Environment.getDataDirectory().path)
        val total = st.totalBytes
        val free = st.availableBytes
        val used = total - free
        val fields = mutableListOf(
            Field("Internal total", Formatter.formatFileSize(ctx, total)),
            Field("Internal used", Formatter.formatFileSize(ctx, used)),
            Field("Internal available", Formatter.formatFileSize(ctx, free))
        )
        val ext = try {
            ctx.getExternalFilesDirs(null).filterNotNull().filter { Environment.isExternalStorageRemovable(it) }
        } catch (e: Exception) { emptyList() }
        if (ext.isNotEmpty()) {
            val es = StatFs(ext[0].path)
            fields.add(Field("External (SD) total", Formatter.formatFileSize(ctx, es.totalBytes)))
            fields.add(Field("External (SD) available", Formatter.formatFileSize(ctx, es.availableBytes)))
        } else {
            fields.add(Field("External storage", null))
        }
        fields.add(Field("Storage type", null))
        return Section("Storage", "স্টোরেজ", fields,
            progress = if (total > 0) used.toFloat() / total else null)
    }

    private fun battery(ctx: Context): Section {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return Section("Battery", "ব্যাটারি", listOf(Field("Battery", null)))
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val status = when (i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> null
        }
        val health = when (i.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            else -> null
        }
        val temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val volt = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        return Section("Battery", "ব্যাটারি", listOf(
            Field("Level", if (pct >= 0) "$pct%" else null),
            Field("Charging status", status),
            Field("Temperature", if (temp != Int.MIN_VALUE) String.format(Locale.US, "%.1f °C", temp / 10.0) else null),
            Field("Voltage", if (volt > 0) "$volt mV" else null),
            Field("Technology", s(i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY))),
            Field("Health", health),
            Field("Design capacity", null)
        ), progress = if (pct >= 0) pct / 100f else null)
    }

    private fun display(ctx: Context): Section {
        val dm = ctx.resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        val inches = if (dm.xdpi > 0 && dm.ydpi > 0) {
            val wi = w / dm.xdpi
            val hi = h / dm.ydpi
            String.format(Locale.US, "%.1f inches (estimated)", sqrt(wi * wi + hi * hi))
        } else null
        val refresh = try {
            val d = (ctx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
                .getDisplay(Display.DEFAULT_DISPLAY)
            String.format(Locale.US, "%.0f Hz", d.refreshRate)
        } catch (e: Exception) { null }
        return Section("Display", "ডিসপ্লে", listOf(
            Field("Resolution", "$w x $h px"),
            Field("Width", "$w px"),
            Field("Height", "$h px"),
            Field("Density", String.format(Locale.US, "%.2f", dm.density)),
            Field("DPI", "${dm.densityDpi} dpi"),
            Field("Screen size", inches),
            Field("Refresh rate", refresh),
            Field("Display technology", null)
        ))
    }

    private fun camera(ctx: Context): Section {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val ids = cm.cameraIdList
        val fields = mutableListOf(Field("Camera sensors", ids.size.toString()))
        for (id in ids) {
            val c = cm.getCameraCharacteristics(id)
            val facing = when (c.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                CameraCharacteristics.LENS_FACING_BACK -> "Rear"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                else -> "Unknown"
            }
            val size = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            val res = size?.let {
                String.format(Locale.US, "%dx%d (%.1f MP)", it.width, it.height, it.width * it.height / 1_000_000.0)
            } ?: "N/A"
            val level = when (c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                else -> "N/A"
            }
            val flash = if (c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) "Yes" else "No"
            val af = c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.joinToString(", ") {
                when (it) {
                    0 -> "OFF"; 1 -> "AUTO"; 2 -> "MACRO"
                    3 -> "CONTINUOUS_VIDEO"; 4 -> "CONTINUOUS_PICTURE"; 5 -> "EDOF"
                    else -> "?"
                }
            } ?: "N/A"
            fields.add(Field(
                "Camera $id ($facing)",
                "Sensor: $res\nHardware level: $level\nFlash: $flash\nFocus modes: $af"
            ))
        }
        return Section("Camera", "ক্যামেরা", fields)
    }

    private fun network(ctx: Context): Section {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val type = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }
        return Section("Network", "নেটওয়ার্ক", listOf(
            Field("Active network", type),
            Field("Baseband version", s(Build.getRadioVersion()))
        ))
    }

    fun report(sections: List<Section>, na: String = "Not Available"): String = buildString {
        appendLine("MR SHADOW DEVICE INFO")
        appendLine("Generated: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()))
        appendLine()
        for (sec in sections) {
            appendLine("== ${sec.titleEn} ==")
            for (f in sec.fields) appendLine("${f.label}: ${f.value ?: na}")
            appendLine()
        }
    }
}
