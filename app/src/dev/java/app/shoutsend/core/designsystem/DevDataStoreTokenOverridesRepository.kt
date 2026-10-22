package app.shoutsend.core.designsystem

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.tokenOverridesDataStore: DataStore<Preferences> by preferencesDataStore(name = "dev_token_overrides")

/**
 * Developer-only DataStore-backed repository storing sparse token overrides as JSON.
 * Writes are debounced by 300ms while in-memory state updates instantly.
 */
@OptIn(FlowPreview::class)
class DevDataStoreTokenOverridesRepository(
    private val context: Context,
    private val scope: CoroutineScope
) : TokenOverridesRepository {

    private val keyOverrides = stringPreferencesKey("token_overrides_json")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    private val _inMemoryOverrides = MutableStateFlow(TokenOverrides())
    override val overrides: Flow<TokenOverrides> = _inMemoryOverrides.asStateFlow()

    private val pendingWrites = MutableSharedFlow<TokenOverrides>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        scope.launch {
            val initial = readFromDataStore()
            _inMemoryOverrides.value = initial

            pendingWrites
                .debounce(300L)
                .collect { toPersist ->
                    persistToDataStore(toPersist)
                }
        }
    }

    override suspend fun update(transform: (TokenOverrides) -> TokenOverrides) {
        val updated = transform(_inMemoryOverrides.value)
        _inMemoryOverrides.value = updated
        pendingWrites.emit(updated)
    }

    override suspend fun resetAll() {
        _inMemoryOverrides.value = TokenOverrides()
        persistToDataStore(TokenOverrides())
    }

    override suspend fun resetColor(role: String) {
        update { current ->
            current.copy(colors = current.colors - role)
        }
    }

    override suspend fun resetRadius(name: String) {
        update { current ->
            current.copy(radii = current.radii - name)
        }
    }

    private suspend fun readFromDataStore(): TokenOverrides {
        return try {
            val prefs = context.tokenOverridesDataStore.data.first()
            val rawJson = prefs[keyOverrides] ?: return TokenOverrides()
            val parsed = json.decodeFromString<TokenOverrides>(rawJson)
            TokenOverrides.validate(parsed).getOrElse { error ->
                Log.w(TAG, "Corrupted token overrides JSON ignored: ${error.message}")
                TokenOverrides()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading dev token overrides: ${e.message}")
            TokenOverrides()
        }
    }

    private suspend fun persistToDataStore(data: TokenOverrides) {
        try {
            context.tokenOverridesDataStore.edit { prefs ->
                if (data.isEmpty()) {
                    prefs.remove(keyOverrides)
                } else {
                    prefs[keyOverrides] = json.encodeToString(data)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing dev token overrides: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "DevTokenOverridesRepo"
    }
}
