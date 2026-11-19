package app.neidliste.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.neidliste.R
import app.neidliste.container
import app.neidliste.data.Board
import app.neidliste.domain.EnvyLevel
import app.neidliste.domain.EnvyVoice
import app.neidliste.domain.TaskItem
import app.neidliste.domain.TaskPalette
import app.neidliste.domain.TaskStatus
import app.neidliste.widget.actions.CloseAllDoneAction
import app.neidliste.widget.actions.CycleStatusAction
import app.neidliste.widget.actions.ResetAction
import app.neidliste.widget.actions.ToggleCloseAction
import app.neidliste.widget.actions.WidgetKeys

class TaskListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskListWidget()
}

/**
 * The home-screen list. It mirrors the app 1:1: tap to advance a task, ↺ to reset,
 * the checkbox closes finished tasks (with a short glow and undo window), the dot opens actions.
 */
class TaskListWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = context.container.repository
        val fallbackListId = repo.ensureDefaultList()
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            val listId = currentState<Preferences>()[ListIdKey] ?: fallbackListId
            val board by remember(listId) { repo.observeBoard(listId) }.collectAsState(initial = null)
            GlanceTheme {
                WidgetBody(board, listId, appWidgetId)
            }
        }
    }

    companion object {
        val ListIdKey = longPreferencesKey("list_id")
    }
}

private fun color(argb: Int, alpha: Float = 1f): ColorProvider {
    val c = Color(argb).copy(alpha = alpha * Color(argb).alpha)
    return ColorProvider(day = c, night = c)
}

@Composable
private fun WidgetBody(board: Board?, listId: Long, appWidgetId: Int) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        when {
            board == null -> Unit
            board.list == null -> Text(
                text = context.getString(R.string.list_missing),
                style = TextStyle(color = GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.fillMaxWidth().padding(8.dp)
                    .clickable(actionStartActivity(WidgetIntents.configure(context, appWidgetId))),
            )
            else -> {
                Header(board, listId, appWidgetId)
                Spacer(GlanceModifier.height(6.dp))
                if (board.tasks.isEmpty()) {
                    Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = context.getString(R.string.empty_list),
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                        )
                    }
                } else {
                    LazyColumn(GlanceModifier.fillMaxSize()) {
                        items(board.tasks, itemId = { it.id }) { task ->
                            TaskRow(task, board)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(board: Board, listId: Long, appWidgetId: Int) {
    val context = LocalContext.current
    val list = board.list ?: return
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.defaultWeight()
                .clickable(actionStartActivity(WidgetIntents.openApp(context, listId))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = list.name,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = context.getString(R.string.progress, board.doneCount, board.tasks.size),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
        }
        HeaderIcon(
            R.drawable.ic_swap, R.string.cd_switch_list,
            actionStartActivity(WidgetIntents.configure(context, appWidgetId)),
        )
        if (board.doneCount > 0) {
            HeaderIcon(
                if (board.anyPending) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_empty,
                R.string.close_all_done,
                actionRunCallback<CloseAllDoneAction>(actionParametersOf(WidgetKeys.ListId to listId)),
                tint = if (board.anyPending) color(GLOW_GREEN) else GlanceTheme.colors.onSurfaceVariant,
            )
        }
        Box(
            modifier = GlanceModifier.size(36.dp).cornerRadius(18.dp)
                .background(GlanceTheme.colors.primary)
                .clickable(actionStartActivity(WidgetIntents.quickAdd(context, listId))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_add),
                contentDescription = context.getString(R.string.add),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                modifier = GlanceModifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun HeaderIcon(icon: Int, description: Int, action: Action, tint: ColorProvider? = null) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier.size(36.dp).clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = context.getString(description),
            colorFilter = ColorFilter.tint(tint ?: GlanceTheme.colors.onSurfaceVariant),
            modifier = GlanceModifier.size(22.dp),
        )
    }
}

@Composable
private fun TaskRow(task: TaskItem, board: Board) {
    val context = LocalContext.current
    val done = task.status == TaskStatus.DONE
    // Calm base colour; only urgency (envy) adds colour.
    val urgency = TaskPalette.rowTint(task.level)
    val rowColor = when {
        task.archivePending -> color(GLOW_GREEN, 0.45f)
        done -> GlanceTheme.colors.surface
        urgency != null -> color(urgency, 0.30f)
        else -> GlanceTheme.colors.secondaryContainer
    }
    // Completing while another task objects opens the objection dialog instead.
    val objector = board.objectorFor(task)
    val mainAction: Action? = when {
        done -> null
        objector != null -> actionStartActivity(WidgetIntents.envyPrompt(context, task.id, objector.id))
        else -> actionRunCallback<CycleStatusAction>(actionParametersOf(WidgetKeys.TaskId to task.id))
    }

    Column(GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().cornerRadius(14.dp).background(rowColor)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ⋮ → actions (move to top, edit, reset, delete)
            Box(
                modifier = GlanceModifier.size(width = 30.dp, height = 44.dp)
                    .clickable(actionStartActivity(WidgetIntents.taskActions(context, task.id))),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_more),
                    contentDescription = context.getString(R.string.cd_color),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.size(18.dp),
                )
            }

            val main = GlanceModifier.defaultWeight().padding(vertical = 6.dp)
            Column(if (mainAction != null) main.clickable(mainAction) else main) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIcon(task)
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        text = task.title,
                        maxLines = 2,
                        style = TextStyle(
                            color = if (done) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                            fontSize = 15.sp,
                            fontWeight = if (task.status == TaskStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal,
                            textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                        ),
                    )
                }
                val line = EnvyVoice.line(context, task)
                if (line != null) {
                    Text(
                        text = "„$line“",
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                        ),
                        modifier = GlanceModifier.padding(start = 24.dp, top = 1.dp),
                    )
                }
            }

            if (task.level != EnvyLevel.CALM) EnvyBadge(task)

            if (done) {
                RowIcon(
                    R.drawable.ic_reset, R.string.cd_reset,
                    actionRunCallback<ResetAction>(actionParametersOf(WidgetKeys.TaskId to task.id)),
                    GlanceTheme.colors.onSurfaceVariant,
                    size = 18,
                )
                RowIcon(
                    if (task.archivePending) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_empty,
                    R.string.cd_close_task,
                    actionRunCallback<ToggleCloseAction>(actionParametersOf(WidgetKeys.TaskId to task.id)),
                    if (task.archivePending) color(GLOW_GREEN) else GlanceTheme.colors.onSurface,
                    size = 24,
                )
            }
        }
    }
}

@Composable
private fun StatusIcon(task: TaskItem) {
    val context = LocalContext.current
    val (icon, tint, cd) = when (task.status) {
        TaskStatus.OPEN -> Triple(R.drawable.ic_status_open, GlanceTheme.colors.onSurfaceVariant, R.string.cd_status_open)
        TaskStatus.IN_PROGRESS -> Triple(R.drawable.ic_status_progress, color(PROGRESS_YELLOW), R.string.cd_status_progress)
        TaskStatus.DONE -> Triple(R.drawable.ic_status_done, color(DONE_GREEN), R.string.cd_status_done)
    }
    Image(
        provider = ImageProvider(icon),
        contentDescription = context.getString(cd),
        colorFilter = ColorFilter.tint(tint),
        modifier = GlanceModifier.size(18.dp),
    )
}

@Composable
private fun EnvyBadge(task: TaskItem) {
    Box(
        modifier = GlanceModifier.padding(horizontal = 4.dp)
            .cornerRadius(10.dp)
            .background(color(TaskPalette.envyColor(task.level)))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = "${TaskPalette.envyEmoji(task.level)} ${task.envy}",
            style = TextStyle(
                color = color(TaskPalette.onEnvyColor(task.level)),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun RowIcon(icon: Int, description: Int, action: Action, tint: ColorProvider, size: Int) {
    val context = LocalContext.current
    Box(modifier = GlanceModifier.size(38.dp).clickable(action), contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = context.getString(description),
            colorFilter = ColorFilter.tint(tint),
            modifier = GlanceModifier.size(size.dp),
        )
    }
}

internal const val GLOW_GREEN = 0xFF43C463.toInt()
internal const val DONE_GREEN = 0xFF2E9E5B.toInt()
internal const val PROGRESS_YELLOW = 0xFFFFB300.toInt()
