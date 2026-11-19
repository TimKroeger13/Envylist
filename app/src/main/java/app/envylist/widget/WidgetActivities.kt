package app.envylist.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import app.envylist.R
import app.envylist.container
import app.envylist.data.TaskEntity
import app.envylist.data.TaskListEntity
import app.envylist.domain.EnvyVoice
import app.envylist.domain.TaskItem
import app.envylist.ui.components.DialogScaffold
import app.envylist.ui.components.ObjectionContent
import app.envylist.ui.components.QuickAddSheet
import app.envylist.ui.components.TaskEditorContent
import app.envylist.ui.components.submitOnHardwareEnter
import app.envylist.ui.theme.EnvyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Writes from short-lived dialogs run in the app scope so closing the dialog never cancels them. */
private fun Context.appLaunch(block: suspend CoroutineScope.() -> Unit) = container.scope.launch(block = block)

// ---------------------------------------------------------------- quick add

class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFloatingWindow()
        val listId = intent.getLongExtra(WidgetIntents.EXTRA_LIST_ID, -1)
        val repo = container.repository
        setContent {
            EnvyTheme {
                var list by remember { mutableStateOf<TaskListEntity?>(null) }
                LaunchedEffect(listId) { list = repo.list(listId) }
                QuickAddSheet(
                    listName = list?.name.orEmpty(),
                    onAdd = { title -> appLaunch { repo.addTask(listId, title) } },
                    onClose = ::finish,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- task actions

class TaskActionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFloatingWindow()
        val taskId = intent.getLongExtra(WidgetIntents.EXTRA_TASK_ID, -1)
        val repo = container.repository
        setContent {
            EnvyTheme {
                var task by remember { mutableStateOf<TaskEntity?>(null) }
                LaunchedEffect(taskId) {
                    task = repo.task(taskId)
                    if (task == null) finish()
                }
                DialogScaffold(onDismiss = ::finish) {
                    task?.let { t ->
                        TaskEditorContent(
                            task = t,
                            onSave = { title, color -> appLaunch { repo.editTask(t.id, title, color) } },
                            onMoveTop = { appLaunch { repo.moveToTop(t.id) }; finish() },
                            onMove = { delta -> appLaunch { repo.moveBy(t.id, delta) } },
                            onReset = { appLaunch { repo.reset(t.id) }; finish() },
                            onDelete = { appLaunch { repo.deleteTask(t.id) }; finish() },
                            onDismiss = ::finish,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- objection

class EnvyPromptActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFloatingWindow()
        val taskId = intent.getLongExtra(WidgetIntents.EXTRA_TASK_ID, -1)
        val objectorId = intent.getLongExtra(WidgetIntents.EXTRA_OBJECTOR_ID, -1)
        val repo = container.repository
        setContent {
            EnvyTheme {
                var pair by remember { mutableStateOf<Pair<TaskItem, TaskItem>?>(null) }
                LaunchedEffect(taskId) {
                    val listId = repo.task(taskId)?.listId
                    val tasks = listId?.let { repo.board(it).tasks }.orEmpty()
                    val task = tasks.firstOrNull { it.id == taskId }
                    val objector = tasks.firstOrNull { it.id == objectorId }
                    if (task == null || objector == null) {
                        appLaunch { repo.cycle(taskId, force = true) }
                        finish()
                    } else {
                        pair = task to objector
                    }
                }
                DialogScaffold(onDismiss = ::finish) {
                    pair?.let { (task, objector) ->
                        ObjectionContent(
                            objector = objector,
                            task = task,
                            onYield = {
                                appLaunch { repo.moveToTop(objector.id) }
                                Toast.makeText(this@EnvyPromptActivity, EnvyVoice.thanks(this@EnvyPromptActivity), Toast.LENGTH_SHORT).show()
                                finish()
                            },
                            onProceed = {
                                appLaunch { repo.cycle(task.id, force = true) }
                                Toast.makeText(this@EnvyPromptActivity, EnvyVoice.huff(this@EnvyPromptActivity), Toast.LENGTH_SHORT).show()
                                finish()
                            },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- choose list

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFloatingWindow()
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(Activity.RESULT_CANCELED, resultIntent())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val repo = container.repository
        setContent {
            EnvyTheme {
                LaunchedEffect(Unit) { repo.ensureDefaultList() }
                val lists by repo.lists.collectAsState(initial = emptyList())
                var newName by remember { mutableStateOf("") }
                DialogScaffold(onDismiss = ::finish) {
                    Text(stringResource(R.string.choose_list), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))
                    lists.forEach { list ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { choose(list.id) }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                        ) {
                            Box(Modifier.size(14.dp).clip(CircleShape).background(Color(list.color)))
                            Spacer(Modifier.width(12.dp))
                            Text(list.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text(stringResource(R.string.new_list)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.weight(1f),
                        )
                        FilledIconButton(
                            onClick = {
                                val name = newName
                                lifecycleScope.launch { choose(repo.createList(name)) }
                            },
                            enabled = newName.isNotBlank(),
                        ) { Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.add)) }
                    }
                }
            }
        }
    }

    private fun choose(listId: Long) {
        lifecycleScope.launch {
            assignList(this@WidgetConfigActivity, appWidgetId, listId)
            setResult(Activity.RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

/** Shows [listId] in the widget with the given id. */
suspend fun assignList(context: Context, appWidgetId: Int, listId: Long) {
    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
    updateAppWidgetState(context, glanceId) { it[TaskListWidget.ListIdKey] = listId }
    TaskListWidget().update(context, glanceId)
}

// ---------------------------------------------------------------- pinned from the app

/** Called by the launcher after "Add widget to home screen": shows the list that was open in the app. */
class PinWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val listId = intent.getLongExtra(WidgetIntents.EXTRA_LIST_ID, -1)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || listId < 0) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                assignList(context, appWidgetId, listId)
            } finally {
                pending.finish()
            }
        }
    }
}

// ---------------------------------------------------------------- envy info

/** Tap on an envy badge in the widget: explains what the face means. */
class EnvyInfoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFloatingWindow()
        val taskId = intent.getLongExtra(WidgetIntents.EXTRA_TASK_ID, -1)
        val repo = container.repository
        setContent {
            EnvyTheme {
                var board by remember { mutableStateOf<app.envylist.data.Board?>(null) }
                LaunchedEffect(taskId) {
                    val listId = repo.task(taskId)?.listId
                    board = listId?.let { repo.board(it) }
                    if (board?.tasks?.none { it.id == taskId } != false) finish()
                }
                DialogScaffold(onDismiss = ::finish) {
                    val b = board
                    val task = b?.tasks?.firstOrNull { it.id == taskId }
                    if (b != null && task != null) {
                        app.envylist.ui.components.EnvyInfoContent(task, b.settings, onClose = ::finish)
                    }
                }
            }
        }
    }
}
