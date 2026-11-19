package app.envylist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // --- lists ---

    @Query("SELECT * FROM lists ORDER BY position, id")
    fun observeLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM lists ORDER BY position, id")
    suspend fun lists(): List<TaskListEntity>

    @Query("SELECT * FROM lists WHERE id = :id")
    fun observeList(id: Long): Flow<TaskListEntity?>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun list(id: Long): TaskListEntity?

    @Insert
    suspend fun insertList(list: TaskListEntity): Long

    @Update
    suspend fun updateList(list: TaskListEntity)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun deleteList(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM lists")
    suspend fun maxListPosition(): Int

    // --- tasks ---

    @Query("SELECT * FROM tasks WHERE listId = :listId AND archivedAt IS NULL ORDER BY position, id")
    fun observeActive(listId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE listId = :listId AND archivedAt IS NULL ORDER BY position, id")
    suspend fun active(listId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE listId = :listId AND archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun observeArchived(listId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun task(id: Long): TaskEntity?

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Update
    suspend fun updateTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("DELETE FROM tasks WHERE listId = :listId AND archivedAt IS NOT NULL")
    suspend fun clearArchive(listId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM tasks WHERE listId = :listId AND archivedAt IS NULL")
    suspend fun maxPosition(listId: Long): Int

    @Query("UPDATE tasks SET archivedAt = :now, archivePendingAt = NULL WHERE archivePendingAt IS NOT NULL AND archivePendingAt <= :cutoff")
    suspend fun archivePendingBefore(cutoff: Long, now: Long): Int

    @Query("SELECT MIN(archivePendingAt) FROM tasks WHERE archivePendingAt IS NOT NULL")
    suspend fun earliestPending(): Long?

    // --- completions ---

    @Query("SELECT * FROM completions WHERE listId = :listId")
    fun observeCompletions(listId: Long): Flow<List<CompletionEntity>>

    @Insert
    suspend fun insertCompletion(completion: CompletionEntity)

    @Query("DELETE FROM completions WHERE id = (SELECT id FROM completions WHERE taskId = :taskId ORDER BY timestamp DESC LIMIT 1)")
    suspend fun deleteLatestCompletion(taskId: Long)

    @Query("DELETE FROM completions WHERE listId = :listId")
    suspend fun deleteCompletionsOfList(listId: Long)
}
