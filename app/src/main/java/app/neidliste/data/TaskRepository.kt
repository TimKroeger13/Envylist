package app.neidliste.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.neidliste.R
import app.neidliste.domain.Completion
import app.neidliste.domain.EnvyEngine
import app.neidliste.domain.EnvySettings
import app.neidliste.domain.TaskItem
import app.neidliste.domain.TaskPalette
import app.neidliste.domain.TaskStatus
import app.neidliste.widget.TaskListWidget
import app.neidliste.work.ArchiveWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/** Everything shown for one list: its tasks with derived envy, plus the settings used. */
data class Board(
    val list: TaskListEntity?,
    val tasks: List<TaskItem>,
    val settings: EnvySettings,
) {
    val doneCount: Int get() = tasks.count { it.status == TaskStatus.DONE }
    val anyPending: Boolean get() = tasks.any { it.archivePending }
    fun objectorFor(task: TaskItem): TaskItem? = EnvyEngine.wouldObject(task, tasks, settings)
}

sealed interface CycleResult {
    data object Changed : CycleResult
    data object Unchanged : CycleResult
    data class Objection(val task: TaskItem, val objector: TaskItem) : CycleResult
}

/**
 * Single entry point for all changes. Every write refreshes all widgets,
 * so the app and the home screen always show the same state.
 */
class TaskRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.taskDao()
    private val writeLock = Mutex()

    val lists: Flow<List<TaskListEntity>> = dao.observeLists()

    fun observeBoard(listId: Long): Flow<Board> = combine(
        dao.observeList(listId),
        dao.observeActive(listId),
        dao.observeCompletions(listId),
        settings.envy,
    ) { list, tasks, completions, s -> Board(list, buildItems(tasks, completions, s), s) }

    suspend fun board(listId: Long): Board = observeBoard(listId).first()

    fun observeArchive(listId: Long): Flow<List<TaskEntity>> = dao.observeArchived(listId)

    // --- lists ---

    suspend fun ensureDefaultList(): Long = writeLock.withLock {
        dao.lists().firstOrNull()?.id ?: dao.insertList(
            TaskListEntity(
                name = context.getString(R.string.default_list_name),
                color = TaskPalette.colors[6],
                position = 0,
                createdAt = clock(),
            ),
        )
    }

    suspend fun list(listId: Long): TaskListEntity? = dao.list(listId)

    suspend fun createList(name: String): Long = write {
        val existing = dao.lists()
        dao.insertList(
            TaskListEntity(
                name = name.trim(),
                color = TaskPalette.next(existing.map { it.color }),
                position = dao.maxListPosition() + 1,
                createdAt = clock(),
            ),
        )
    }

    suspend fun renameList(listId: Long, name: String) = write {
        dao.list(listId)?.let { dao.updateList(it.copy(name = name.trim())) }
    }

    /** Deletes a list with all its tasks. Returns the id of a list that still exists. */
    suspend fun deleteList(listId: Long): Long {
        write {
            db.withTransaction {
                dao.deleteCompletionsOfList(listId)
                dao.deleteList(listId)
            }
        }
        return ensureDefaultList()
    }

    // --- tasks ---

    suspend fun addTask(listId: Long, title: String): Long? {
        val clean = title.trim()
        if (clean.isEmpty()) return null
        return write {
            val now = clock()
            val active = dao.active(listId)
            dao.insertTask(
                TaskEntity(
                    listId = listId,
                    title = clean,
                    color = TaskPalette.next(active.map { it.color }),
                    position = dao.maxPosition(listId) + 1,
                    createdAt = now,
                    envyBaseline = now,
                ),
            )
        }
    }

    suspend fun task(taskId: Long): TaskEntity? = dao.task(taskId)

    suspend fun editTask(taskId: Long, title: String, color: Int) = write {
        val t = dao.task(taskId) ?: return@write
        val clean = title.trim().ifEmpty { t.title }
        dao.updateTask(t.copy(title = clean, color = color))
    }

    suspend fun deleteTask(taskId: Long) = write { dao.deleteTask(taskId) }

    /**
     * One tap: open → in progress → done. Completing a task while another one objects
     * returns [CycleResult.Objection] unless [force] is set.
     */
    suspend fun cycle(taskId: Long, force: Boolean = false): CycleResult {
        val task = dao.task(taskId) ?: return CycleResult.Unchanged
        val next = EnvyEngine.nextStatus(task.status) ?: return CycleResult.Unchanged
        // Jealous tasks complain once: when you *start* another task.
        if (next == TaskStatus.IN_PROGRESS && !force) {
            val board = board(task.listId)
            val item = board.tasks.firstOrNull { it.id == taskId }
            val objector = EnvyEngine.objector(taskId, board.tasks, board.settings)
            if (item != null && objector != null) return CycleResult.Objection(item, objector)
        }
        write {
            val now = clock()
            db.withTransaction {
                val fresh = dao.task(taskId) ?: return@withTransaction
                if (fresh.status != task.status) return@withTransaction // changed meanwhile
                if (next == TaskStatus.DONE) {
                    dao.updateTask(fresh.copy(status = next, completedAt = now))
                    dao.insertCompletion(CompletionEntity(taskId = taskId, listId = fresh.listId, timestamp = now))
                } else {
                    dao.updateTask(fresh.copy(status = next))
                }
            }
        }
        if (next == TaskStatus.DONE) kickIfNeeded(task.listId)
        return CycleResult.Changed
    }

    private val _kicks = MutableSharedFlow<Long>(extraBufferCapacity = 8)

    /** Ids of tasks that just shoved themselves to the top (for the little animation in the app). */
    val kicks: SharedFlow<Long> = _kicks.asSharedFlow()

    private suspend fun kickIfNeeded(listId: Long) {
        val board = board(listId)
        val kicker = EnvyEngine.kicker(board.tasks, board.settings) ?: return
        moveToTop(kicker.id)
        _kicks.tryEmit(kicker.id)
    }

    /**
     * Back to open. A completion from the last few minutes is treated as a mistake and removed
     * (everybody's envy drops again); an older one counts and the chore starts a fresh round.
     */
    suspend fun reset(taskId: Long) = write {
        val t = dao.task(taskId) ?: return@write
        val now = clock()
        db.withTransaction {
            when (t.status) {
                TaskStatus.OPEN -> Unit
                TaskStatus.IN_PROGRESS -> dao.updateTask(t.copy(status = TaskStatus.OPEN))
                TaskStatus.DONE -> if (EnvyEngine.isUndo(t.completedAt, now)) {
                    dao.deleteLatestCompletion(t.id)
                    dao.updateTask(t.copy(status = TaskStatus.OPEN, completedAt = null, archivePendingAt = null))
                } else {
                    dao.updateTask(
                        t.copy(status = TaskStatus.OPEN, completedAt = null, archivePendingAt = null, envyBaseline = now),
                    )
                }
            }
        }
    }

    // --- closing (archive) with a short grace period ---

    /** Tick (or untick within the grace period) the close-checkbox of a finished task. */
    suspend fun toggleClose(taskId: Long) {
        write {
            val t = dao.task(taskId) ?: return@write
            if (t.status != TaskStatus.DONE || t.archivedAt != null) return@write
            dao.updateTask(t.copy(archivePendingAt = if (t.archivePendingAt == null) clock() else null))
        }
        scheduleSweep()
    }

    /** Tick (or untick) "close all finished" for a list. */
    suspend fun toggleCloseAllDone(listId: Long) {
        write {
            val done = dao.active(listId).filter { it.status == TaskStatus.DONE }
            if (done.isEmpty()) return@write
            val undo = done.any { it.archivePendingAt != null }
            val now = clock()
            dao.updateTasks(done.map { it.copy(archivePendingAt = if (undo) null else now) })
        }
        scheduleSweep()
    }

    /** Archives every task whose grace period is over. Returns true if some are still pending. */
    suspend fun sweep(): Boolean {
        write {
            val now = clock()
            dao.archivePendingBefore(cutoff = now - CLOSE_GRACE_MS + 250, now = now)
        }
        return dao.earliestPending() != null
    }

    private fun scheduleSweep() {
        // Fast path while the process is alive …
        scope.launch {
            delay(CLOSE_GRACE_MS)
            if (sweep()) {
                delay(CLOSE_GRACE_MS)
                sweep()
            }
        }
        // … and a guaranteed path if the process dies (e.g. widget click while app is closed).
        WorkManager.getInstance(context).enqueueUniqueWork(
            ArchiveWorker.NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ArchiveWorker>()
                .setInitialDelay(CLOSE_GRACE_MS + 1_000, TimeUnit.MILLISECONDS)
                .build(),
        )
    }

    // --- archive ---

    suspend fun restore(taskId: Long) = write {
        val t = dao.task(taskId) ?: return@write
        dao.updateTask(t.copy(archivedAt = null, archivePendingAt = null, position = dao.maxPosition(t.listId) + 1))
    }

    suspend fun clearArchive(listId: Long) = write { dao.clearArchive(listId) }

    // --- ordering ---

    suspend fun moveToTop(taskId: Long) = reorderWith(taskId) { ids -> EnvyEngine.moveToTop(ids, taskId) }

    suspend fun moveBy(taskId: Long, delta: Int) = reorderWith(taskId) { ids -> EnvyEngine.moveBy(ids, taskId, delta) }

    suspend fun reorder(listId: Long, orderedIds: List<Long>) = write { applyOrder(listId, orderedIds) }

    suspend fun sortByEnvy(listId: Long) {
        val ids = EnvyEngine.sortByEnvy(board(listId).tasks)
        write { applyOrder(listId, ids) }
    }

    private suspend fun reorderWith(taskId: Long, transform: (List<Long>) -> List<Long>) = write {
        val t = dao.task(taskId) ?: return@write
        applyOrder(t.listId, transform(dao.active(t.listId).map { it.id }))
    }

    private suspend fun applyOrder(listId: Long, orderedIds: List<Long>) {
        val byId = dao.active(listId).associateBy { it.id }
        val ordered = orderedIds.mapNotNull { byId[it] } + byId.values.filter { it.id !in orderedIds }
        dao.updateTasks(ordered.mapIndexed { i, t -> t.copy(position = i) })
    }

    // --- helpers ---

    private suspend fun <T> write(block: suspend () -> T): T {
        val result = writeLock.withLock { block() }
        refreshWidgets()
        return result
    }

    suspend fun refreshWidgets() {
        runCatching { TaskListWidget().updateAll(context) }
    }

    companion object {
        const val CLOSE_GRACE_MS = 2_500L

        fun buildItems(tasks: List<TaskEntity>, completions: List<CompletionEntity>, s: EnvySettings): List<TaskItem> {
            val events = completions.map { Completion(it.taskId, it.timestamp) }
            return tasks.map { t ->
                val envy = EnvyEngine.envyOf(t.id, t.status, t.envyBaseline, events)
                TaskItem(
                    id = t.id,
                    listId = t.listId,
                    title = t.title,
                    color = t.color,
                    status = t.status,
                    position = t.position,
                    envy = envy,
                    level = EnvyEngine.level(envy, s),
                    archivePending = t.archivePendingAt != null,
                    completedAt = t.completedAt,
                )
            }
        }
    }
}
