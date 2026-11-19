package app.neidliste.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import app.neidliste.ui.main.MainActivity

/** Explicit intents for everything the widget opens. A unique data Uri keeps PendingIntents apart. */
object WidgetIntents {
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_LIST_ID = "list_id"
    const val EXTRA_OBJECTOR_ID = "objector_id"

    private const val FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

    fun openApp(context: Context, listId: Long) = Intent(context, MainActivity::class.java)
        .setData("neidliste://list/$listId".toUri())
        .putExtra(EXTRA_LIST_ID, listId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun quickAdd(context: Context, listId: Long) = Intent(context, QuickAddActivity::class.java)
        .setData("neidliste://add/$listId".toUri())
        .putExtra(EXTRA_LIST_ID, listId)
        .addFlags(FLAGS)

    fun taskActions(context: Context, taskId: Long) = Intent(context, TaskActionsActivity::class.java)
        .setData("neidliste://task/$taskId".toUri())
        .putExtra(EXTRA_TASK_ID, taskId)
        .addFlags(FLAGS)

    fun envyPrompt(context: Context, taskId: Long, objectorId: Long) = Intent(context, EnvyPromptActivity::class.java)
        .setData("neidliste://objection/$taskId/$objectorId".toUri())
        .putExtra(EXTRA_TASK_ID, taskId)
        .putExtra(EXTRA_OBJECTOR_ID, objectorId)
        .addFlags(FLAGS)

    fun configure(context: Context, appWidgetId: Int) = Intent(context, WidgetConfigActivity::class.java)
        .setData("neidliste://configure/$appWidgetId".toUri())
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        .addFlags(FLAGS)
}
