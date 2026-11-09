package com.mrshadow.deviceinfo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

const val WHATSAPP_URL = "https://wa.me/message/AXTGUAHKBLIHL1"
const val TELEGRAM_URL = "https://t.me/mr_shadow23"
const val DEV_NAME = "𝙼𝚁  𝚂𝙷𝙰𝙳𝙾𝚆"

fun openLink(ctx: Context, url: String, bn: Boolean) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(ctx, tr(bn, "No app found to open this link", "লিংকটি খোলার মতো অ্যাপ পাওয়া যায়নি"), Toast.LENGTH_SHORT).show()
    }
}

fun shareText(ctx: Context, text: String, bn: Boolean) {
    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    ctx.startActivity(Intent.createChooser(i, tr(bn, "Share", "শেয়ার করুন")))
}

@Composable
private fun cardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

// ---------------------------------------------------------------- HOME

@Composable
fun HomeScreen(vm: DeviceVm, st: AppSettings) {
    val ctx = LocalContext.current
    val bn = st.bn

    LaunchedEffect(st.autoRefresh) {
        while (st.autoRefresh) {
            delay(5000)
            vm.refresh()
        }
    }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val ok = try {
                ctx.contentResolver.openOutputStream(uri)?.use {
                    it.write(DeviceInfoCollector.report(vm.sections).toByteArray())
                }
                true
            } catch (e: Exception) { false }
            Toast.makeText(
                ctx,
                if (ok) tr(bn, "File saved", "ফাইল সেভ হয়েছে") else tr(bn, "Could not save file", "ফাইল সেভ করা যায়নি"),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("MR SHADOW", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                tr(bn, "Device Information", "ডিভাইস তথ্য"),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.refresh() }, Modifier.weight(1f)) {
                        Text(tr(bn, "Refresh", "রিফ্রেশ"))
                    }
                    FilledTonalButton(onClick = {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("device-info", DeviceInfoCollector.report(vm.sections)))
                        Toast.makeText(ctx, tr(bn, "Copied", "কপি হয়েছে"), Toast.LENGTH_SHORT).show()
                    }, Modifier.weight(1f)) { Text(tr(bn, "Copy", "কপি")) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {
                        shareText(ctx, DeviceInfoCollector.report(vm.sections), bn)
                    }, Modifier.weight(1f)) { Text(tr(bn, "Share", "শেয়ার")) }
                    FilledTonalButton(onClick = {
                        export.launch("mr-shadow-device-info.txt")
                    }, Modifier.weight(1f)) { Text(tr(bn, "Export TXT", "TXT সেভ")) }
                }
            }
        }
        if (vm.loading && vm.sections.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(vm.sections, key = { it.titleEn }) { sec ->
                SectionCard(sec, st, startOpen = sec.titleEn == "Basic Device")
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun SectionCard(sec: Section, st: AppSettings, startOpen: Boolean) {
    val bn = st.bn
    var open by rememberSaveable(sec.titleEn) { mutableStateOf(startOpen) }
    val fields = sec.fields.filter { (st.showAdvanced || !it.advanced) && (st.showUnavailable || it.value != null) }
    Card(shape = RoundedCornerShape(20.dp), colors = cardColors()) {
        Column(Modifier.animateContentSize()) {
            Row(
                Modifier.fillMaxWidth().clickable { open = !open }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    tr(bn, sec.titleEn, sec.titleBn),
                    Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
            if (open) {
                sec.progress?.let {
                    LinearProgressIndicator(
                        progress = { it.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(8.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                fields.forEachIndexed { i, f ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            f.label, Modifier.weight(0.42f), fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            f.value ?: tr(bn, "Not Available", "পাওয়া যায়নি"),
                            Modifier.weight(0.58f), fontSize = 14.sp,
                            color = if (f.value == null) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (i < fields.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- APPS

@Composable
fun AppsScreen(st: AppSettings) {
    val ctx = LocalContext.current
    val bn = st.bn
    val apps = remember { AppsRepository.load(ctx) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text(tr(bn, "Apps & Tools", "অ্যাপস ও টুলস"), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        if (apps.isEmpty()) {
            item {
                Text(
                    tr(bn, "No apps available yet.", "এখনো কোনো অ্যাপ যোগ করা হয়নি।"),
                    Modifier.fillMaxWidth().padding(40.dp), textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(apps) { app ->
            Card(shape = RoundedCornerShape(20.dp), colors = cardColors()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                app.name.take(1).uppercase(), fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOf(app.category, app.version).filter { it.isNotBlank() }.joinToString(" • "),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (app.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(app.description, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { openLink(ctx, app.downloadUrl, bn) },
                            enabled = app.downloadUrl.isNotBlank()
                        ) {
                            Text(
                                if (app.downloadUrl.isBlank()) tr(bn, "Link not set", "লিংক নেই")
                                else tr(bn, "Open / Download", "খুলুন / ডাউনলোড")
                            )
                        }
                        if (app.websiteUrl.isNotBlank()) {
                            OutlinedButton(onClick = { openLink(ctx, app.websiteUrl, bn) }) {
                                Text(tr(bn, "Website", "ওয়েবসাইট"))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ---------------------------------------------------------------- DEVELOPER

@Composable
fun DeveloperScreen(vm: DeviceVm, st: AppSettings) {
    val ctx = LocalContext.current
    val bn = st.bn
    var preview by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(16.dp))
        Text(DEV_NAME, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Android Device Services\nRoot Support\nDevice Information Utility",
            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        ContactRow(Icons.Default.Chat, "WhatsApp") { openLink(ctx, WHATSAPP_URL, bn) }
        Spacer(Modifier.height(12.dp))
        ContactRow(Icons.AutoMirrored.Filled.Send, "Telegram") { openLink(ctx, TELEGRAM_URL, bn) }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { preview = true },
            Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                tr(bn, "Share Your Device Info With Developer", "ডেভেলপারকে আপনার ডিভাইস তথ্য পাঠান"),
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (preview) {
        AlertDialog(
            onDismissRequest = { preview = false },
            title = { Text(tr(bn, "Review before sending", "পাঠানোর আগে দেখে নিন")) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        tr(
                            bn,
                            "This information will be sent to the developer only if you confirm. No IMEI, serial number, phone number, SIM info or location is included.",
                            "আপনি নিশ্চিত করলেই এই তথ্য ডেভেলপারের কাছে যাবে। IMEI, সিরিয়াল নম্বর, ফোন নম্বর, সিম তথ্য বা লোকেশন এতে নেই।"
                        ),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        DeviceInfoCollector.report(vm.sections, tr(bn, "Not Available", "পাওয়া যায়নি")),
                        fontSize = 11.sp, fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    preview = false
                    // Backend is not connected in this stage, so nothing is sent.
                    Toast.makeText(
                        ctx,
                        tr(bn, "Sending is not connected yet. Nothing was sent.", "পাঠানোর সংযোগ এখনো তৈরি হয়নি। কিছুই পাঠানো হয়নি।"),
                        Toast.LENGTH_LONG
                    ).show()
                }) { Text(tr(bn, "Send", "পাঠান")) }
            },
            dismissButton = {
                TextButton(onClick = { preview = false }) { Text(tr(bn, "Cancel", "বাতিল")) }
            }
        )
    }
}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = cardColors(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- SETTINGS

@Composable
fun SettingsScreen(st: AppSettings) {
    val ctx = LocalContext.current
    val bn = st.bn
    var dialog by rememberSaveable { mutableStateOf(0) } // 1 privacy, 2 sharing info

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Spacer(Modifier.height(8.dp))
        Text(tr(bn, "Settings", "সেটিংস"), fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Group(tr(bn, "Appearance", "চেহারা"))
        RadioRow(tr(bn, "Dark", "ডার্ক"), st.theme == 1) { st.updateTheme(1) }
        RadioRow(tr(bn, "Light", "লাইট"), st.theme == 2) { st.updateTheme(2) }
        RadioRow(tr(bn, "System default", "সিস্টেম ডিফল্ট"), st.theme == 0) { st.updateTheme(0) }

        Group(tr(bn, "Language", "ভাষা"))
        RadioRow("বাংলা", st.bn) { st.updateBn(true) }
        RadioRow("English", !st.bn) { st.updateBn(false) }

        Group(tr(bn, "Device Information", "ডিভাইস তথ্য"))
        SwitchRow(tr(bn, "Auto refresh (every 5 sec)", "অটো রিফ্রেশ (৫ সেকেন্ড পর পর)"), st.autoRefresh) { st.updateAutoRefresh(it) }
        SwitchRow(tr(bn, "Show advanced information", "অ্যাডভান্সড তথ্য দেখান"), st.showAdvanced) { st.updateShowAdvanced(it) }
        SwitchRow(tr(bn, "Show unavailable fields", "অনুপলব্ধ ফিল্ড দেখান"), st.showUnavailable) { st.updateShowUnavailable(it) }

        Group(tr(bn, "Privacy", "প্রাইভেসি"))
        ClickRow(tr(bn, "Privacy Policy", "প্রাইভেসি পলিসি")) { dialog = 1 }
        ClickRow(tr(bn, "Data Sharing Information", "ডেটা শেয়ারিং তথ্য")) { dialog = 2 }

        Group(tr(bn, "About", "অ্যাপ সম্পর্কে"))
        InfoRow(tr(bn, "Application", "অ্যাপ"), "MR SHADOW DEVICE INFO")
        InfoRow(tr(bn, "Version name", "ভার্সন নাম"), BuildConfig.VERSION_NAME)
        InfoRow(tr(bn, "Version code", "ভার্সন কোড"), BuildConfig.VERSION_CODE.toString())
        InfoRow(tr(bn, "Developer", "ডেভেলপার"), DEV_NAME)

        Group(tr(bn, "Support", "সাপোর্ট"))
        ClickRow("WhatsApp") { openLink(ctx, WHATSAPP_URL, bn) }
        ClickRow("Telegram") { openLink(ctx, TELEGRAM_URL, bn) }
        ClickRow(tr(bn, "Share App", "অ্যাপ শেয়ার করুন")) {
            shareText(ctx, "MR SHADOW DEVICE INFO", bn)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (dialog != 0) {
        val title = if (dialog == 1) tr(bn, "Privacy Policy", "প্রাইভেসি পলিসি")
        else tr(bn, "Data Sharing Information", "ডেটা শেয়ারিং তথ্য")
        val body = if (dialog == 1) tr(
            bn,
            "This app reads your phone's hardware and system information using standard Android APIs, only to show it to you.\n\nNothing is sent anywhere automatically. Information is sent to the developer only when you press the share button, review the report, and confirm.\n\nThe app never collects IMEI, serial number, phone number, SIM information, location, contacts, messages or files.",
            "এই অ্যাপ শুধু আপনাকে দেখানোর জন্য স্ট্যান্ডার্ড অ্যান্ড্রয়েড API দিয়ে ফোনের হার্ডওয়্যার ও সিস্টেম তথ্য পড়ে।\n\nস্বয়ংক্রিয়ভাবে কিছুই কোথাও পাঠানো হয় না। শেয়ার বাটন চেপে, রিপোর্ট দেখে, নিশ্চিত করলেই কেবল তথ্য ডেভেলপারের কাছে যায়।\n\nIMEI, সিরিয়াল নম্বর, ফোন নম্বর, সিম তথ্য, লোকেশন, কন্টাক্ট, মেসেজ বা ফাইল কখনো সংগ্রহ করা হয় না।"
        ) else tr(
            bn,
            "When you choose 'Share Your Device Info With Developer', you will see the full report first. You can cancel at any time. Only the reviewed report is sent.",
            "'ডেভেলপারকে আপনার ডিভাইস তথ্য পাঠান' চাপলে আগে পুরো রিপোর্ট দেখানো হবে। আপনি যেকোনো সময় বাতিল করতে পারবেন। শুধু দেখে নেওয়া রিপোর্টই পাঠানো হবে।"
        )
        AlertDialog(
            onDismissRequest = { dialog = 0 },
            title = { Text(title) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(body) } },
            confirmButton = { TextButton(onClick = { dialog = 0 }) { Text("OK") } }
        )
    }
}

@Composable
private fun Group(title: String) {
    Spacer(Modifier.height(20.dp))
    Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ClickRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
