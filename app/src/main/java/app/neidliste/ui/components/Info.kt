package app.neidliste.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neidliste.R
import app.neidliste.domain.EnvyLevel
import app.neidliste.domain.EnvySettings
import app.neidliste.domain.TaskItem
import app.neidliste.domain.TaskPalette
import app.neidliste.ui.theme.DoneGreen
import app.neidliste.ui.theme.ProgressYellow

fun levelName(level: EnvyLevel): Int = when (level) {
    EnvyLevel.CALM, EnvyLevel.MILD -> R.string.level_mild
    EnvyLevel.ANNOYED -> R.string.level_annoyed
    EnvyLevel.ANGRY -> R.string.level_angry
    EnvyLevel.OBJECTION -> R.string.level_objection
}

/** Explains a task's envy badge: why it is jealous and what comes next. */
@Composable
fun EnvyInfoContent(task: TaskItem, settings: EnvySettings, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(TaskPalette.envyColor(task.level))),
            contentAlignment = Alignment.Center,
        ) { Text(TaskPalette.envyEmoji(task.level), fontSize = 26.sp) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(stringResource(levelName(task.level)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.envy_points, task.envy),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Text(stringResource(R.string.envy_info_watched, task.title, task.envy), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.envy_info_rule),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(14.dp))
    EnvyLadder(settings, current = task.level)
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(R.string.envy_info_objection, settings.objection),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.got_it)) }
}

/** The four envy levels with their thresholds; the current one is highlighted. */
@Composable
fun EnvyLadder(settings: EnvySettings, current: EnvyLevel? = null) {
    val steps = listOf(
        EnvyLevel.MILD to settings.level1,
        EnvyLevel.ANNOYED to settings.level2,
        EnvyLevel.ANGRY to settings.level3,
        EnvyLevel.OBJECTION to settings.objection,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEach { (level, from) ->
            val active = level == current
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (active) Color(TaskPalette.envyColor(level)).copy(alpha = 0.25f) else Color.Transparent,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(TaskPalette.envyEmoji(level), fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(levelName(level)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.envy_from, from),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Legend of every symbol in the list and the widget. */
@Composable
fun SymbolHelpContent(settings: EnvySettings, onClose: () -> Unit) {
    Text(stringResource(R.string.help), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(12.dp))
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
    ) {
        HelpRow(R.drawable.ic_status_open, MaterialTheme.colorScheme.onSurfaceVariant, R.string.help_open)
        HelpRow(R.drawable.ic_status_progress, ProgressYellow, R.string.help_progress)
        HelpRow(R.drawable.ic_status_done, DoneGreen, R.string.help_done)
        HelpRow(R.drawable.ic_reset, MaterialTheme.colorScheme.onSurfaceVariant, R.string.help_reset)
        HelpRow(R.drawable.ic_checkbox_empty, MaterialTheme.colorScheme.onSurface, R.string.help_close)
        HelpRow(R.drawable.ic_more, MaterialTheme.colorScheme.onSurfaceVariant, R.string.help_more)
        HelpRow(R.drawable.ic_drag, MaterialTheme.colorScheme.onSurfaceVariant, R.string.help_drag)
        HelpRow(R.drawable.ic_archive, MaterialTheme.colorScheme.onSurfaceVariant, R.string.help_archive)
        Text(stringResource(R.string.help_envy), style = MaterialTheme.typography.bodyMedium)
        EnvyLadder(settings)
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.got_it)) }
}

@Composable
private fun HelpRow(icon: Int, tint: Color, text: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
    }
}
