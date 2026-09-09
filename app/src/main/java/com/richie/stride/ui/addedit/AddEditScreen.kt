package com.richie.stride.ui.addedit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.richie.stride.data.Category
import com.richie.stride.data.DEFAULT_SLOT_ID
import com.richie.stride.data.GoalType
import com.richie.stride.data.Habit
import com.richie.stride.data.HabitSlot
import com.richie.stride.data.Schedule
import com.richie.stride.data.ScheduleType
import com.richie.stride.ui.MainViewModel
import com.richie.stride.ui.components.CategorySwatch
import com.richie.stride.ui.components.TimePickerDialog
import com.richie.stride.ui.components.label
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Compose's DatePicker works in UTC-midnight epoch millis, not the device's local timezone -
// converting via the device zone instead of UTC is a well-known source of off-by-one-day bugs.
private fun LocalDate.toEpochMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toLocalDateFromUtcMillis(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Local editable draft of a slot, before it's turned into a real HabitSlot on save. */
private data class SlotDraft(val id: String, val label: String, val reminderTime: LocalTime?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    viewModel: MainViewModel,
    habitId: String?,
    initialStartDate: LocalDate?,
    onDone: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val existing = habitId?.let { id -> state.habits.find { it.id == id } }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var nameError by remember { mutableStateOf(false) }
    var category by remember(existing) { mutableStateOf(existing?.category ?: Category.MIND) }
    var goalType by remember(existing) { mutableStateOf(existing?.goalType ?: GoalType.YES_NO) }
    var target by remember(existing) { mutableStateOf((existing?.target ?: 1).toString()) }
    var unit by remember(existing) { mutableStateOf(existing?.unit ?: "") }
    var scheduleType by remember(existing) { mutableStateOf(existing?.schedule?.type ?: ScheduleType.DAILY) }
    var scheduleDays by remember(existing) { mutableStateOf(existing?.schedule?.days ?: emptySet()) }
    var scheduleError by remember { mutableStateOf(false) }
    var interval by remember(existing) { mutableStateOf((existing?.schedule?.interval ?: 2).toString()) }
    var timesPerWeek by remember(existing) { mutableStateOf((existing?.schedule?.timesPerWeek ?: 3).toString()) }
    var grace by remember(existing) { mutableStateOf(existing?.grace ?: true) }
    var slots by remember(existing) {
        mutableStateOf(
            existing?.slots?.map { SlotDraft(it.id, it.label, it.reminderTime) }
                ?: listOf(SlotDraft(DEFAULT_SLOT_ID, "", null))
        )
    }
    var timePickerTargetSlotId by remember { mutableStateOf<String?>(null) }
    var startDate by remember(existing) { mutableStateOf(existing?.createdAt ?: initialStartDate ?: LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = { Text("Name") },
            isError = nameError,
            modifier = Modifier.fillMaxWidth()
        )
        if (nameError) Text("Enter a name for this habit.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

        Text("Category", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        Row {
            Category.entries.forEach { cat ->
                Column(
                    Modifier
                        .padding(end = 14.dp)
                        .clickable { category = cat },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CategorySwatch(cat, selected = category == cat)
                    Text(
                        cat.label(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }

        Text("Goal type", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        Row {
            listOf(GoalType.YES_NO to "Complete once", GoalType.COUNTER to "Counter", GoalType.DURATION to "Duration").forEach { (type, label) ->
                FilterChip(
                    selected = goalType == type,
                    onClick = { goalType = type },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
        }
        if (goalType != GoalType.YES_NO) {
            Row(Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = target,
                    onValueChange = { new -> if (new.all { it.isDigit() }) target = new },
                    label = { Text("Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                if (goalType == GoalType.COUNTER) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (glasses, steps\u2026)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Text("Schedule", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        var scheduleMenuOpen by remember { mutableStateOf(false) }
        Column {
            OutlinedButton(onClick = { scheduleMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(scheduleType.label())
            }
            DropdownMenu(expanded = scheduleMenuOpen, onDismissRequest = { scheduleMenuOpen = false }) {
                ScheduleType.entries.forEach { type ->
                    DropdownMenuItem(text = { Text(type.label()) }, onClick = { scheduleType = type; scheduleMenuOpen = false })
                }
            }
        }
        when (scheduleType) {
            ScheduleType.SPECIFIC -> {
                Row(Modifier.padding(top = 8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = scheduleDays.contains(day.value),
                            onClick = {
                                scheduleDays = if (scheduleDays.contains(day.value)) scheduleDays - day.value else scheduleDays + day.value
                                scheduleError = false
                            },
                            label = { Text(day.name.take(1)) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
                if (scheduleError) Text("Pick at least one day", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            ScheduleType.INTERVAL -> {
                OutlinedTextField(
                    value = interval,
                    onValueChange = { new -> if (new.all { it.isDigit() }) interval = new },
                    label = { Text("Every X days") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            ScheduleType.TIMES_PER_WEEK -> {
                OutlinedTextField(
                    value = timesPerWeek,
                    onValueChange = { new -> if (new.all { it.isDigit() }) timesPerWeek = new },
                    label = { Text("Times per week") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            else -> {}
        }

        if (existing == null) {
            Text("Starts on", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(startDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
            }
            if (scheduleType == ScheduleType.INTERVAL) {
                Text(
                    "\"Every X days\" counts from this date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Allow 1 grace skip / week")
            Switch(checked = grace, onCheckedChange = { grace = it })
        }

        Text("When", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
        slots.forEach { slot ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (slots.size > 1) {
                    OutlinedTextField(
                        value = slot.label,
                        onValueChange = { new -> slots = slots.map { if (it.id == slot.id) it.copy(label = new) else it } },
                        label = { Text("Label") },
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                }
                OutlinedButton(
                    onClick = { timePickerTargetSlotId = slot.id },
                    modifier = if (slots.size > 1) Modifier else Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(
                        slot.reminderTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "No reminder",
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                if (slots.size > 1) {
                    IconButton(onClick = { slots = slots.filterNot { it.id == slot.id } }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove this time")
                    }
                }
            }
        }
        TextButton(onClick = { slots = slots + SlotDraft(viewModel.newSlotId(), "", null) }) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("Add another time", modifier = Modifier.padding(start = 4.dp))
        }
        if (slots.size > 1) {
            Text(
                "Each time is tracked separately - useful for a habit you do more than once a day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Button(
            onClick = {
                if (name.isBlank()) { nameError = true; return@Button }
                if (scheduleType == ScheduleType.SPECIFIC && scheduleDays.isEmpty()) { scheduleError = true; return@Button }
                val habit = Habit(
                    id = existing?.id ?: viewModel.newHabitId(),
                    name = name.trim(),
                    category = category,
                    goalType = goalType,
                    target = if (goalType == GoalType.YES_NO) 1 else (target.toIntOrNull() ?: 1).coerceAtLeast(1),
                    unit = if (goalType == GoalType.COUNTER) unit.trim() else "",
                    schedule = Schedule(
                        type = scheduleType,
                        days = scheduleDays,
                        interval = (interval.toIntOrNull() ?: 2).coerceAtLeast(1),
                        timesPerWeek = (timesPerWeek.toIntOrNull() ?: 3).coerceIn(1, 7)
                    ),
                    grace = grace,
                    slots = slots.map { HabitSlot(id = it.id, label = it.label.trim(), reminderTime = it.reminderTime) },
                    archived = existing?.archived ?: false,
                    pausedUntil = existing?.pausedUntil,
                    createdAt = existing?.createdAt ?: startDate
                )
                viewModel.saveHabit(habit)
                onDone()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
        ) { Text("Save habit") }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate.toEpochMillisUtc())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis -> startDate = millis.toLocalDateFromUtcMillis() }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    timePickerTargetSlotId?.let { targetId ->
        val targetSlot = slots.find { it.id == targetId }
        TimePickerDialog(
            initialTime = targetSlot?.reminderTime,
            onDismiss = { timePickerTargetSlotId = null },
            onConfirm = { time -> slots = slots.map { if (it.id == targetId) it.copy(reminderTime = time) else it } },
            onClearReminder = { slots = slots.map { if (it.id == targetId) it.copy(reminderTime = null) else it } }
        )
    }
}

private fun ScheduleType.label(): String = when (this) {
    ScheduleType.DAILY -> "Every day"
    ScheduleType.WEEKDAYS -> "Weekdays"
    ScheduleType.WEEKENDS -> "Weekends"
    ScheduleType.SPECIFIC -> "Specific days"
    ScheduleType.INTERVAL -> "Every X days"
    ScheduleType.TIMES_PER_WEEK -> "X times / week"
}
