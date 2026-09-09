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
    private val habitSlotDao = db.habitSlotDao()

    val habits: Flow<List<Habit>> = combine(habitDao.observeAll(), habitSlotDao.observeAll()) { habitEntities, slotEntities ->
        val slotsByHabit = slotEntities.groupBy { it.habitId }
            .mapValues { (_, v) -> v.sortedBy { s -> s.sortOrder }.map { s -> s.toDomain() } }
        habitEntities.map { it.toDomain(slotsByHabit[it.id] ?: emptyList()) }
    }

    /**
     * All completions, grouped by habit id, then slot id, then keyed by date. Used directly by
     * Today/Calendar for per-slot rows and aggregate "done for the day" checks, and by Insights
     * for the per-slot comparison. [completionsByHabit] below is the older, simpler view (just
     * the default slot) that Detail and Habits still use, since they don't show per-slot data.
     */
    val completionsBySlot: Flow<Map<String, Map<String, Map<LocalDate, Completion>>>> =
        completionDao.observeAll().map { list ->
            list.map { it.toDomain() }
                .groupBy { it.habitId }
                .mapValues { (_, byHabit) ->
                    byHabit.groupBy { it.slotId }.mapValues { (_, bySlot) -> bySlot.associateBy { c -> c.date } }
                }
        }

    /**
     * Backward-compatible view used by every existing screen: just the default slot's
     * completions, in the exact same (habitId -> date -> Completion) shape as before slots
     * existed. A single-occurrence habit's only slot IS the default slot, so this is a
     * no-op/identical view for every habit until something actually adds a second slot.
     */
    val completionsByHabit: Flow<Map<String, Map<LocalDate, Completion>>> =
        completionsBySlot.map { bySlot -> bySlot.mapValues { (_, slots) -> slots[DEFAULT_SLOT_ID] ?: emptyMap() } }

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

    suspend fun getHabit(id: String): Habit? {
        val entity = habitDao.getById(id) ?: return null
        val slots = habitSlotDao.getForHabit(id).map { it.toDomain() }
        return entity.toDomain(slots)
    }

    suspend fun saveHabit(habit: Habit) {
        // If editing removed a slot entirely (not just cleared its reminder), its completions
        // become orphaned - nothing references that slotId anymore, but they'd sit in the
        // table forever otherwise. Clean them up here, once, rather than leaking rows silently.
        val previousSlotIds = habitSlotDao.getForHabit(habit.id).map { it.slotId }.toSet()
        val newSlotIds = habit.slots.map { it.id }.toSet()
        val removedSlotIds = previousSlotIds - newSlotIds

        habitDao.upsert(habit.toEntity())
        habitSlotDao.clearForHabit(habit.id)
        habitSlotDao.upsertAll(habit.slots.mapIndexed { idx, slot -> slot.toEntity(habit.id, idx) })
        removedSlotIds.forEach { removedId -> completionDao.deleteForSlot(habit.id, removedId) }
    }

    fun newHabitId(): String = UUID.randomUUID().toString()
    fun newSlotId(): String = UUID.randomUUID().toString()

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
        habitSlotDao.clearForHabit(id)
    }

    suspend fun toggleYesNo(habitId: String, date: LocalDate, slotId: String = DEFAULT_SLOT_ID) {
        val existing = completionDao.get(habitId, slotId, date.toString())
        if (existing != null && !existing.isGrace && existing.value >= 1) {
            completionDao.delete(habitId, slotId, date.toString())
        } else {
            completionDao.upsert(CompletionEntity(habitId, slotId, date.toString(), 1, false))
        }
    }

    suspend fun setValue(habitId: String, date: LocalDate, value: Int, slotId: String = DEFAULT_SLOT_ID) {
        if (value <= 0) {
            completionDao.delete(habitId, slotId, date.toString())
        } else {
            completionDao.upsert(CompletionEntity(habitId, slotId, date.toString(), value, false))
        }
    }

    suspend fun stepValue(habitId: String, date: LocalDate, delta: Int, slotId: String = DEFAULT_SLOT_ID): Int {
        val current = completionDao.get(habitId, slotId, date.toString())?.takeIf { !it.isGrace }?.value ?: 0
        val next = (current + delta).coerceAtLeast(0)
        setValue(habitId, date, next, slotId)
        return next
    }

    suspend fun setGrace(habitId: String, date: LocalDate, slotId: String = DEFAULT_SLOT_ID) {
        completionDao.upsert(CompletionEntity(habitId, slotId, date.toString(), 0, true))
    }

    suspend fun clearCompletion(habitId: String, date: LocalDate, slotId: String = DEFAULT_SLOT_ID) {
        completionDao.delete(habitId, slotId, date.toString())
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
        habitSlotDao.deleteAll()
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
        newHabits.forEach { h ->
            habitSlotDao.upsertAll(h.slots.mapIndexed { idx, slot -> slot.toEntity(h.id, idx) })
        }
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
