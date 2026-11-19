package app.neidliste.ui.components

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.neidliste.R

/**
 * Bottom input bar that rides on top of the keyboard, like a messenger.
 * Type one task, tap + and it closes again.
 */
@Composable
fun QuickAddSheet(listName: String, onAdd: (String) -> Unit, onClose: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current

    // Taps that arrive right while the sheet opens must not close it again.
    val openedAt = remember { SystemClock.uptimeMillis() }
    val closeFromOutside = { if (SystemClock.uptimeMillis() - openedAt > 400) onClose() }

    // One task per opening: add it and close.
    fun submit() {
        val title = text.trim()
        if (title.isEmpty()) return
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        onAdd(title)
        keyboard?.hide()
        onClose()
    }

    val noRipple = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            // no dimming: only the input bar itself is visible
            .clickable(interactionSource = noRipple, indication = null, onClick = closeFromOutside),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Appears instantly and rides on the keyboard; no extra animation of its own.
        Box(Modifier.fillMaxWidth()) {
            // The text field exists only now, so focus and keyboard are requested here.
            LaunchedEffect(Unit) {
                withFrameNanos { }
                focus.requestFocus()
                keyboard?.show()
            }
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            ) {
                Column(
                    Modifier
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
                    )
                    Text(
                        listName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 10.dp),
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            ) {
                                if (text.isEmpty()) {
                                    Text(
                                        stringResource(R.string.add_task_hint),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                BasicTextField(
                                    value = text,
                                    onValueChange = { text = it },
                                    maxLines = 5,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focus)
                                        .submitOnHardwareEnter { submit() },
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        FilledIconButton(
                            onClick = { submit() },
                            enabled = text.isNotBlank(),
                            modifier = Modifier.size(52.dp),
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_add),
                                contentDescription = stringResource(R.string.add),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}
