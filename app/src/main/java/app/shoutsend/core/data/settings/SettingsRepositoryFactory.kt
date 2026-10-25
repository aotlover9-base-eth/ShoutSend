package app.shoutsend.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "shoutsend_settings")

fun createSettingsRepository(
    context: Context,
    scope: CoroutineScope
): SettingsRepository {
    return DataStoreSettingsRepository(
        dataStore = context.settingsDataStore,
        scope = scope
    )
}
