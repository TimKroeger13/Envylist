@file:OptIn(ExperimentalMaterial3Api::class)

package app.neidliste.ui.main

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.neidliste.R
import app.neidliste.data.Board
import app.neidliste.data.TaskEntity
import app.neidliste.data.TaskListEntity
import app.neidliste.domain.EnvyVoice
import app.neidliste.domain.TaskItem
import app.neidliste.ui.components.DialogCard
import app.neidliste.ui.components.ObjectionContent
import app.neidliste.ui.components.TaskEditorContent
import app.neidliste.ui.components.TaskRow
import app.neidliste.ui.theme.DoneGreen
import app.neidliste.widget.PinWidgetReceiver
import app.neidliste.widget.TaskListWidgetReceiver
import app.neidliste.widget.WidgetIntents
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private enum class ListDialog { New, Rename, Delete }

@Composable
fun BoardScreen(viewModel: MainViewModel, onOpenArchive: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val board by viewModel.board.collectAsStateWithLifecycle()
    val objection by viewModel.objection.collectAsStateWithLifecycle()

    var editingTaskId by remember { mutableStateOf<Long?>(null) }
    var listDialog by remember { mutableStateOf<ListDialog?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        val b = board
                        if (b != null && b.doneCount > 0) {
                            IconButton(onClick = viewModel::closeAllDone) {
                                Icon(
                                    painterResource(if (b.anyPending) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_empty),
                                    contentDescription = stringResource(R.string.close_all_done),
                                    tint = if (b.anyPending) DoneGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = onOpenArchive) {
                            Icon(painterResource(R.drawable.ic_archive), contentDescription = stringResource(R.string.archive))
                        }
                        OverflowMenu(
                            onNewList = { listDialog = ListDialog.New },
                            onRenameList = { listDialog = ListDialog.Rename },
                            onDeleteList = { listDialog = ListDialog.Delete },
                            onSortByEnvy = viewModel::sortByEnvy,
                            onAddWidget = { selected?.let { requestPinWidget(context, it) } },
                            onSettings = onOpenSettings,
                        )
                    },
                )
                ListTabs(
                    lists = lists,
                    selected = selected,
                    onSelect = viewModel::select,
                    onNew = { listDialog = ListDialog.New },
                )
            }
        },
        bottomBar = { AddBar(onAdd = viewModel::addTask) },
    ) { padding ->
        TaskList(
            board = board,
            padding = padding,
            viewModel = viewModel,
            onEdit = { editingTaskId = it },
        )
    }

    editingTaskId?.let { id ->
        TaskEditorDialog(viewModel, id, onDismiss = { editingTaskId = null })
    }

    objection?.let { o ->
        Dialog(onDismissRequest = viewModel::dismissObjection) {
            DialogCard {
                ObjectionContent(
                    objector = o.objector,
                    task = o.task,
                    onYield = {
                        viewModel.yieldToObjector()
                        Toast.makeText(context, EnvyVoice.thanks(context), Toast.LENGTH_SHORT).show()
                    },
                    onProceed = {
                        viewModel.proceedAnyway()
                        Toast.makeText(context, EnvyVoice.huff(context), Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }

    val currentList = lists.firstOrNull { it.id == selected }
    when (listDialog) {
        ListDialog.New -> NameDialog(
            title = stringResource(R.string.new_list),
            initial = "",
            onConfirm = { viewModel.createList(it); listDialog = null },
            onDismiss = { listDialog = null },
        )
        ListDialog.Rename -> NameDialog(
            title = stringResource(R.string.rename_list),
            initial = currentList?.name.orEmpty(),
            onConfirm = { viewModel.renameList(it); listDialog = null },
            onDismiss = { listDialog = null },
        )
        ListDialog.Delete -> AlertDialog(
            onDismissRequest = { listDialog = null },
            title = { Text(stringResource(R.string.delete_list)) },
            text = { Text(stringResource(R.string.delete_list_confirm, currentList?.name.orEmpty())) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteList(); listDialog = null }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { listDialog = null }) { Text(stringResource(R.string.cancel)) } },
        )
        null -> Unit
    }
}

@Composable
private fun TaskList(board: Board?, padding: PaddingValues, viewModel: MainViewModel, onEdit: (Long) -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val tasks = board?.tasks.orEmpty()

    // Local copy while dragging, so the list follows the finger before the database catches up.
    var local by remember { mutableStateOf(tasks) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(tasks) { if (!dragging) local = tasks }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIdx = local.indexOfFirst { it.id == from.key }
        val toIdx = local.indexOfFirst { it.id == to.key }
        if (fromIdx >= 0 && toIdx >= 0) {
            local = local.toMutableList().apply { add(toIdx, removeAt(fromIdx)) }
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "progress") { ProgressHeader(board) }
        if (board != null && local.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.empty_list),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                )
            }
        }
        items(local, key = { it.id }) { task ->
            ReorderableItem(reorderState, key = task.id) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 10.dp else 0.dp, label = "dragElevation")
                TaskRow(
                    task = task,
                    speech = speechOf(context, task),
                    onTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        viewModel.cycle(task.id)
                    },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.reset(task.id)
                    },
                    onDot = { onEdit(task.id) },
                    onReset = { viewModel.reset(task.id) },
                    onToggleClose = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        viewModel.toggleClose(task.id)
                    },
                    dragHandle = Modifier.draggableHandle(
                        onDragStarted = {
                            dragging = true
                            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        },
                        onDragStopped = {
                            dragging = false
                            viewModel.reorder(local.map { it.id })
                        },
                    ),
                    modifier = Modifier.shadow(elevation, MaterialTheme.shapes.large),
                )
            }
        }
    }
}

private fun speechOf(context: Context, task: TaskItem): String? = EnvyVoice.line(context, task)

@Composable
private fun ProgressHeader(board: Board?) {
    if (board == null || board.tasks.isEmpty()) return
    val total = board.tasks.size
    val done = board.doneCount
    Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        Text(
            stringResource(R.string.progress_long, done, total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            color = DoneGreen,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
        )
    }
}

@Composable
private fun ListTabs(lists: List<TaskListEntity>, selected: Long?, onSelect: (Long) -> Unit, onNew: () -> Unit) {
    if (lists.isEmpty()) return
    val index = lists.indexOfFirst { it.id == selected }.coerceAtLeast(0)
    PrimaryScrollableTabRow(selectedTabIndex = index, edgePadding = 8.dp) {
        lists.forEach { list ->
            Tab(
                selected = list.id == selected,
                onClick = { onSelect(list.id) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(list.color)))
                        Spacer(Modifier.width(6.dp))
                        Text(list.name, maxLines = 1)
                    }
                },
            )
        }
        Tab(
            selected = false,
            onClick = onNew,
            icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.new_list)) },
        )
    }
}

@Composable
private fun OverflowMenu(
    onNewList: () -> Unit,
    onRenameList: () -> Unit,
    onDeleteList: () -> Unit,
    onSortByEnvy: () -> Unit,
    onAddWidget: () -> Unit,
    onSettings: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.cd_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            @Composable
            fun entry(text: Int, icon: Int, action: () -> Unit) = DropdownMenuItem(
                text = { Text(stringResource(text)) },
                leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
                onClick = { open = false; action() },
            )
            entry(R.string.sort_by_envy, R.drawable.ic_sort, onSortByEnvy)
            entry(R.string.new_list, R.drawable.ic_add, onNewList)
            entry(R.string.rename_list, R.drawable.ic_swap, onRenameList)
            entry(R.string.delete_list, R.drawable.ic_delete, onDeleteList)
            entry(R.string.add_widget, R.drawable.ic_dot, onAddWidget)
            entry(R.string.settings, R.drawable.ic_settings, onSettings)
        }
    }
}

@Composable
private fun AddBar(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    fun submit() {
        if (text.isNotBlank()) {
            onAdd(text)
            text = ""
        }
    }
    Surface(tonalElevation = 3.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.add_task_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = { submit() }, enabled = text.isNotBlank(), modifier = Modifier.size(52.dp)) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.add))
            }
        }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.list_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun TaskEditorDialog(viewModel: MainViewModel, taskId: Long, onDismiss: () -> Unit) {
    var task by remember { mutableStateOf<TaskEntity?>(null) }
    LaunchedEffect(taskId) {
        task = viewModel.loadTask(taskId)
        if (task == null) onDismiss()
    }
    val t = task ?: return
    Dialog(onDismissRequest = onDismiss) {
        DialogCard {
            TaskEditorContent(
                task = t,
                onSave = { title, color -> viewModel.editTask(t.id, title, color) },
                onMoveTop = { viewModel.moveToTop(t.id); onDismiss() },
                onMove = { delta -> viewModel.moveBy(t.id, delta) },
                onReset = { viewModel.reset(t.id); onDismiss() },
                onDelete = { viewModel.deleteTask(t.id); onDismiss() },
                onDismiss = onDismiss,
            )
        }
    }
}

private fun requestPinWidget(context: Context, listId: Long) {
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) {
        Toast.makeText(context, R.string.add_widget_unsupported, Toast.LENGTH_LONG).show()
        return
    }
    val callback = PendingIntent.getBroadcast(
        context,
        listId.toInt(),
        Intent(context, PinWidgetReceiver::class.java).putExtra(WidgetIntents.EXTRA_LIST_ID, listId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
    manager.requestPinAppWidget(ComponentName(context, TaskListWidgetReceiver::class.java), null, callback)
}
