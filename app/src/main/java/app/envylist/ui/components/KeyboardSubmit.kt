package app.envylist.ui.components

import android.view.KeyCharacterMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Multi-line text fields: on a physical keyboard (PC/emulator, tablet keyboard) Enter submits and
 * Shift+Enter inserts a line break. The on-screen keyboard's Enter always inserts a line break;
 * there the + button submits.
 */
fun Modifier.submitOnHardwareEnter(onSubmit: () -> Unit): Modifier = onPreviewKeyEvent { event ->
    val hardware = event.nativeKeyEvent.deviceId != KeyCharacterMap.VIRTUAL_KEYBOARD
    val plainEnter = (event.key == Key.Enter || event.key == Key.NumPadEnter) && !event.isShiftPressed
    when {
        !hardware || !plainEnter -> false
        event.type == KeyEventType.KeyDown -> { onSubmit(); true }
        else -> true // swallow the matching KeyUp
    }
}
