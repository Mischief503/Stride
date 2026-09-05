package com.richie.stride.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.richie.stride.data.AppLanguage
import com.richie.stride.data.ThemeMode
import com.richie.stride.ui.ImportResult
import com.richie.stride.ui.MainViewModel
import com.richie.stride.ui.components.ConfirmDialog
import com.richie.stride.ui.theme.AccentOption
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: MainViewModel, onResetDone: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val settings = state.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var weekStartMenuOpen by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
            if (text != null) {
                viewModel.importJson(text) { result ->
                    if (result is ImportResult.Failure) importError = result.message
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Language", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
        Row {
            AppLanguage.entries.forEach { lang ->
                FilterChip(
                    selected = settings.language == lang,
                    onClick = { viewModel.setLanguage(lang) },
                    label = { Text(lang.tag.uppercase()) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        Text("Theme", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        Row {
            listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.SYSTEM to "System").forEach { (mode, label) ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }

        Text("Accent color", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        Row {
            AccentOption.entries.forEach { accent ->
                Box(
                    Modifier
                        .padding(end = 10.dp)
                        .size(32.dp)
                        .background(accent.color, CircleShape)
                        .clickable { viewModel.setAccent(accent) }
                ) {}
            }
        }

        Text("Week starts on", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        Box {
            OutlinedButton(onClick = { weekStartMenuOpen = true }) {
                Text(settings.weekStart.getDisplayName(TextStyle.FULL, Locale.getDefault()))
            }
            DropdownMenu(expanded = weekStartMenuOpen, onDismissRequest = { weekStartMenuOpen = false }) {
                DayOfWeek.entries.forEach { day ->
                    DropdownMenuItem(
                        text = { Text(day.getDisplayName(TextStyle.FULL, Locale.getDefault())) },
                        onClick = { viewModel.setWeekStart(day); weekStartMenuOpen = false }
                    )
                }
            }
        }

        SettingSwitchRow("Reduce motion", settings.reduceMotion) { viewModel.setReduceMotion(it) }
        SettingSwitchRow("Sound effects", settings.sound) { viewModel.setSound(it) }

        Text("Notifications", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(if (notifGranted) "Reminders are on." else "Reminders aren't enabled yet.")
                if (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OutlinedButton(
                        onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Enable") }
                }
            }
        }

        Text("Data", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        Row {
            OutlinedButton(onClick = { exportLauncher.launch("habit-tracker-backup.json") }, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text("Export data")
            }
            OutlinedButton(onClick = { importLauncher.launch("application/json") }, modifier = Modifier.weight(1f)) {
                Text("Import data")
            }
        }
        OutlinedButton(
            onClick = { showResetConfirm = true },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("Reset all data", color = MaterialTheme.colorScheme.error) }

        Text("About", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        Text("Stride \u2014 fully local, no account required.")

        androidx.compose.foundation.layout.Spacer(Modifier.padding(40.dp))
    }

    if (showResetConfirm) {
        ConfirmDialog(
            title = "Reset all data?",
            body = "This deletes every habit and setting on this device. This can't be undone.",
            confirmLabel = "Reset",
            onDismiss = { showResetConfirm = false },
            onConfirm = { viewModel.resetAllData { onResetDone() }; showResetConfirm = false }
        )
    }
    importError?.let { msg ->
        ConfirmDialog(
            title = "Import failed",
            body = msg,
            confirmLabel = "Close",
            onDismiss = { importError = null },
            onConfirm = { importError = null }
        )
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
