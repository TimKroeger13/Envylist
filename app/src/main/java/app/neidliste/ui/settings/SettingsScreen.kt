@file:OptIn(ExperimentalMaterial3Api::class)

package app.neidliste.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.neidliste.R
import app.neidliste.domain.EnvyLevel
import app.neidliste.domain.TaskPalette
import app.neidliste.ui.main.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val s by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.envy_enabled), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.envy_enabled_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = s.enabled, onCheckedChange = { on -> viewModel.updateSettings { it.copy(enabled = on) } })
            }
            Spacer(Modifier.height(16.dp))

            Stepper(R.string.envy_level1, EnvyLevel.MILD, s.level1, 1, s.level2 - 1, s.enabled) { v ->
                viewModel.updateSettings { it.copy(level1 = v) }
            }
            Stepper(R.string.envy_level2, EnvyLevel.ANNOYED, s.level2, s.level1 + 1, s.level3 - 1, s.enabled) { v ->
                viewModel.updateSettings { it.copy(level2 = v) }
            }
            Stepper(R.string.envy_level3, EnvyLevel.ANGRY, s.level3, s.level2 + 1, s.objection - 1, s.enabled) { v ->
                viewModel.updateSettings { it.copy(level3 = v) }
            }
            Stepper(R.string.envy_objection, EnvyLevel.OBJECTION, s.objection, s.level3 + 1, 99, s.enabled) { v ->
                viewModel.updateSettings { it.copy(objection = v) }
            }

            Spacer(Modifier.height(20.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    stringResource(R.string.envy_explainer),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun Stepper(label: Int, level: EnvyLevel, value: Int, min: Int, max: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(TaskPalette.envyEmoji(level))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onChange(value - 1) }, enabled = enabled && value > min) { Text("−") }
        Text(
            value.toString(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color(TaskPalette.envyColor(level)) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 40.dp),
        )
        FilledTonalIconButton(onClick = { onChange(value + 1) }, enabled = enabled && value < max) { Text("+") }
    }
}
