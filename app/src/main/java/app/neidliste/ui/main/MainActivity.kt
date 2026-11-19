package app.neidliste.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.neidliste.container
import app.neidliste.ui.archive.ArchiveScreen
import app.neidliste.ui.settings.SettingsScreen
import app.neidliste.ui.theme.NeidTheme
import app.neidliste.widget.WidgetIntents

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels { MainViewModel.factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            NeidTheme {
                AppRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val listId = intent?.getLongExtra(WidgetIntents.EXTRA_LIST_ID, -1L) ?: -1L
        if (listId > 0) viewModel.select(listId)
    }
}

private enum class Screen { Board, Archive, Settings }

@Composable
private fun AppRoot(viewModel: MainViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.Board) }
    BackHandler(enabled = screen != Screen.Board) { screen = Screen.Board }
    when (screen) {
        Screen.Board -> BoardScreen(
            viewModel = viewModel,
            onOpenArchive = { screen = Screen.Archive },
            onOpenSettings = { screen = Screen.Settings },
        )
        Screen.Archive -> ArchiveScreen(viewModel = viewModel, onBack = { screen = Screen.Board })
        Screen.Settings -> SettingsScreen(viewModel = viewModel, onBack = { screen = Screen.Board })
    }
}
