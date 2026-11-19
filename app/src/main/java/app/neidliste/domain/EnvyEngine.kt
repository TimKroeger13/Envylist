package app.neidliste.domain

/**
 * Pure rules of the envy game.
 *
 * Envy is never stored. A task's envy is the number of completions of *other* tasks in the
 * same list since its baseline (creation, or the last time it was restarted as a recurring chore).
 * Undoing an accidental completion removes that completion, so everybody's envy drops again.
 */
object EnvyEngine {

    /** Resetting a task completed less than this long ago counts as "undo" rather than "do it again". */
    const val UNDO_WINDOW_MS: Long = 10 * 60 * 1000L

    fun envyOf(taskId: Long, status: TaskStatus, baseline: Long, completions: List<Completion>): Int {
        if (status == TaskStatus.DONE) return 0
        return completions.count { it.taskId != taskId && it.timestamp > baseline }
    }

    fun level(envy: Int, settings: EnvySettings): EnvyLevel = when {
        !settings.enabled -> EnvyLevel.CALM
        envy >= settings.objection -> EnvyLevel.OBJECTION
        envy >= settings.level3 -> EnvyLevel.ANGRY
        envy >= settings.level2 -> EnvyLevel.ANNOYED
        envy >= settings.level1 -> EnvyLevel.MILD
        else -> EnvyLevel.CALM
    }

    /** Next status for a tap; `null` means a tap does nothing (done tasks need an explicit reset). */
    fun nextStatus(current: TaskStatus): TaskStatus? = when (current) {
        TaskStatus.OPEN -> TaskStatus.IN_PROGRESS
        TaskStatus.IN_PROGRESS -> TaskStatus.DONE
        TaskStatus.DONE -> null
    }

    /**
     * The task that raises an objection when [taskId] is about to be completed,
     * i.e. the most envious other unfinished task at or above the objection threshold.
     */
    fun objector(taskId: Long, tasks: List<TaskItem>, settings: EnvySettings): TaskItem? {
        if (!settings.enabled) return null
        // Finishing a task that is itself at objection level is always fine: nobody complains.
        val self = tasks.firstOrNull { it.id == taskId }
        if (self != null && self.envy >= settings.objection) return null
        return tasks
            .filter { it.id != taskId && it.status != TaskStatus.DONE && it.envy >= settings.objection }
            .sortedWith(compareByDescending<TaskItem> { it.envy }.thenBy { it.position })
            .firstOrNull()
    }

    /**
     * Whether tapping [task] would *start* it while another task objects. Jealous tasks complain once,
     * when you begin working on something else, not again when you finish it.
     */
    fun wouldObject(task: TaskItem, tasks: List<TaskItem>, settings: EnvySettings): TaskItem? =
        if (nextStatus(task.status) == TaskStatus.IN_PROGRESS) objector(task.id, tasks, settings) else null

    /** A task this many points beyond the objection level pushes itself to the top again. */
    const val KICK_EVERY = 3

    /**
     * After a completion: the most envious task that has been objecting for a while
     * (every [KICK_EVERY] points beyond the threshold) shoves itself to the top, unless it is already there.
     */
    fun kicker(tasks: List<TaskItem>, settings: EnvySettings): TaskItem? {
        if (!settings.enabled) return null
        val candidate = tasks
            .filter { it.status != TaskStatus.DONE && it.envy > settings.objection }
            .sortedWith(compareByDescending<TaskItem> { it.envy }.thenBy { it.position })
            .firstOrNull() ?: return null
        if ((candidate.envy - settings.objection) % KICK_EVERY != 0) return null
        val first = tasks.minByOrNull { it.position }
        return if (first?.id == candidate.id) null else candidate
    }

    fun isUndo(completedAt: Long?, now: Long): Boolean =
        completedAt != null && now - completedAt in 0..UNDO_WINDOW_MS

    // --- ordering helpers (operate on ids in display order) ---

    fun moveToTop(ids: List<Long>, id: Long): List<Long> =
        if (id !in ids) ids else listOf(id) + ids.filter { it != id }

    fun moveBy(ids: List<Long>, id: Long, delta: Int): List<Long> {
        val from = ids.indexOf(id)
        if (from < 0) return ids
        val to = (from + delta).coerceIn(0, ids.lastIndex)
        return ids.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** Unfinished tasks by envy (most jealous first), finished tasks keep their order at the bottom. */
    fun sortByEnvy(tasks: List<TaskItem>): List<Long> {
        val (done, open) = tasks.sortedBy { it.position }.partition { it.status == TaskStatus.DONE }
        return open.sortedByDescending { it.envy }.map { it.id } + done.map { it.id }
    }
}
