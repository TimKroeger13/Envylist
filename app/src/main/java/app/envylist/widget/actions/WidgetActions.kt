package app.envylist.widget.actions

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import app.envylist.container

object WidgetKeys {
    val TaskId = ActionParameters.Key<Long>("task_id")
    val ListId = ActionParameters.Key<Long>("list_id")
}

/** Tap on a task: open → in progress → done. Objections were already handled when drawing. */
class CycleStatusAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[WidgetKeys.TaskId] ?: return
        context.container.repository.cycle(id, force = true)
    }
}

class ResetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[WidgetKeys.TaskId] ?: return
        context.container.repository.reset(id)
    }
}

class ToggleCloseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[WidgetKeys.TaskId] ?: return
        context.container.repository.toggleClose(id)
    }
}

class CloseAllDoneAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[WidgetKeys.ListId] ?: return
        context.container.repository.toggleCloseAllDone(id)
    }
}
