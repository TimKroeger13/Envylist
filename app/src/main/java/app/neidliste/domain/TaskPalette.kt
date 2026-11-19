package app.neidliste.domain

/** Bright, friendly colours for tasks and lists. Readable on light and dark backgrounds. */
object TaskPalette {
    val colors: List<Int> = listOf(
        0xFFEF5350.toInt(), // red
        0xFFFF7043.toInt(), // deep orange
        0xFFFFA726.toInt(), // orange
        0xFFFFCA28.toInt(), // amber
        0xFF9CCC65.toInt(), // light green
        0xFF26A69A.toInt(), // teal
        0xFF29B6F6.toInt(), // light blue
        0xFF5C6BC0.toInt(), // indigo
        0xFFAB47BC.toInt(), // purple
        0xFFEC407A.toInt(), // pink
        0xFF66BB6A.toInt(), // green
        0xFF8D6E63.toInt(), // brown
    )

    /** The least used palette colour, so a list stays as colourful as possible. */
    fun next(used: List<Int>): Int = colors.minBy { c -> used.count { it == c } }

    fun envyColor(level: EnvyLevel): Int = when (level) {
        EnvyLevel.CALM -> 0x00000000
        EnvyLevel.MILD -> 0xFFFFD54F.toInt()
        EnvyLevel.ANNOYED -> 0xFFFF9800.toInt()
        EnvyLevel.ANGRY -> 0xFFF44336.toInt()
        EnvyLevel.OBJECTION -> 0xFFB71C1C.toInt()
    }

    /** Row colour for the urgency level, `null` = calm (neutral base colour). */
    fun rowTint(level: EnvyLevel): Int? = when (level) {
        EnvyLevel.CALM -> null
        EnvyLevel.MILD -> 0xFFFFD54F.toInt()
        EnvyLevel.ANNOYED -> 0xFFFF9800.toInt()
        EnvyLevel.ANGRY -> 0xFFF44336.toInt()
        EnvyLevel.OBJECTION -> 0xFFD32F2F.toInt()
    }

    fun onEnvyColor(level: EnvyLevel): Int = when (level) {
        EnvyLevel.MILD, EnvyLevel.ANNOYED -> 0xFF3E2723.toInt()
        else -> 0xFFFFFFFF.toInt()
    }

    fun envyEmoji(level: EnvyLevel): String = when (level) {
        EnvyLevel.CALM -> ""
        EnvyLevel.MILD -> "👀"
        EnvyLevel.ANNOYED -> "😒"
        EnvyLevel.ANGRY -> "😤"
        EnvyLevel.OBJECTION -> "🛑"
    }
}
