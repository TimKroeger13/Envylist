package app.envylist.domain

/** Lifecycle of a task: one tap moves it one step forward. */
enum class TaskStatus { OPEN, IN_PROGRESS, DONE }

/** How jealous a task currently is, derived from its envy points and the user's thresholds. */
enum class EnvyLevel { CALM, MILD, ANNOYED, ANGRY, OBJECTION }

data class EnvySettings(
    val enabled: Boolean = true,
    val level1: Int = 5,
    val level2: Int = 10,
    val level3: Int = 15,
    val objection: Int = 20,
)

/** A task as shown in the app and the widget, including its derived envy. */
data class TaskItem(
    val id: Long,
    val listId: Long,
    val title: String,
    val color: Int,
    val status: TaskStatus,
    val position: Int,
    val envy: Int,
    val level: EnvyLevel,
    val archivePending: Boolean,
    val completedAt: Long?,
)

/** A completion that made the other tasks of the list jealous. */
data class Completion(val taskId: Long, val timestamp: Long)
