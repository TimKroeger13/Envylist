package app.neidliste.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.neidliste.R
import app.neidliste.domain.EnvyLevel
import app.neidliste.domain.TaskItem
import app.neidliste.domain.TaskPalette
import app.neidliste.domain.TaskStatus
import app.neidliste.ui.theme.DoneGreen
import app.neidliste.ui.theme.GlowGreen
import app.neidliste.ui.theme.ProgressYellow

/** The same row as in the widget, with a bit of animation on top. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: TaskItem,
    speech: String?,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDot: () -> Unit,
    onReset: () -> Unit,
    onToggleClose: () -> Unit,
    dragHandle: Modifier,
    modifier: Modifier = Modifier,
) {
    val done = task.status == TaskStatus.DONE
    // Ticking the close-checkbox: one soft light-up into green, then a calm green until it leaves.
    val glow = remember { Animatable(0f) }
    LaunchedEffect(task.archivePending) {
        if (task.archivePending) {
            glow.animateTo(1f, tween(durationMillis = 180))
            glow.animateTo(0.55f, tween(durationMillis = 900))
        } else {
            glow.animateTo(0f, tween(durationMillis = 250))
        }
    }
    // Calm base colour; only urgency (envy) adds colour.
    val urgency = TaskPalette.rowTint(task.level)
    val base by animateColorAsState(
        targetValue = when {
            done -> MaterialTheme.colorScheme.surfaceContainerLow
            urgency != null -> lerp(MaterialTheme.colorScheme.surfaceContainerHigh, Color(urgency), 0.30f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(350),
        label = "rowColor",
    )
    val background = lerp(base, GlowGreen.copy(alpha = 0.55f), glow.value)
    val scale by animateFloatAsState(
        targetValue = if (task.archivePending) 1.015f else 1f,
        animationSpec = tween(220),
        label = "rowScale",
    )

    Surface(
        color = background,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 2.dp)) {
            Box(
                modifier = Modifier.size(width = 40.dp, height = 52.dp).clickable(onClick = onDot),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_more),
                    contentDescription = stringResource(R.string.cd_color),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                    .padding(vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusIcon(task)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = task.title,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (task.status == TaskStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal,
                            textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                        ),
                        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (speech != null) {
                    Text(
                        text = "„$speech“",
                        maxLines = 2,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 28.dp, top = 2.dp),
                    )
                }
            }
            if (task.level != EnvyLevel.CALM) EnvyBadge(task)
            if (done) {
                IconButton(onClick = onReset) {
                    Icon(
                        painterResource(R.drawable.ic_reset),
                        contentDescription = stringResource(R.string.cd_reset),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onToggleClose) {
                    Icon(
                        painterResource(if (task.archivePending) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_empty),
                        contentDescription = stringResource(R.string.cd_close_task),
                        tint = if (task.archivePending) DoneGreen else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Icon(
                painterResource(R.drawable.ic_drag),
                contentDescription = stringResource(R.string.cd_drag),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = dragHandle.padding(10.dp).size(20.dp),
            )
        }
    }
}

@Composable
fun StatusIcon(task: TaskItem, modifier: Modifier = Modifier) {
    val (icon, tint, cd) = when (task.status) {
        TaskStatus.OPEN -> Triple(R.drawable.ic_status_open, MaterialTheme.colorScheme.onSurfaceVariant, R.string.cd_status_open)
        TaskStatus.IN_PROGRESS -> Triple(R.drawable.ic_status_progress, ProgressYellow, R.string.cd_status_progress)
        TaskStatus.DONE -> Triple(R.drawable.ic_status_done, DoneGreen, R.string.cd_status_done)
    }
    Icon(painterResource(icon), contentDescription = stringResource(cd), tint = tint, modifier = modifier.size(20.dp))
}

@Composable
fun EnvyBadge(task: TaskItem) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(TaskPalette.envyColor(task.level)))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = "${TaskPalette.envyEmoji(task.level)} ${task.envy}",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(TaskPalette.onEnvyColor(task.level)),
        )
    }
}
