package com.richie.stride.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.richie.stride.StrideApp
import com.richie.stride.data.AppLanguage
import com.richie.stride.data.AppSettings
import com.richie.stride.data.Completion
import com.richie.stride.data.Habit
import com.richie.stride.data.HabitNote
import com.richie.stride.data.Mood
import com.richie.stride.data.Routine
import com.richie.stride.data.ThemeMode
import com.richie.stride.notifications.ReminderScheduler
import com.richie.stride.ui.theme.AccentOption
import com.richie.stride.util.BackupImportException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

data class AppState(
    val habits: List<Habit> = emptyList(),
    val completionsByHabit: Map<String, Map<LocalDate, Completion>> = emptyMap(),
    val notesByHabit: Map<String, Map<LocalDate, HabitNote>> = emptyMap(),
    val routines: List<Routine> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val loaded: Boolean = false
)

sealed interface ImportResult {
    data object Success : ImportResult
    data class Failure(val message: String) : ImportResult
}

class MainViewModel(private val app: StrideApp) : ViewModel() {

    private val repo = app.repository
    private val settingsRepo = app.settingsRepository

    val state: StateFlow<AppState> = combine(
        repo.habits, repo.completionsByHabit, repo.allNotes, repo.routines, settingsRepo.settings
    ) { habits, completions, notes, routines, settings ->
        val notesByHabit = notes.groupBy { it.habitId }.mapValues { (_, v) -> v.associateBy { n -> n.date } }
        AppState(habits, completions, notesByHabit, routines, settings, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppState())

    private val _lastUndo = MutableStateFlow<Pair<String, LocalDate>?>(null)
    val lastUndo: StateFlow<Pair<String, LocalDate>?> = _lastUndo

    fun toggleYesNo(habitId: String, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            repo.toggleYesNo(habitId, date)
            _lastUndo.value = habitId to date
        }
    }

    fun stepValue(habitId: String, date: LocalDate = LocalDate.now(), delta: Int) {
        viewModelScope.launch {
            repo.stepValue(habitId, date, delta)
            _lastUndo.value = habitId to date
        }
    }

    fun undoLast() {
        val (habitId, date) = _lastUndo.value ?: return
        viewModelScope.launch { repo.clearCompletion(habitId, date) }
        _lastUndo.value = null
    }

    fun useGrace(habitId: String, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch { repo.setGrace(habitId, date) }
    }

    fun setValue(habitId: String, date: LocalDate, value: Int) {
        viewModelScope.launch { repo.setValue(habitId, date, value) }
    }

    fun clearCompletion(habitId: String, date: LocalDate) {
        viewModelScope.launch { repo.clearCompletion(habitId, date) }
    }

    fun cycleYesNoBackfill(habitId: String, date: LocalDate) {
        // Tapping a past heatmap cell for a yes/no habit cycles none -> done -> grace -> none.
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
        viewModelScope.launch {
            repo.saveHabit(habit)
            if (habit.reminderTime != null && !habit.archived) {
                ReminderScheduler.schedule(app, habit.id, habit.name, habit.reminderTime.hour, habit.reminderTime.minute)
            } else {
                ReminderScheduler.cancel(app, habit.id)
            }
        }
    }

    fun newHabitId(): String = repo.newHabitId()

    fun archiveHabit(habitId: String) {
        viewModelScope.launch {
            repo.setArchived(habitId, true)
            ReminderScheduler.cancel(app, habitId)
        }
    }

    fun restoreHabit(habitId: String) {
        viewModelScope.launch { repo.setArchived(habitId, false) }
    }

    fun deletePermanently(habitId: String) {
        viewModelScope.launch {
            repo.deletePermanently(habitId)
            ReminderScheduler.cancel(app, habitId)
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
