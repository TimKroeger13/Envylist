package app.neidliste.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neidliste.R
import app.neidliste.data.TaskEntity
import app.neidliste.domain.EnvyVoice
import app.neidliste.domain.TaskItem
import app.neidliste.domain.TaskPalette
import app.neidliste.domain.TaskStatus
import app.neidliste.ui.theme.contentColorOn

/** Full-screen transparent host for the small dialogs the widget opens. Tap outside to close. */
@Composable
fun DialogScaffold(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        DialogCard(
            Modifier
                .padding(20.dp)
                // swallow taps on the card itself so they don't dismiss the dialog
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            content,
        )
    }
}

@Composable
fun DialogCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = modifier.widthIn(max = 460.dp),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

/** The jealous task speaks up before another one gets finished first. */
@Composable
fun ObjectionContent(objector: TaskItem, task: TaskItem, onYield: () -> Unit, onProceed: () -> Unit) {
    val context = LocalContext.current
    val message = remember(objector.id, task.id, objector.envy) { EnvyVoice.objection(context, objector, task) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(TaskPalette.envyColor(objector.level))),
            contentAlignment = Alignment.Center,
        ) {
            Text(TaskPalette.envyEmoji(objector.level), fontSize = 26.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(objector.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.envy_points, objector.envy),
                style = MaterialTheme.typography.labelLarge,
                color = Color(TaskPalette.envyColor(objector.level)),
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Surface(
        color = Color(TaskPalette.envyColor(objector.level)).copy(alpha = 0.18f),
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(14.dp))
    }
    Spacer(Modifier.height(18.dp))
    Button(
        onClick = onYield,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(TaskPalette.envyColor(objector.level)),
            contentColor = contentColorOn(TaskPalette.envyColor(objector.level)),
        ),
    ) { Text(stringResource(R.string.yield_button)) }
    TextButton(onClick = onProceed, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.proceed_button))
    }
}

/** Edit title and colour, move the task around, reset or delete it. */
@Composable
fun TaskEditorContent(
    task: TaskEntity,
    onSave: (title: String, color: Int) -> Unit,
    onMoveTop: () -> Unit,
    onMove: (delta: Int) -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(task.title) }
    var color by remember { mutableIntStateOf(task.color) }
    var confirmDelete by remember { mutableStateOf(false) }

    Text(stringResource(R.string.edit_task), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text(stringResource(R.string.task_title)) },
        maxLines = 5,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { onSave(title, color); onMoveTop() }, modifier = Modifier.weight(1f)) {
            Icon(painterResource(R.drawable.ic_arrow_top), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.move_top), maxLines = 1)
        }
        OutlinedButton(onClick = { onMove(-1) }) {
            Icon(painterResource(R.drawable.ic_arrow_up), contentDescription = stringResource(R.string.move_up), modifier = Modifier.size(18.dp))
        }
        OutlinedButton(onClick = { onMove(+1) }) {
            Icon(painterResource(R.drawable.ic_arrow_down), contentDescription = stringResource(R.string.move_down), modifier = Modifier.size(18.dp))
        }
    }
    if (task.status != TaskStatus.OPEN) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_reset), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.reset))
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = { if (confirmDelete) onDelete() else confirmDelete = true },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Text(stringResource(if (confirmDelete) R.string.confirm_delete_task else R.string.delete), maxLines = 2)
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        Button(onClick = { onSave(title, color); onDismiss() }) { Text(stringResource(R.string.save)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    Text(stringResource(R.string.color), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TaskPalette.colors.forEach { c ->
            val isSelected = c == selected
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .then(
                        if (isSelected) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                        else Modifier,
                    )
                    .clickable { onSelect(c) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        painterResource(R.drawable.ic_status_done),
                        contentDescription = null,
                        tint = contentColorOn(c),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
