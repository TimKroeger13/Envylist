package app.envylist.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

private val EnvyGreen = Color(0xFF2E9E5B)

@Composable
fun EnvyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFF7BD89A), secondary = EnvyGreen)
        else -> lightColorScheme(primary = EnvyGreen, secondary = Color(0xFF26A69A))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Readable text colour on top of an arbitrary task colour. */
fun contentColorOn(argb: Int): Color = if (Color(argb).luminance() > 0.5f) Color(0xFF1C1B1F) else Color.White

val GlowGreen = Color(0xFF43C463)
val DoneGreen = Color(0xFF2E9E5B)
val ProgressYellow = Color(0xFFFFB300)
