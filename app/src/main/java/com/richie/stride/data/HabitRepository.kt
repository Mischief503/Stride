package com.richie.stride.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class HabitRepository(private val db: AppDatabase) {

    private val habitDao = db.habitDao()
    private val completionDao = db.completionDao()
    private val noteDao = db.noteDao()
    private val routineDao = db.routineDao()

    val habits: Flow<List<Habit>> = habitDao.observeAll().map { list -> list.map { it.toDomain() } }

    /** All completions, grouped by habit id then keyed by date, for cheap stats lookups. */
    val completionsByHabit: Flow<Map<String, Map<LocalDate, Completion>>> =
        completionDao.observeAll().map { list ->
            list.map { it.toDomain() }
                .groupBy { it.habitId }
                .mapValues { (_, v) -> v.associateBy { c -> c.date } }
        }

    val routines: Flow<List<Routine>> = combine(
        routineDao.observeAll(),
        routineDao.observeAllCrossRefs()
    ) { routineEntities, crossRefs ->
        routineEntities.map { r ->
            val ids = crossRefs.filter { it.routineId == r.id }.sortedBy { it.position }.map { it.habitId }
            r.toDomain(ids)
        }
    }

    fun notesForHabit(habitId: String): Flow<Map<LocalDate, HabitNote>> =
        noteDao.observeForHabit(habitId).map { list -> list.map { it.toDomain() }.associateBy { it.date } }

    val allNotes: Flow<List<HabitNote>> = noteDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getHabit(id: String): Habit? = habitDao.getById(id)?.toDomain()

    suspend fun saveHabit(habit: Habit) = habitDao.upsert(habit.toEntity())

    fun newHabitId(): String = UUID.randomUUID().toString()

    suspend fun setArchived(id: String, archived: Boolean) {
        val entity = habitDao.getById(id) ?: return
        habitDao.update(entity.copy(archived = archived))
    }

    suspend fun setPausedUntil(id: String, until: LocalDate?) {
        val entity = habitDao.getById(id) ?: return
        habitDao.update(entity.copy(pausedUntil = until?.toString()))
    }

    suspend fun deletePermanently(id: String) {
        habitDao.deleteById(id)
        completionDao.deleteForHabit(id)
        noteDao.deleteForHabit(id)
        routineDao.clearCrossRefsForHabit(id)
    }

    suspend fun toggleYesNo(habitId: String, date: LocalDate) {
        val existing = completionDao.get(habitId, date.toString())
        if (existing != null && !existing.isGrace && existing.value >= 1) {
            completionDao.delete(habitId, date.toString())
        } else {
            completionDao.upsert(CompletionEntity(habitId, date.toString(), 1, false))
        }
    }

    suspend fun setValue(habitId: String, date: LocalDate, value: Int) {
        if (value <= 0) {
            completionDao.delete(habitId, date.toString())
        } else {
            completionDao.upsert(CompletionEntity(habitId, date.toString(), value, false))
        }
    }

    suspend fun stepValue(habitId: String, date: LocalDate, delta: Int): Int {
        val current = completionDao.get(habitId, date.toString())?.takeIf { !it.isGrace }?.value ?: 0
        val next = (current + delta).coerceAtLeast(0)
        setValue(habitId, date, next)
        return next
    }

    suspend fun setGrace(habitId: String, date: LocalDate) {
        completionDao.upsert(CompletionEntity(habitId, date.toString(), 0, true))
    }

    suspend fun clearCompletion(habitId: String, date: LocalDate) {
        completionDao.delete(habitId, date.toString())
    }

    suspend fun saveNote(note: HabitNote) {
        if (note.text.isBlank() && note.mood == null) {
            noteDao.delete(note.habitId, note.date.toString())
        } else {
            noteDao.upsert(note.toEntity())
        }
    }

    suspend fun saveRoutine(routine: Routine) {
        routineDao.upsert(RoutineEntity(routine.id, routine.name, routine.timeLabel.name, routine.sortOrder))
        routineDao.clearCrossRefsForRoutine(routine.id)
        routineDao.insertCrossRefs(routine.habitIds.mapIndexed { idx, habitId ->
            RoutineHabitCrossRef(routine.id, habitId, idx)
        })
    }

    suspend fun deleteRoutine(id: String) {
        routineDao.clearCrossRefsForRoutine(id)
        routineDao.deleteById(id)
    }

    suspend fun wipeAllData() {
        completionDao.deleteAll()
        noteDao.deleteAll()
        routineDao.deleteAllCrossRefs()
        routineDao.deleteAll()
        habitDao.deleteAll()
    }

    /** Used by import: wipes existing data and replaces it wholesale with a parsed backup. */
    suspend fun replaceAllData(
        newHabits: List<Habit>,
        newCompletions: List<Completion>,
        newNotes: List<HabitNote>,
        newRoutines: List<Routine>
    ) {
        wipeAllData()
        habitDao.upsertAll(newHabits.map { it.toEntity() })
        completionDao.upsertAll(newCompletions.map { it.toEntity() })
        newNotes.forEach { noteDao.upsert(it.toEntity()) }
        newRoutines.forEach { r ->
            routineDao.upsert(RoutineEntity(r.id, r.name, r.timeLabel.name, r.sortOrder))
            routineDao.insertCrossRefs(r.habitIds.mapIndexed { idx, habitId ->
                RoutineHabitCrossRef(r.id, habitId, idx)
            })
        }
    }
}
