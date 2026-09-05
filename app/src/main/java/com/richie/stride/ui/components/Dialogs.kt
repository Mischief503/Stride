package com.richie.stride.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.richie.stride.data.Mood
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HabitMenuDialog(
    habitName: String,
    dateLabel: String?,
    canUseGrace: Boolean,
    canLogValue: Boolean,
    isPaused: Boolean,
    onDismiss: () -> Unit,
    onUseGrace: () -> Unit,
    onLogNote: () -> Unit,
    onLogValue: () -> Unit,
    onViewInsights: () -> Unit,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onArchive: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Column { Text(habitName); dateLabel?.let { Text(it, style = androidx.compose.material3.MaterialTheme.typography.bodySmall) } } },
        text = {
            Column {
                if (canUseGrace) MenuRow("Use a grace skip") { onUseGrace() }
                if (canLogValue) MenuRow("Log amount") { onLogValue() }
                MenuRow("Add a note") { onLogNote() }
                MenuRow("View insights") { onViewInsights() }
                MenuRow("Edit habit") { onEdit() }
                if (isPaused) MenuRow("Resume habit") { onResume() } else MenuRow("Pause habit") { onPause() }
                MenuRow("Archive habit") { onArchive() }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) { Text(label) }
    }
}

@Composable
fun NoteDialog(
    habitName: String,
    date: LocalDate,
    initialMood: Mood?,
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (Mood?, String) -> Unit
) {
    var mood by remember { mutableStateOf(initialMood) }
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How did it go?") },
        text = {
            Column {
                Text("$habitName \u2014 ${date.format(DateTimeFormatter.ofPattern("MMM d"))}")
                Row(Modifier.padding(vertical = 8.dp)) {
                    Mood.entries.forEach { m ->
                        FilterChip(
                            selected = mood == m,
                            onClick = { mood = if (mood == m) null else m },
                            label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Add a short note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(mood, text) }) { Text("Save note") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Skip") } }
    )
}

@Composable
fun ValueDialog(
    habitName: String,
    date: LocalDate,
    target: Int,
    unit: String,
    initialValue: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onClear: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log $habitName") },
        text = {
            Column {
                Text(date.format(DateTimeFormatter.ofPattern("MMM d")))
                OutlinedTextField(
                    value = text,
                    onValueChange = { new -> if (new.all { it.isDigit() }) text = new },
                    label = { Text("Target: $target $unit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.toIntOrNull() ?: 0) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClear) { Text("Clear") } }
    )
}

@Composable
fun PauseDialog(
    habitName: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val defaultResume = remember { LocalDate.now().plusDays(7) }
    var dateText by remember { mutableStateOf(defaultResume.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pause $habitName?") },
        text = {
            Column {
                Text("It won't count toward streaks or stats until you resume it.")
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Resume on (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = runCatching { LocalDate.parse(dateText) }.getOrDefault(defaultResume)
                onConfirm(parsed)
            }) { Text("Pause") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
