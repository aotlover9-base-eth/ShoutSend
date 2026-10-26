package app.shoutsend.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.core.data.vault.repository.VaultRepository
import app.shoutsend.modem.payload.Payload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Filter categories for transmission direction.
 */
enum class VaultFilterDirection {
    ALL,
    STARRED,
    SENT,
    RECEIVED
}

/**
 * Filter categories for payload types.
 */
enum class VaultFilterType {
    ALL,
    TEXT,
    URL,
    WIFI,
    CONTACT
}

/**
 * Immutable UI state for the Transmission Vault screen.
 */
data class VaultUiState(
    val items: List<VaultItem> = emptyList(),
    val filteredItems: List<VaultItem> = emptyList(),
    val directionFilter: VaultFilterDirection = VaultFilterDirection.ALL,
    val typeFilter: VaultFilterType = VaultFilterType.ALL,
    val totalCount: Int = 0,
    val starredCount: Int = 0,
    val lastDeletedItem: VaultItem? = null,
    val showClearAllDialog: Boolean = false,
    val snackbarMessage: String? = null
)

/**
 * ViewModel managing encrypted message history, reactive filtering, deletion, and re-transmission.
 *
 * Rules:
 * - Sensitive credentials (passwords, contacts) are never logged (Rule 25).
 * - Real deletion from encrypted SQLCipher database via [VaultRepository].
 * - Supports one-step Undo for accidental swipe-to-delete.
 */
class VaultViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            vaultRepository.observeItems().collect { allItems ->
                _uiState.update { current ->
                    val filtered = applyFilters(allItems, current.directionFilter, current.typeFilter)
                    current.copy(
                        items = allItems,
                        filteredItems = filtered,
                        totalCount = allItems.size,
                        starredCount = allItems.count { it.isStarred }
                    )
                }
            }
        }
    }

    /**
     * Changes the direction filter (All, Starred, Sent, Received).
     */
    fun setDirectionFilter(filter: VaultFilterDirection) {
        _uiState.update { current ->
            current.copy(
                directionFilter = filter,
                filteredItems = applyFilters(current.items, filter, current.typeFilter)
            )
        }
    }

    /**
     * Changes the payload type filter (All, Text, URL, Wi-Fi, Contact).
     */
    fun setTypeFilter(filter: VaultFilterType) {
        _uiState.update { current ->
            current.copy(
                typeFilter = filter,
                filteredItems = applyFilters(current.items, current.directionFilter, filter)
            )
        }
    }

    /**
     * Toggles the starred status of an item.
     */
    fun toggleStar(item: VaultItem) {
        viewModelScope.launch {
            vaultRepository.setStarred(item.id, !item.isStarred)
        }
    }

    /**
     * Deletes a vault entry, storing it in state to allow one-step Undo.
     */
    fun deleteItem(item: VaultItem) {
        viewModelScope.launch {
            vaultRepository.delete(item.id)
            _uiState.update { current ->
                current.copy(
                    lastDeletedItem = item,
                    snackbarMessage = "Transmission deleted"
                )
            }
        }
    }

    /**
     * Undoes the last deletion, re-saving the item into encrypted storage.
     */
    fun undoDelete() {
        val itemToRestore = _uiState.value.lastDeletedItem ?: return
        val payload = itemToRestore.payload ?: return
        viewModelScope.launch {
            val newId = vaultRepository.save(
                direction = itemToRestore.direction,
                payload = payload,
                profileSummary = itemToRestore.profileSummary,
                durationMs = itemToRestore.durationMs,
                snrDb = itemToRestore.snrDb,
                isSecure = itemToRestore.isSecure
            )
            if (itemToRestore.isStarred) {
                vaultRepository.setStarred(newId, true)
            }
            _uiState.update { it.copy(lastDeletedItem = null, snackbarMessage = null) }
        }
    }

    /**
     * Clears the current snackbar message.
     */
    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    /**
     * Prompts the Clear All confirmation dialog.
     */
    fun requestClearAll() {
        _uiState.update { it.copy(showClearAllDialog = true) }
    }

    /**
     * Dismisses the Clear All confirmation dialog.
     */
    fun dismissClearAllDialog() {
        _uiState.update { it.copy(showClearAllDialog = false) }
    }

    /**
     * Permanently purges all items from the encrypted vault.
     */
    fun confirmClearAll() {
        viewModelScope.launch {
            vaultRepository.deleteAll()
            _uiState.update { it.copy(showClearAllDialog = false, lastDeletedItem = null) }
        }
    }

    /**
     * Observes a single item by its ID.
     */
    fun observeItemById(id: Long): Flow<VaultItem?> {
        return vaultRepository.observeItems().map { list ->
            list.find { it.id == id }
        }
    }

    private fun applyFilters(
        items: List<VaultItem>,
        direction: VaultFilterDirection,
        type: VaultFilterType
    ): List<VaultItem> {
        return items.filter { item ->
            val matchesDirection = when (direction) {
                VaultFilterDirection.ALL -> true
                VaultFilterDirection.STARRED -> item.isStarred
                VaultFilterDirection.SENT -> item.direction == Direction.SENT
                VaultFilterDirection.RECEIVED -> item.direction == Direction.RECEIVED
            }

            val matchesType = when (type) {
                VaultFilterType.ALL -> true
                VaultFilterType.TEXT -> item.payload is Payload.Text
                VaultFilterType.URL -> item.payload is Payload.Url
                VaultFilterType.WIFI -> item.payload is Payload.WiFi
                VaultFilterType.CONTACT -> item.payload is Payload.Contact
            }

            matchesDirection && matchesType
        }
    }
}
