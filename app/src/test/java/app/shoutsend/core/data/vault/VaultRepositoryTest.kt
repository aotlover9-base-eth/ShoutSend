package app.shoutsend.core.data.vault

import app.shoutsend.core.data.vault.crypto.FakeKeystoreCipher
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.core.data.vault.repository.DefaultVaultRepository
import app.shoutsend.modem.packet.MessageType
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.WifiSecurity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultRepositoryTest {

    private lateinit var fakeDao: FakeVaultDao
    private lateinit var fakeCipher: FakeKeystoreCipher
    private var simulatedTime: Long = 1000L
    private val testDispatcher = StandardTestDispatcher()

    private fun createRepository(): DefaultVaultRepository {
        return DefaultVaultRepository(
            dao = fakeDao,
            cipher = fakeCipher,
            dispatcher = testDispatcher,
            clock = { simulatedTime }
        )
    }

    @Before
    fun setUp() {
        fakeDao = FakeVaultDao()
        fakeCipher = FakeKeystoreCipher()
        simulatedTime = 1000L
    }

    @Test
    fun saveAndObserveNewestFirstOrder() = runTest(testDispatcher) {
        val repo = createRepository()

        simulatedTime = 1000L
        val idA = repo.save(
            direction = Direction.SENT,
            payload = Payload.Text("Message A"),
            profileSummary = "STEALTH / MFSK16 / NORMAL",
            durationMs = 1200L,
            snrDb = null,
            isSecure = false
        )

        simulatedTime = 2000L
        val idB = repo.save(
            direction = Direction.RECEIVED,
            payload = Payload.Text("Message B"),
            profileSummary = "RETRO / MFSK16 / FAST",
            durationMs = 800L,
            snrDb = 18.5f,
            isSecure = true
        )

        simulatedTime = 1500L
        val idC = repo.save(
            direction = Direction.SENT,
            payload = Payload.Text("Message C"),
            profileSummary = "STEALTH / MFSK16 / NORMAL",
            durationMs = 1100L,
            snrDb = null,
            isSecure = false
        )

        val items = repo.observeItems().first()
        assertEquals(3, items.size)

        // Expected order: newest timestamp first -> B (2000), C (1500), A (1000)
        assertEquals(idB, items[0].id)
        assertEquals(2000L, items[0].timestamp)
        assertEquals(Direction.RECEIVED, items[0].direction)
        assertEquals(Payload.Text("Message B"), items[0].payload)
        assertTrue(items[0].isSecure)
        assertEquals(18.5f, items[0].snrDb)

        assertEquals(idC, items[1].id)
        assertEquals(1500L, items[1].timestamp)
        assertEquals(Payload.Text("Message C"), items[1].payload)

        assertEquals(idA, items[2].id)
        assertEquals(1000L, items[2].timestamp)
        assertEquals(Payload.Text("Message A"), items[2].payload)
    }

    @Test
    fun starTogglePersistsAndUpdates() = runTest(testDispatcher) {
        val repo = createRepository()

        val id = repo.save(
            direction = Direction.SENT,
            payload = Payload.Text("Star Me"),
            profileSummary = "STEALTH / MFSK16 / NORMAL",
            durationMs = 1000L,
            snrDb = null,
            isSecure = false
        )

        val initialItems = repo.observeItems().first()
        assertEquals(1, initialItems.size)
        assertFalse(initialItems[0].isStarred)

        repo.setStarred(id, true)
        val starredItems = repo.observeItems().first()
        assertTrue(starredItems[0].isStarred)

        repo.setStarred(id, false)
        val unstarredItems = repo.observeItems().first()
        assertFalse(unstarredItems[0].isStarred)
    }

    @Test
    fun pruneRemovesOldestUnstarredEntriesWhilePreservingStarredEntries() = runTest(testDispatcher) {
        val repo = createRepository()

        // 5 unstarred entries at timestamps 100, 200, 300, 400, 500
        val unstarredIds = mutableListOf<Long>()
        listOf(100L, 200L, 300L, 400L, 500L).forEach { ts ->
            simulatedTime = ts
            val id = repo.save(
                direction = Direction.SENT,
                payload = Payload.Text("Unstarred at $ts"),
                profileSummary = "STEALTH",
                durationMs = 500L,
                snrDb = null,
                isSecure = false
            )
            unstarredIds.add(id)
        }

        // 2 starred entries at timestamps 50 (oldest of all) and 250
        simulatedTime = 50L
        val starOldestId = repo.save(
            direction = Direction.RECEIVED,
            payload = Payload.Text("Starred oldest"),
            profileSummary = "STEALTH",
            durationMs = 500L,
            snrDb = null,
            isSecure = false
        )
        repo.setStarred(starOldestId, true)

        simulatedTime = 250L
        val starMidId = repo.save(
            direction = Direction.RECEIVED,
            payload = Payload.Text("Starred mid"),
            profileSummary = "STEALTH",
            durationMs = 500L,
            snrDb = null,
            isSecure = false
        )
        repo.setStarred(starMidId, true)

        assertEquals(7, repo.observeItems().first().size)

        // Prune keeping at most 3 unstarred entries
        repo.prune(maxEntries = 3)

        val remaining = repo.observeItems().first()
        // Kept unstarred: timestamps 500, 400, 300 (3 newest)
        // Dropped unstarred: timestamps 100, 200 (oldest)
        // Starred entries preserved: timestamps 50 and 250
        assertEquals(5, remaining.size)

        val remainingIds = remaining.map { it.id }.toSet()

        // Starred rows must NEVER be deleted
        assertTrue("Starred oldest must be preserved", starOldestId in remainingIds)
        assertTrue("Starred mid must be preserved", starMidId in remainingIds)

        // 3 newest unstarred must be kept
        assertTrue("Unstarred 500 must be kept", unstarredIds[4] in remainingIds)
        assertTrue("Unstarred 400 must be kept", unstarredIds[3] in remainingIds)
        assertTrue("Unstarred 300 must be kept", unstarredIds[2] in remainingIds)

        // 2 oldest unstarred must be deleted
        assertFalse("Unstarred 100 must be deleted", unstarredIds[0] in remainingIds)
        assertFalse("Unstarred 200 must be deleted", unstarredIds[1] in remainingIds)
    }

    @Test
    fun unreadableRowHandlingWhenDecryptionFails() = runTest(testDispatcher) {
        val repo = createRepository()

        val id = repo.save(
            direction = Direction.RECEIVED,
            payload = Payload.Text("Will fail decryption"),
            profileSummary = "STEALTH / MFSK16 / NORMAL",
            durationMs = 900L,
            snrDb = 12.0f,
            isSecure = true
        )

        // Simulate Keystore decryption failure (e.g. key invalidated, corrupted tag)
        fakeCipher.shouldFailDecryption = true

        val items = repo.observeItems().first()
        assertEquals(1, items.size)
        val item = items[0]

        assertEquals(id, item.id)
        assertTrue("Item must be marked unreadable on decryption failure", item.isUnreadable)
        assertNull("Payload must be null when unreadable", item.payload)
        // Metadata must remain intact
        assertEquals(Direction.RECEIVED, item.direction)
        assertTrue(item.isSecure)
        assertEquals(12.0f, item.snrDb)
        assertEquals("STEALTH / MFSK16 / NORMAL", item.profileSummary)
    }

    @Test
    fun sensitiveDataMaskingToStringContainsNoSecrets() = runTest(testDispatcher) {
        val secretText = "Classified-Mission-Directives-998"
        val secretSsid = "Area51-Secure-WLAN"
        val secretPassword = "SuperSecretPassword123!"
        val secretName = "Agent 007"
        val secretPhone = "+1-555-0199"
        val secretEmail = "classified@antigravity.gov"
        val secretUrl = "https://internal.bank.net/transfer?auth=TOKEN99"

        val textItem = VaultItem(
            id = 1,
            timestamp = 1000L,
            direction = Direction.SENT,
            payload = Payload.Text(secretText),
            isUnreadable = false,
            isSecure = false,
            isStarred = false,
            profileSummary = "STEALTH",
            durationMs = 500L,
            snrDb = null
        )

        val wifiItem = VaultItem(
            id = 2,
            timestamp = 2000L,
            direction = Direction.RECEIVED,
            payload = Payload.WiFi(
                ssid = secretSsid,
                password = secretPassword,
                security = WifiSecurity.WPA2,
                isHidden = true
            ),
            isUnreadable = false,
            isSecure = true,
            isStarred = true,
            profileSummary = "RETRO",
            durationMs = 800L,
            snrDb = 15.0f
        )

        val contactItem = VaultItem(
            id = 3,
            timestamp = 3000L,
            direction = Direction.SENT,
            payload = Payload.Contact(
                displayName = secretName,
                phones = listOf(secretPhone),
                email = secretEmail
            ),
            isUnreadable = false,
            isSecure = false,
            isStarred = false,
            profileSummary = "STEALTH",
            durationMs = 600L,
            snrDb = null
        )

        val urlItem = VaultItem(
            id = 4,
            timestamp = 4000L,
            direction = Direction.RECEIVED,
            payload = Payload.Url(secretUrl),
            isUnreadable = false,
            isSecure = false,
            isStarred = false,
            profileSummary = "STEALTH",
            durationMs = 700L,
            snrDb = null
        )

        val unreadableItem = VaultItem(
            id = 5,
            timestamp = 5000L,
            direction = Direction.SENT,
            payload = null,
            isUnreadable = true,
            isSecure = false,
            isStarred = false,
            profileSummary = "STEALTH",
            durationMs = 500L,
            snrDb = null
        )

        val allItems = listOf(textItem, wifiItem, contactItem, urlItem, unreadableItem)

        allItems.forEach { item ->
            val str = item.toString()
            assertFalse("toString() must NEVER reveal secret text: $str", str.contains(secretText))
            assertFalse("toString() must NEVER reveal Wi-Fi SSID: $str", str.contains(secretSsid))
            assertFalse("toString() must NEVER reveal Wi-Fi password: $str", str.contains(secretPassword))
            assertFalse("toString() must NEVER reveal contact name: $str", str.contains(secretName))
            assertFalse("toString() must NEVER reveal contact phone: $str", str.contains(secretPhone))
            assertFalse("toString() must NEVER reveal contact email: $str", str.contains(secretEmail))
            assertFalse("toString() must NEVER reveal URL secret path or tokens: $str", str.contains("TOKEN99"))
            assertFalse("toString() must NEVER reveal raw URL: $str", str.contains("transfer?auth"))
        }

        // Verify type summary is shown instead
        assertTrue(textItem.toString().contains("type=TEXT"))
        assertTrue(wifiItem.toString().contains("type=WIFI"))
        assertTrue(contactItem.toString().contains("type=CONTACT"))
        assertTrue(urlItem.toString().contains("type=URL"))
        assertTrue(unreadableItem.toString().contains("type=UNREADABLE"))
    }

    @Test
    fun deleteAndClearAll() = runTest(testDispatcher) {
        val repo = createRepository()

        val id1 = repo.save(
            direction = Direction.SENT,
            payload = Payload.Text("Msg 1"),
            profileSummary = "STEALTH",
            durationMs = 500L,
            snrDb = null,
            isSecure = false
        )
        val id2 = repo.save(
            direction = Direction.SENT,
            payload = Payload.Text("Msg 2"),
            profileSummary = "STEALTH",
            durationMs = 500L,
            snrDb = null,
            isSecure = false
        )

        assertEquals(2, repo.observeItems().first().size)

        repo.delete(id1)
        val afterDelete = repo.observeItems().first()
        assertEquals(1, afterDelete.size)
        assertEquals(id2, afterDelete[0].id)

        repo.deleteAll()
        assertTrue(repo.observeItems().first().isEmpty())
    }

    @Test
    fun allPayloadTypesRoundtripSuccessfully() = runTest(testDispatcher) {
        val repo = createRepository()

        val payloads = listOf(
            Payload.Text("Hello World!"),
            Payload.Url("https://shoutsend.app/download"),
            Payload.WiFi("HomeWifi", "SecretPass123", WifiSecurity.WPA2, false),
            Payload.Contact("Alice Smith", listOf("+1234567890"), "alice@example.com")
        )

        payloads.forEach { payload ->
            val id = repo.save(
                direction = Direction.SENT,
                payload = payload,
                profileSummary = "STEALTH / MFSK16 / NORMAL",
                durationMs = 1200L,
                snrDb = 20.0f,
                isSecure = true
            )

            val items = repo.observeItems().first()
            val item = items.first { it.id == id }
            assertFalse(item.isUnreadable)
            assertNotNull(item.payload)
            assertEquals(payload.messageType, item.payload?.messageType)
            assertEquals(payload, item.payload)
        }
    }
}
