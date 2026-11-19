package app.neidliste.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.neidliste.AppContainer
import app.neidliste.data.Board
import app.neidliste.data.CycleResult
import app.neidliste.data.SettingsRepository
import app.neidliste.data.TaskEntity
import app.neidliste.data.TaskListEntity
import app.neidliste.data.TaskRepository
import app.neidliste.domain.EnvySettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repo: TaskRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val started = SharingStarted.WhileSubscribed(5_000)

    val lists: StateFlow<List<TaskListEntity>> = repo.lists.stateIn(viewModelScope, started, emptyList())

    private val _selected = MutableStateFlow<Long?>(null)
    val selected: StateFlow<Long?> = _selected.asStateFlow()

    val board: StateFlow<Board?> = _selected.filterNotNull()
        .flatMapLatest { repo.observeBoard(it) }
        .stateIn(viewModelScope, started, null)

    val archive: StateFlow<List<TaskEntity>> = _selected.filterNotNull()
        .flatMapLatest { repo.observeArchive(it) }
        .stateIn(viewModelScope, started, emptyList())

    val settings: StateFlow<EnvySettings> = settingsRepo.envy.stateIn(viewModelScope, started, EnvySettings())

    private val _objection = MutableStateFlow<CycleResult.Objection?>(null)
    val objection: StateFlow<CycleResult.Objection?> = _objection.asStateFlow()

    /** A task that just shoved itself to the top. */
    val kicks = repo.kicks

    init {
        viewModelScope.launch {
            val id = repo.ensureDefaultList()
            if (_selected.value == null) _selected.value = id
        }
        // keep the selection valid when lists are deleted elsewhere
        viewModelScope.launch {
            repo.lists.collect { all ->
                val current = _selected.value
                if (all.isNotEmpty() && current != null && all.none { it.id == current }) {
                    _selected.value = all.first().id
                }
            }
        }
    }

    fun select(listId: Long) {
        _selected.value = listId
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    // --- tasks ---

    fun addTask(title: String) = launch { _selected.value?.let { repo.addTask(it, title) } }

    fun cycle(taskId: Long) = launch {
        val result = repo.cycle(taskId)
        if (result is CycleResult.Objection) _objection.value = result
    }

    /** "Fine, you first": the jealous task moves to the top, nothing gets finished. */
    fun yieldToObjector() = launch {
        val o = _objection.value ?: return@launch
        _objection.value = null
        repo.moveToTop(o.objector.id)
    }

    /** "Sorry, doing it anyway". */
    fun proceedAnyway() = launch {
        val o = _objection.value ?: return@launch
        _objection.value = null
        repo.cycle(o.task.id, force = true)
    }

    fun dismissObjection() {
        _objection.value = null
    }

    fun reset(taskId: Long) = launch { repo.reset(taskId) }
    fun toggleClose(taskId: Long) = launch { repo.toggleClose(taskId) }
    fun closeAllDone() = launch { _selected.value?.let { repo.toggleCloseAllDone(it) } }
    fun sortByEnvy() = launch { _selected.value?.let { repo.sortByEnvy(it) } }
    fun reorder(ids: List<Long>) = launch { _selected.value?.let { repo.reorder(it, ids) } }
    fun moveToTop(taskId: Long) = launch { repo.moveToTop(taskId) }
    fun moveBy(taskId: Long, delta: Int) = launch { repo.moveBy(taskId, delta) }
    fun editTask(taskId: Long, title: String, color: Int) = launch { repo.editTask(taskId, title, color) }
    fun deleteTask(taskId: Long) = launch { repo.deleteTask(taskId) }
    suspend fun loadTask(taskId: Long): TaskEntity? = repo.task(taskId)

    // --- lists ---

    fun createList(name: String) = launch {
        if (name.isNotBlank()) _selected.value = repo.createList(name)
    }

    fun renameList(name: String) = launch {
        val id = _selected.value ?: return@launch
        if (name.isNotBlank()) repo.renameList(id, name)
    }

    fun deleteList() = launch {
        val id = _selected.value ?: return@launch
        _selected.value = repo.deleteList(id)
    }

    // --- archive ---

    fun restore(taskId: Long) = launch { repo.restore(taskId) }
    fun clearArchive() = launch { _selected.value?.let { repo.clearArchive(it) } }

    // --- settings ---

    fun updateSettings(transform: (EnvySettings) -> EnvySettings) = launch { settingsRepo.update(transform) }

    val widgetBottomInset: StateFlow<Int> = settingsRepo.widgetBottomInset.stateIn(viewModelScope, started, 0)

    fun setWidgetBottomInset(dp: Int) = launch {
        settingsRepo.setWidgetBottomInset(dp)
        repo.refreshWidgets()
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { MainViewModel(container.repository, container.settings) }
        }
    }
}
