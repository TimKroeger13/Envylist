package app.neidliste.domain

import android.content.Context
import app.neidliste.R
import kotlin.math.absoluteValue
import kotlin.random.Random

/** The tasks speak for themselves. Lines rotate as envy grows, but stay stable between redraws. */
object EnvyVoice {

    fun line(context: Context, item: TaskItem): String? {
        val res = when (item.level) {
            EnvyLevel.CALM -> return null
            EnvyLevel.MILD -> R.array.envy_mild
            EnvyLevel.ANNOYED -> R.array.envy_annoyed
            EnvyLevel.ANGRY -> R.array.envy_angry
            EnvyLevel.OBJECTION -> R.array.envy_objection_short
        }
        return pick(context, res, item.id * 31 + item.envy)
    }

    fun objection(context: Context, objector: TaskItem, other: TaskItem): String =
        pick(context, R.array.envy_objection_long, objector.envy * 7L + other.id * 13L)
            .format(objector.envy, other.title)

    fun kick(context: Context, item: TaskItem): String = pick(context, R.array.envy_kick, item.id + item.envy)

    fun thanks(context: Context): String = pick(context, R.array.envy_thanks, Random.nextLong())

    fun huff(context: Context): String = pick(context, R.array.envy_huff, Random.nextLong())

    private fun pick(context: Context, res: Int, seed: Long): String {
        val lines = context.resources.getStringArray(res)
        return lines[(seed % lines.size).absoluteValue.toInt()]
    }
}
