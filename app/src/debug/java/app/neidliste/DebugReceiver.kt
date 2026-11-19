package app.neidliste

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch

/**
 * Test hook for debug builds (adb input cannot type umlauts):
 *
 *   adb shell am broadcast -n app.neidliste/.DebugReceiver -a add --es title "Wäsche waschen"
 *   adb shell am broadcast -n app.neidliste/.DebugReceiver -a envy --ei l1 1 --ei l2 2 --ei l3 3 --ei obj 4
 */
class DebugReceiver : BroadcastReceiver() {
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
