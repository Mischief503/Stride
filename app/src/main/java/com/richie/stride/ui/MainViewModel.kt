package com.richie.stride.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richie.stride.StrideApp
import com.richie.stride.data.AppLanguage
import com.richie.stride.data.AppSettings
import com.richie.stride.data.Completion
import com.richie.stride.data.DEFAULT_SLOT_ID
import com.richie.stride.data.Habit
import com.richie.stride.data.HabitNote
import com.richie.stride.data.Mood
import com.richie.stride.data.Routine
import com.richie.stride.data.StatsCalculator
import com.richie.stride.data.ThemeMode
import com.richie.stride.notifications.ReminderScheduler
import com.richie.stride.ui.theme.AccentOption
import com.richie.stride.util.BackupImportException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

data class AppState(
    val habits: List<Habit> = emptyList(),
    val completionsByHabit: Map<String, Map<LocalDate, Completion>> = emptyMap(),
    val completionsBySlot: Map<String, Map<String, Map<LocalDate, Completion>>> = emptyMap(),
    val notesByHabit: Map<String, Map<LocalDate, HabitNote>> = emptyMap(),
    val routines: List<Routine> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val loaded: Boolean = false
)

sealed interface ImportResult {
    data object Success : ImportResult
    data class Failure(val message: String) : ImportResult
}

/** A one-shot event: a habit was just marked done and can still be undone. */
data class UndoableCompletion(val habitId: String, val habitName: String, val date: LocalDate)

class MainViewModel(private val app: StrideApp) : ViewModel() {

    private val repo = app.repository
    private val settingsRepo = app.settingsRepository

    val state: StateFlow<AppState> = combine(
        repo.habits,
        combine(repo.completionsByHabit, repo.completionsBySlot) { byHabit, bySlot -> byHabit to bySlot },
        repo.allNotes,
        repo.routines,
        settingsRepo.settings
    ) { habits, completionsPair, notes, routines, settings ->
        val (completionsByHabit, completionsBySlot) = completionsPair
        val notesByHabit = notes.groupBy { it.habitId }.mapValues { (_, v) -> v.associateBy { n -> n.date } }
        AppState(habits, completionsByHabit, completionsBySlot, notesByHabit, routines, settings, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppState())

    // Channel, not StateFlow: this is a one-shot "show a snackbar" signal, not persistent
    // state. A StateFlow would replay its last value to a new collector (e.g. after a
    // recomposition or config change), which could pop the snackbar back up days later for
    // no reason. A Channel is consumed exactly once.
    private val _undoEvents = Channel<UndoableCompletion>(Channel.BUFFERED)
    val undoEvents = _undoEvents.receiveAsFlow()

    private data class PendingUndo(val habitId: String, val slotId: String, val date: LocalDate)
    private var pendingUndo: PendingUndo? = null

    private fun displayNameFor(habit: Habit, slotId: String): String {
        if (!habit.hasMultipleSlots) return habit.name
        val label = habit.slots.find { it.id == slotId }?.label?.trim()
        return if (label.isNullOrBlank()) habit.name else "${habit.name} ($label)"
    }

    fun toggleYesNo(habitId: String, date: LocalDate = LocalDate.now(), slotId: String = DEFAULT_SLOT_ID) {
        val habit = state.value.habits.find { it.id == habitId } ?: return
        val completion = state.value.completionsBySlot[habitId]?.get(slotId)?.get(date)
        val wasDone = StatsCalculator.isDoneOn(habit, completion)
        viewModelScope.launch {
            repo.toggleYesNo(habitId, date, slotId)
            if (!wasDone) {
                pendingUndo = PendingUndo(habitId, slotId, date)
                _undoEvents.send(UndoableCompletion(habitId, displayNameFor(habit, slotId), date))
            } else {
                pendingUndo = null
            }
        }
    }

    fun stepValue(habitId: String, date: LocalDate = LocalDate.now(), delta: Int, slotId: String = DEFAULT_SLOT_ID) {
        val habit = state.value.habits.find { it.id == habitId } ?: return
        val current = state.value.completionsBySlot[habitId]?.get(slotId)?.get(date)?.takeIf { !it.isGrace }?.value ?: 0
        val wasDone = current >= habit.target
        viewModelScope.launch {
            repo.stepValue(habitId, date, delta, slotId)
            val next = (current + delta).coerceAtLeast(0)
            val nowDone = next >= habit.target
            if (!wasDone && nowDone) {
                pendingUndo = PendingUndo(habitId, slotId, date)
                _undoEvents.send(UndoableCompletion(habitId, displayNameFor(habit, slotId), date))
            }
        }
    }

    fun undoLast() {
        val (habitId, slotId, date) = pendingUndo ?: return
        viewModelScope.launch { repo.clearCompletion(habitId, date, slotId) }
        pendingUndo = null
    }

    fun useGrace(habitId: String, date: LocalDate = LocalDate.now(), slotId: String = DEFAULT_SLOT_ID) {
        viewModelScope.launch { repo.setGrace(habitId, date, slotId) }
    }

    fun setValue(habitId: String, date: LocalDate, value: Int, slotId: String = DEFAULT_SLOT_ID) {
        viewModelScope.launch { repo.setValue(habitId, date, value, slotId) }
    }

    fun clearCompletion(habitId: String, date: LocalDate, slotId: String = DEFAULT_SLOT_ID) {
        viewModelScope.launch { repo.clearCompletion(habitId, date, slotId) }
    }

    fun cycleYesNoBackfill(habitId: String, date: LocalDate) {
        // Tapping a past heatmap cell for a yes/no habit cycles none -> done -> grace -> none.
        // Detail's heatmap only shows the default slot for now - a known, deliberate limit for
        // this pass, not an oversight; extending Detail to show every slot's own heatmap is a
        // reasonable next step but out of scope here.
        viewModelScope.launch {
            val habit = repo.getHabit(habitId) ?: return@launch
            val existing = state.value.completionsByHabit[habitId]?.get(date)
            when {
                existing == null -> repo.toggleYesNo(habitId, date) // marks done
                !existing.isGrace && existing.value >= 1 && habit.grace -> repo.setGrace(habitId, date)
                else -> repo.clearCompletion(habitId, date)
            }
        }
    }

    fun saveHabit(habit: Habit) {
        // A removed slot's alarm would otherwise keep firing forever with nothing in the UI
        // to trace it back to - cancel those explicitly, not just the current slot list.
        val previousSlotIds = state.value.habits.find { it.id == habit.id }?.slots?.map { it.id }?.toSet() ?: emptySet()
        val removedSlotIds = previousSlotIds - habit.slots.map { it.id }.toSet()
        viewModelScope.launch {
            repo.saveHabit(habit)
            removedSlotIds.forEach { removedId -> ReminderScheduler.cancel(app, habit.id, removedId) }
            habit.slots.forEach { slot ->
                if (slot.reminderTime != null && !habit.archived) {
                    ReminderScheduler.schedule(app, habit.id, slot.id, habit.name, slot.reminderTime.hour, slot.reminderTime.minute)
                } else {
                    ReminderScheduler.cancel(app, habit.id, slot.id)
                }
            }
        }
    }

    fun newHabitId(): String = repo.newHabitId()
    fun newSlotId(): String = repo.newSlotId()

    fun archiveHabit(habitId: String) {
        viewModelScope.launch {
            repo.setArchived(habitId, true)
            val slotIds = state.value.habits.find { it.id == habitId }?.slots?.map { it.id } ?: emptyList()
            ReminderScheduler.cancelAll(app, habitId, slotIds)
        }
    }

    fun restoreHabit(habitId: String) {
        viewModelScope.launch { repo.setArchived(habitId, false) }
    }

    fun deletePermanently(habitId: String) {
        viewModelScope.launch {
            val slotIds = state.value.habits.find { it.id == habitId }?.slots?.map { it.id } ?: emptyList()
            repo.deletePermanently(habitId)
            ReminderScheduler.cancelAll(app, habitId, slotIds)
        }
    }

    fun pauseHabit(habitId: String, until: LocalDate) {
        viewModelScope.launch { repo.setPausedUntil(habitId, until) }
    }

    fun resumeHabit(habitId: String) {
        viewModelScope.launch { repo.setPausedUntil(habitId, null) }
    }

    fun saveNote(habitId: String, date: LocalDate, mood: Mood?, text: String) {
        viewModelScope.launch { repo.saveNote(HabitNote(habitId, date, mood, text)) }
    }

    fun saveRoutine(routine: Routine) {
        viewModelScope.launch { repo.saveRoutine(routine) }
    }

    fun deleteRoutine(routineId: String) {
        viewModelScope.launch { repo.deleteRoutine(routineId) }
    }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch { settingsRepo.setLanguage(language) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    fun setAccent(accent: AccentOption) = viewModelScope.launch { settingsRepo.setAccent(accent) }
    fun setWeekStart(day: DayOfWeek) = viewModelScope.launch { settingsRepo.setWeekStart(day) }
    fun setReduceMotion(value: Boolean) = viewModelScope.launch { settingsRepo.setReduceMotion(value) }
    fun setSound(value: Boolean) = viewModelScope.launch { settingsRepo.setSound(value) }
    fun setOnboarded(value: Boolean) = viewModelScope.launch { settingsRepo.setOnboarded(value) }

    suspend fun exportJson(): String = app.backupManager.exportJson()

    fun importJson(text: String, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            try {
                app.backupManager.importJson(text)
                onResult(ImportResult.Success)
            } catch (e: BackupImportException) {
                onResult(ImportResult.Failure(e.message ?: "Import failed"))
            } catch (e: Exception) {
                onResult(ImportResult.Failure("Import failed"))
            }
        }
    }

    fun resetAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.wipeAllData()
            settingsRepo.resetAll()
            onDone()
        }
    }
}
