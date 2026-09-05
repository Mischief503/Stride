package com.richie.stride.util

import com.richie.stride.data.Category
import com.richie.stride.data.Completion
import com.richie.stride.data.GoalType
import com.richie.stride.data.Habit
import com.richie.stride.data.HabitNote
import com.richie.stride.data.HabitRepository
import com.richie.stride.data.Mood
import com.richie.stride.data.Routine
import com.richie.stride.data.Schedule
import com.richie.stride.data.ScheduleType
import com.richie.stride.data.TimeLabel
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

class BackupImportException(message: String) : Exception(message)

class BackupManager(private val repository: HabitRepository) {

    suspend fun exportJson(): String {
        val habits = repository.habits.first()
        val completions = repository.completionsByHabit.first().values.flatMap { it.values }
        val notes = repository.allNotes.first()
        val routines = repository.routines.first()

        val root = JSONObject()
        root.put("version", 1)

        val habitsArr = JSONArray()
        habits.forEach { h -> habitsArr.put(habitToJson(h)) }
        root.put("habits", habitsArr)

        val completionsArr = JSONArray()
        completions.forEach { c ->
            completionsArr.put(JSONObject().apply {
                put("habitId", c.habitId)
                put("date", c.date.toString())
                put("value", c.value)
                put("isGrace", c.isGrace)
            })
        }
        root.put("completions", completionsArr)

        val notesArr = JSONArray()
        notes.forEach { n ->
            notesArr.put(JSONObject().apply {
                put("habitId", n.habitId)
                put("date", n.date.toString())
                put("mood", n.mood?.name ?: JSONObject.NULL)
                put("text", n.text)
            })
        }
        root.put("notes", notesArr)

        val routinesArr = JSONArray()
        routines.forEach { r ->
            routinesArr.put(JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("timeLabel", r.timeLabel.name)
                put("sortOrder", r.sortOrder)
                put("habitIds", JSONArray(r.habitIds))
            })
        }
        root.put("routines", routinesArr)

        return root.toString(2)
    }

    private fun habitToJson(h: Habit): JSONObject = JSONObject().apply {
        put("id", h.id)
        put("name", h.name)
        put("category", h.category.name)
        put("goalType", h.goalType.name)
        put("target", h.target)
        put("unit", h.unit)
        put("scheduleType", h.schedule.type.name)
        put("scheduleDays", JSONArray(h.schedule.days.toList()))
        put("scheduleInterval", h.schedule.interval)
        put("scheduleTimesPerWeek", h.schedule.timesPerWeek)
        put("grace", h.grace)
        put("reminderTime", h.reminderTime?.toString() ?: JSONObject.NULL)
        put("archived", h.archived)
        put("pausedUntil", h.pausedUntil?.toString() ?: JSONObject.NULL)
        put("createdAt", h.createdAt.toString())
    }

    /** Parses and validates a backup JSON string, then replaces all existing data with it. */
    suspend fun importJson(jsonText: String) {
        val root = try {
            JSONObject(jsonText)
        } catch (e: Exception) {
            throw BackupImportException("Not valid JSON")
        }

        val habitsArr = root.optJSONArray("habits")
            ?: throw BackupImportException("Missing habits array")

        val habits = mutableListOf<Habit>()
        for (i in 0 until habitsArr.length()) {
            val obj = habitsArr.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            val id = obj.optString("id", "").ifBlank {
                throw BackupImportException("Habit missing id")
            }
            if (name.isBlank()) throw BackupImportException("Habit missing name")

            val scheduleDays = obj.optJSONArray("scheduleDays")?.let { arr ->
                (0 until arr.length()).mapNotNull { idx -> arr.optInt(idx, -1).takeIf { it in 1..7 } }.toSet()
            } ?: emptySet()

            habits.add(
                Habit(
                    id = id,
                    name = name,
                    category = runCatching { Category.valueOf(obj.optString("category", "OTHER")) }
                        .getOrDefault(Category.OTHER),
                    goalType = runCatching { GoalType.valueOf(obj.optString("goalType", "YES_NO")) }
                        .getOrDefault(GoalType.YES_NO),
                    target = obj.optInt("target", 1).coerceAtLeast(1),
                    unit = obj.optString("unit", ""),
                    schedule = Schedule(
                        type = runCatching { ScheduleType.valueOf(obj.optString("scheduleType", "DAILY")) }
                            .getOrDefault(ScheduleType.DAILY),
                        days = scheduleDays,
                        interval = obj.optInt("scheduleInterval", 2).coerceAtLeast(1),
                        timesPerWeek = obj.optInt("scheduleTimesPerWeek", 3).coerceIn(1, 7)
                    ),
                    grace = obj.optBoolean("grace", false),
                    reminderTime = obj.optString("reminderTime", "").ifBlank { null }
                        ?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
                    archived = obj.optBoolean("archived", false),
                    pausedUntil = obj.optString("pausedUntil", "").ifBlank { null }
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                    createdAt = obj.optString("createdAt", "").ifBlank { null }
                        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
                )
            )
        }

        val validHabitIds = habits.map { it.id }.toSet()

        val completions = mutableListOf<Completion>()
        root.optJSONArray("completions")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val habitId = obj.optString("habitId", "")
                if (habitId !in validHabitIds) continue
                val date = obj.optString("date", "").ifBlank { null }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: continue
                completions.add(
                    Completion(
                        habitId = habitId,
                        date = date,
                        value = obj.optInt("value", 0),
                        isGrace = obj.optBoolean("isGrace", false)
                    )
                )
            }
        }

        val notes = mutableListOf<HabitNote>()
        root.optJSONArray("notes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val habitId = obj.optString("habitId", "")
                if (habitId !in validHabitIds) continue
                val date = obj.optString("date", "").ifBlank { null }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: continue
                notes.add(
                    HabitNote(
                        habitId = habitId,
                        date = date,
                        mood = obj.optString("mood", "").ifBlank { null }
                            ?.let { runCatching { Mood.valueOf(it) }.getOrNull() },
                        text = obj.optString("text", "")
                    )
                )
            }
        }

        val routines = mutableListOf<Routine>()
        root.optJSONArray("routines")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val habitIdsArr = obj.optJSONArray("habitIds")
                val habitIds = if (habitIdsArr != null) {
                    (0 until habitIdsArr.length()).mapNotNull { idx -> habitIdsArr.optString(idx, null) }
                        .filter { it in validHabitIds }
                } else emptyList()
                val routineId = obj.optString("id", "")
                if (routineId.isBlank()) continue
                routines.add(
                    Routine(
                        id = routineId,
                        name = obj.optString("name", "Routine"),
                        timeLabel = runCatching { TimeLabel.valueOf(obj.optString("timeLabel", "MORNING")) }
                            .getOrDefault(TimeLabel.MORNING),
                        sortOrder = obj.optInt("sortOrder", 0),
                        habitIds = habitIds
                    )
                )
            }
        }

        repository.replaceAllData(habits, completions, notes, routines)
    }
}
