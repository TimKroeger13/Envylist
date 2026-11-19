package app.neidliste.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.neidliste.domain.EnvySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val enabled = booleanPreferencesKey("envy_enabled")
        val level1 = intPreferencesKey("envy_level1")
        val level2 = intPreferencesKey("envy_level2")
        val level3 = intPreferencesKey("envy_level3")
        val objection = intPreferencesKey("envy_objection")
    }

    val envy: Flow<EnvySettings> = context.settingsStore.data.map { it.toSettings() }

    suspend fun current(): EnvySettings = envy.first()

    suspend fun update(transform: (EnvySettings) -> EnvySettings) {
        context.settingsStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[Keys.enabled] = s.enabled
            prefs[Keys.level1] = s.level1
            prefs[Keys.level2] = s.level2
            prefs[Keys.level3] = s.level3
            prefs[Keys.objection] = s.objection
        }
    }

    private fun Preferences.toSettings(): EnvySettings {
        val d = EnvySettings()
        return EnvySettings(
            enabled = this[Keys.enabled] ?: d.enabled,
            level1 = this[Keys.level1] ?: d.level1,
            level2 = this[Keys.level2] ?: d.level2,
            level3 = this[Keys.level3] ?: d.level3,
            objection = this[Keys.objection] ?: d.objection,
        )
    }
}
