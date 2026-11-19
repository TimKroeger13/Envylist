package app.neidliste.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvyEngineTest {

    private val settings = EnvySettings(level1 = 5, level2 = 10, level3 = 15, objection = 20)

    private fun item(id: Long, envy: Int, status: TaskStatus = TaskStatus.OPEN, position: Int = id.toInt()) =
        TaskItem(
            id = id, listId = 1, title = "t$id", color = 0, status = status, position = position,
            envy = envy, level = EnvyEngine.level(envy, settings), archivePending = false, completedAt = null,
        )

    @Test
    fun envyCountsOnlyOtherCompletionsAfterBaseline() {
        val completions = listOf(
            Completion(taskId = 2, timestamp = 50),   // before baseline
            Completion(taskId = 2, timestamp = 150),
            Completion(taskId = 3, timestamp = 200),
            Completion(taskId = 1, timestamp = 250),  // own completion never counts
        )
        assertEquals(2, EnvyEngine.envyOf(1, TaskStatus.OPEN, baseline = 100, completions = completions))
        assertEquals(2, EnvyEngine.envyOf(1, TaskStatus.IN_PROGRESS, baseline = 100, completions = completions))
    }

    @Test
    fun doneTasksAreNeverJealous() {
        val completions = listOf(Completion(2, 150), Completion(3, 200))
        assertEquals(0, EnvyEngine.envyOf(1, TaskStatus.DONE, baseline = 0, completions = completions))
    }

    @Test
    fun levelsFollowThresholds() {
        assertEquals(EnvyLevel.CALM, EnvyEngine.level(4, settings))
        assertEquals(EnvyLevel.MILD, EnvyEngine.level(5, settings))
        assertEquals(EnvyLevel.ANNOYED, EnvyEngine.level(10, settings))
        assertEquals(EnvyLevel.ANGRY, EnvyEngine.level(15, settings))
        assertEquals(EnvyLevel.OBJECTION, EnvyEngine.level(20, settings))
        assertEquals(EnvyLevel.CALM, EnvyEngine.level(99, settings.copy(enabled = false)))
    }

    @Test
    fun statusCyclesForwardAndStopsAtDone() {
        assertEquals(TaskStatus.IN_PROGRESS, EnvyEngine.nextStatus(TaskStatus.OPEN))
        assertEquals(TaskStatus.DONE, EnvyEngine.nextStatus(TaskStatus.IN_PROGRESS))
        assertNull(EnvyEngine.nextStatus(TaskStatus.DONE))
    }

    @Test
    fun mostEnviousOtherTaskObjects() {
        val tasks = listOf(item(1, 0), item(2, 21), item(3, 25), item(4, 30, TaskStatus.DONE))
        assertEquals(3L, EnvyEngine.objector(1, tasks, settings)?.id)
        // a task that is itself at objection level may always be completed, nobody complains
        assertNull(EnvyEngine.objector(3, tasks, settings))
        assertNull(EnvyEngine.objector(2, tasks, settings))
        assertNull(EnvyEngine.objector(1, tasks, settings.copy(enabled = false)))
        assertNull(EnvyEngine.objector(1, listOf(item(1, 0), item(2, 19)), settings))
    }

    @Test
    fun objectionOnlyOnceWhenStartingAnotherTask() {
        val tasks = listOf(item(1, 0, TaskStatus.OPEN), item(2, 0, TaskStatus.IN_PROGRESS), item(3, 22))
        assertEquals(3L, EnvyEngine.wouldObject(tasks[0], tasks, settings)?.id) // starting → complains
        assertNull(EnvyEngine.wouldObject(tasks[1], tasks, settings))            // finishing → no second time
    }

    @Test
    fun longObjectingTaskKicksItselfToTheTop() {
        // objection = 20, kicks at 23, 26, ...
        val kicking = listOf(item(1, 0, position = 0), item(2, 23, position = 3), item(3, 21, position = 1))
        assertEquals(2L, EnvyEngine.kicker(kicking, settings)?.id)
        // not on the in-between points
        assertNull(EnvyEngine.kicker(listOf(item(1, 0, position = 0), item(2, 22, position = 3)), settings))
        // already on top → nothing to do
        assertNull(EnvyEngine.kicker(listOf(item(2, 23, position = 0), item(1, 0, position = 1)), settings))
        // exactly at the threshold it only objects, it does not kick yet
        assertNull(EnvyEngine.kicker(listOf(item(1, 0, position = 0), item(2, 20, position = 1)), settings))
    }

    @Test
    fun undoWindow() {
        assertTrue(EnvyEngine.isUndo(completedAt = 1_000, now = 1_000 + 60_000))
        assertFalse(EnvyEngine.isUndo(completedAt = 1_000, now = 1_000 + EnvyEngine.UNDO_WINDOW_MS + 1))
        assertFalse(EnvyEngine.isUndo(completedAt = null, now = 5))
    }

    @Test
    fun ordering() {
        val ids = listOf(1L, 2L, 3L, 4L)
        assertEquals(listOf(3L, 1L, 2L, 4L), EnvyEngine.moveToTop(ids, 3))
        assertEquals(listOf(1L, 3L, 2L, 4L), EnvyEngine.moveBy(ids, 3, -1))
        assertEquals(listOf(1L, 2L, 4L, 3L), EnvyEngine.moveBy(ids, 3, +1))
        assertEquals(ids, EnvyEngine.moveBy(ids, 1, -1))
        assertEquals(ids, EnvyEngine.moveBy(ids, 4, +5))
    }

    @Test
    fun sortByEnvyKeepsDoneAtBottom() {
        val tasks = listOf(item(1, 2), item(2, 9, TaskStatus.DONE), item(3, 7), item(4, 7))
        assertEquals(listOf(3L, 4L, 1L, 2L), EnvyEngine.sortByEnvy(tasks))
    }

    @Test
    fun paletteSpreadsColors() {
        val c = TaskPalette.colors
        assertEquals(c[0], TaskPalette.next(emptyList()))
        assertEquals(c[1], TaskPalette.next(listOf(c[0])))
        assertEquals(c[0], TaskPalette.next(c))
    }
}
