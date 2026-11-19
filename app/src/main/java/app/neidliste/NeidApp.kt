package app.neidliste

import android.app.Application
import android.content.Context
import app.neidliste.data.AppDatabase
import app.neidliste.data.SettingsRepository
import app.neidliste.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database = AppDatabase.create(context)
    val settings = SettingsRepository(context)
    val repository = TaskRepository(context, database, settings, scope)
}

class NeidApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as NeidApp).container
