package com.richie.stride.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: String): HabitEntity?

    @Query("SELECT * FROM habits")
    suspend fun getAllOnce(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(habits: List<HabitEntity>)

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM habits")
    suspend fun deleteAll()
}

@Dao
interface CompletionDao {
    @Query("SELECT * FROM completions WHERE habitId = :habitId")
    fun observeForHabit(habitId: String): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions")
    fun observeAll(): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions")
    suspend fun getAllOnce(): List<CompletionEntity>

    @Query("SELECT * FROM completions WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun get(habitId: String, date: String): CompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(completion: CompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(completions: List<CompletionEntity>)

    @Query("DELETE FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: String, date: String)

    @Query("DELETE FROM completions WHERE habitId = :habitId")
    suspend fun deleteForHabit(habitId: String)

    @Query("DELETE FROM completions")
    suspend fun deleteAll()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE habitId = :habitId")
    fun observeForHabit(habitId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun get(habitId: String, date: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Query("DELETE FROM notes WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: String, date: String)

    @Query("DELETE FROM notes WHERE habitId = :habitId")
    suspend fun deleteForHabit(habitId: String)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routine_habit_cross_ref")
    fun observeAllCrossRefs(): Flow<List<RoutineHabitCrossRef>>

    @Query("SELECT * FROM routine_habit_cross_ref WHERE routineId = :routineId ORDER BY position ASC")
    suspend fun getCrossRefsForRoutine(routineId: String): List<RoutineHabitCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM routine_habit_cross_ref WHERE routineId = :routineId")
    suspend fun clearCrossRefsForRoutine(routineId: String)

    @Query("DELETE FROM routine_habit_cross_ref WHERE habitId = :habitId")
    suspend fun clearCrossRefsForHabit(habitId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(crossRefs: List<RoutineHabitCrossRef>)

    @Query("DELETE FROM routines")
    suspend fun deleteAll()

    @Query("DELETE FROM routine_habit_cross_ref")
    suspend fun deleteAllCrossRefs()
}
