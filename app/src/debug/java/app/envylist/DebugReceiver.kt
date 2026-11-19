package app.envylist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch

/**
 * Test hook for debug builds (adb input cannot type umlauts):
 *
 *   adb shell am broadcast -n app.envylist/.DebugReceiver -a add --es title "Wäsche waschen"
 *   adb shell am broadcast -n app.envylist/.DebugReceiver -a envy --ei l1 1 --ei l2 2 --ei l3 3 --ei obj 4
 *   adb shell am broadcast -n app.envylist.debug/app.envylist.DebugReceiver -a demo   (screenshot data)
 */
class DebugReceiver : BroadcastReceiver() {

    /** A tidy example list showing every envy level, for README screenshots. Expects a freshly cleared app. */
    private suspend fun demo(context: Context) {
        val r = context.container.repository
        val de = context.resources.configuration.locales[0].language == "de"
        fun t(german: String, english: String) = if (de) german else english
        val listId = r.ensureDefaultList()
        r.renameList(listId, t("Haushalt", "Household"))
        context.container.settings.update { it.copy(level1 = 1, level2 = 3, level3 = 5, objection = 7) }
        // finished-and-gone chores: their completions make the others jealous
        suspend fun others(n: Int) = repeat(n) {
            kotlinx.coroutines.delay(5)
            val id = r.addTask(listId, "x")!!
            r.cycle(id, force = true)
            r.cycle(id, force = true)
            r.deleteTask(id)
        }
        r.addTask(listId, t("Steuererklärung", "Tax return"))
        others(3)
        r.addTask(listId, t("Staubsaugen", "Vacuum"))
        others(2)
        r.addTask(listId, t("Fenster putzen", "Clean the windows"))
        val bath = r.addTask(listId, t("Bad putzen", "Clean the bathroom"))!!
        r.addTask(listId, t("Einkaufen:\nMilch, Brot, Kaffee", "Groceries:\nmilk, bread, coffee"))
        val plants = r.addTask(listId, t("Pflanzen gießen", "Water the plants"))!!
        kotlinx.coroutines.delay(5)
        r.cycle(bath, force = true)
        r.cycle(bath, force = true)
        others(1)
        r.cycle(plants, force = true)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val c = context.container
        val pending = goAsync()
        c.scope.launch {
            try {
                when (intent.action) {
                    "add" -> {
                        val listId = c.repository.ensureDefaultList()
                        c.repository.addTask(listId, intent.getStringExtra("title").orEmpty())
                    }
                    "demo" -> demo(context)
                    "sizes" -> {
                        val mgr = android.appwidget.AppWidgetManager.getInstance(context)
                        val ids = mgr.getAppWidgetIds(
                            android.content.ComponentName(context, app.envylist.widget.TaskListWidgetReceiver::class.java),
                        )
                        ids.forEach { id ->
                            val o = mgr.getAppWidgetOptions(id)
                            android.util.Log.i(
                                "EnvylistSizes",
                                "id=$id minW=${o.getInt("appWidgetMinWidth")} maxW=${o.getInt("appWidgetMaxWidth")} " +
                                    "minH=${o.getInt("appWidgetMinHeight")} maxH=${o.getInt("appWidgetMaxHeight")} " +
                                    "sizes=${o.get("appWidgetSizes")}",
                            )
                        }
                    }
                    "envy" -> c.settings.update {
                        it.copy(
                            level1 = intent.getIntExtra("l1", it.level1),
                            level2 = intent.getIntExtra("l2", it.level2),
                            level3 = intent.getIntExtra("l3", it.level3),
                            objection = intent.getIntExtra("obj", it.objection),
                        )
                    }
                }
                c.repository.refreshWidgets()
            } finally {
                pending.finish()
            }
        }
    }
}
