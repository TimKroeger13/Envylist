package app.neidliste.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import app.neidliste.domain.TaskStatus

@Entity(tableName = "lists")
data class TaskListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val position: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("listId")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,
    val note: String? = null,
    val color: Int,
    val status: TaskStatus = TaskStatus.OPEN,
    val position: Int,
    val createdAt: Long,
    /** Completions of other tasks after this moment make the task jealous. */
    val envyBaseline: Long,
    val completedAt: Long? = null,
    /** Set when the user ticked the close-checkbox; the task is archived shortly after. */
    val archivePendingAt: Long? = null,
    val archivedAt: Long? = null,
)

/** One "done" of a task. Kept after archiving so envy and history survive. */
@Entity(tableName = "completions", indices = [Index("listId"), Index("taskId")])
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val listId: Long,
    val timestamp: Long,
)
