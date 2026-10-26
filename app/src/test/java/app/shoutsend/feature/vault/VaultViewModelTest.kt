package app.shoutsend.feature.vault

import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.core.data.vault.repository.VaultRepository
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.WifiSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeVault: FakeVaultRepository
    private lateinit var viewModel: VaultViewModel

    private val item1 = VaultItem(
        id = 1L,
        timestamp = 1000L,
        direction = Direction.SENT,
        payload = Payload.Text("Hello World"),
        isUnreadable = false,
        isSecure = false,
        isStarred = true,
        profileSummary = "Retro Normal",
        durationMs = 4500L,
        snrDb = null
    )

    private val item2 = VaultItem(
        id = 2L,
        timestamp = 2000L,
        direction = Direction.RECEIVED,
        payload = Payload.Url("https://shoutsend.app"),
        isUnreadable = false,
        isSecure = false,
        isStarred = false,
        profileSummary = "Stealth Fast",
        durationMs = 3200L,
        snrDb = 24.5f
    )

    private val item3 = VaultItem(
        id = 3L,
        timestamp = 3000L,
        direction = Direction.SENT,
        payload = Payload.WiFi("OfficeWiFi", "SecretKey123", WifiSecurity.WPA2),
        isUnreadable = false,
        isSecure = false,
        isStarred = false,
        profileSummary = "Retro Normal",
        durationMs = 5000L,
        snrDb = null
    )

    private val item4 = VaultItem(
        id = 4L,
        timestamp = 4000L,
        direction = Direction.RECEIVED,
        payload = Payload.Contact("Alice Smith", listOf("+15551234567"), "alice@example.com"),
        isUnreadable = false,
        isSecure = true,
        isStarred = true,
        profileSummary = "Stealth Robust",
        durationMs = 6000L,
        snrDb = 18.2f
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeVault = FakeVaultRepository(listOf(item4, item3, item2, item1))
        viewModel = VaultViewModel(fakeVault)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateObservesAllItemsAndCounts() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(4, state.totalCount)
        assertEquals(2, state.starredCount)
        assertEquals(4, state.filteredItems.size)
        assertEquals(VaultFilterDirection.ALL, state.directionFilter)
        assertEquals(VaultFilterType.ALL, state.typeFilter)
    }

    @Test
    fun testDirectionFilterStarred() = runTest {
        advanceUntilIdle()
        viewModel.setDirectionFilter(VaultFilterDirection.STARRED)
        val state = viewModel.uiState.value
        assertEquals(2, state.filteredItems.size)
        assertTrue(state.filteredItems.all { it.isStarred })
        assertEquals(listOf(4L, 1L), state.filteredItems.map { it.id })
    }

    @Test
    fun testDirectionFilterSentAndReceived() = runTest {
        advanceUntilIdle()
        viewModel.setDirectionFilter(VaultFilterDirection.SENT)
        assertEquals(listOf(3L, 1L), viewModel.uiState.value.filteredItems.map { it.id })

        viewModel.setDirectionFilter(VaultFilterDirection.RECEIVED)
        assertEquals(listOf(4L, 2L), viewModel.uiState.value.filteredItems.map { it.id })
    }

    @Test
    fun testTypeFilterWiFi() = runTest {
        advanceUntilIdle()
        viewModel.setTypeFilter(VaultFilterType.WIFI)
        val filtered = viewModel.uiState.value.filteredItems
        assertEquals(1, filtered.size)
        assertEquals(3L, filtered[0].id)
        assertTrue(filtered[0].payload is Payload.WiFi)
    }

    @Test
    fun testTypeFilterContact() = runTest {
        advanceUntilIdle()
        viewModel.setTypeFilter(VaultFilterType.CONTACT)
        val filtered = viewModel.uiState.value.filteredItems
        assertEquals(1, filtered.size)
        assertEquals(4L, filtered[0].id)
        assertTrue(filtered[0].payload is Payload.Contact)
    }

    @Test
    fun testCombinedDirectionAndTypeFilter() = runTest {
        advanceUntilIdle()
        viewModel.setDirectionFilter(VaultFilterDirection.STARRED)
        viewModel.setTypeFilter(VaultFilterType.TEXT)
        val filtered = viewModel.uiState.value.filteredItems
        assertEquals(1, filtered.size)
        assertEquals(1L, filtered[0].id)
        assertEquals("Hello World", (filtered[0].payload as Payload.Text).value)
    }

    @Test
    fun testToggleStarUpdatesRepository() = runTest {
        advanceUntilIdle()
        assertFalse(item2.isStarred)

        viewModel.toggleStar(item2)
        advanceUntilIdle()

        val updated = viewModel.uiState.value.items.find { it.id == 2L }
        assertNotNull(updated)
        assertTrue(updated!!.isStarred)
        assertEquals(3, viewModel.uiState.value.starredCount)
    }

    @Test
    fun testDeleteItemAndUndoRestoresItem() = runTest {
        advanceUntilIdle()
        assertEquals(4, viewModel.uiState.value.totalCount)

        viewModel.deleteItem(item3)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.totalCount)
        assertEquals(item3, viewModel.uiState.value.lastDeletedItem)
        assertEquals("Transmission deleted", viewModel.uiState.value.snackbarMessage)

        viewModel.undoDelete()
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.totalCount)
        assertNull(viewModel.uiState.value.lastDeletedItem)
        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun testClearAllDialogLifecycleAndConfirm() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showClearAllDialog)

        viewModel.requestClearAll()
        assertTrue(viewModel.uiState.value.showClearAllDialog)

        viewModel.dismissClearAllDialog()
        assertFalse(viewModel.uiState.value.showClearAllDialog)

        viewModel.requestClearAll()
        viewModel.confirmClearAll()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showClearAllDialog)
        assertEquals(0, viewModel.uiState.value.totalCount)
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun testObserveItemByIdEmitsMatchingItemOrNull() = runTest {
        advanceUntilIdle()
        val found = viewModel.observeItemById(2L).first()
        assertNotNull(found)
        assertEquals(2L, found?.id)
        assertTrue(found?.payload is Payload.Url)

        val notFound = viewModel.observeItemById(999L).first()
        assertNull(notFound)
    }

    private class FakeVaultRepository(initialItems: List<VaultItem> = emptyList()) : VaultRepository {
        private val items = initialItems.toMutableList()
        private val _flow = MutableStateFlow<List<VaultItem>>(items.toList())

        override fun observeItems(): Flow<List<VaultItem>> = _flow.asStateFlow()

        override suspend fun save(
            direction: Direction,
            payload: Payload,
            profileSummary: String,
            durationMs: Long,
            snrDb: Float?,
            isSecure: Boolean
        ): Long {
            val nextId = (items.maxOfOrNull { it.id } ?: 0L) + 1L
            val item = VaultItem(
                id = nextId,
                timestamp = System.currentTimeMillis(),
                direction = direction,
                payload = payload,
                isUnreadable = false,
                isSecure = isSecure,
                isStarred = false,
                profileSummary = profileSummary,
                durationMs = durationMs,
                snrDb = snrDb
            )
            items.add(0, item)
            _flow.value = items.toList()
            return nextId
        }

        override suspend fun delete(id: Long) {
            items.removeAll { it.id == id }
            _flow.value = items.toList()
        }

        override suspend fun deleteAll() {
            items.clear()
            _flow.value = emptyList()
        }

        override suspend fun setStarred(id: Long, isStarred: Boolean) {
            val idx = items.indexOfFirst { it.id == id }
            if (idx >= 0) {
                items[idx] = items[idx].copy(isStarred = isStarred)
                _flow.value = items.toList()
            }
        }

        override suspend fun prune(maxEntries: Int) {
            // Unused
        }
    }
}
