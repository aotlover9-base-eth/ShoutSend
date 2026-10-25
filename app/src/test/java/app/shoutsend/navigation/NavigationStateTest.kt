package app.shoutsend.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStateTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun topLevelDestinationsResolveFromTabKey() {
        assertEquals(TopLevelDestination.SEND, TopLevelDestination.fromTabKey("send"))
        assertEquals(TopLevelDestination.RECEIVE, TopLevelDestination.fromTabKey("receive"))
        assertEquals(TopLevelDestination.VAULT, TopLevelDestination.fromTabKey("vault"))
        assertEquals(TopLevelDestination.RECEIVE, TopLevelDestination.fromTabKey("unknown"))
        assertEquals(TopLevelDestination.RECEIVE, TopLevelDestination.fromTabKey(""))
    }

    @Test
    fun topLevelDestinationsHaveValidMetadata() {
        for (dest in TopLevelDestination.entries) {
            assertTrue(dest.label.isNotBlank())
            assertTrue(dest.iconGlyph.isNotBlank())
            assertNotNull(dest.route)
        }
    }

    @Test
    fun routeSerializationRoundtrip() {
        // Test VaultDetailRoute serialization
        val detail = VaultDetailRoute(id = 12345L)
        val detailJson = json.encodeToString(detail)
        val decodedDetail = json.decodeFromString<VaultDetailRoute>(detailJson)
        assertEquals(12345L, decodedDetail.id)

        // Test routes
        val sendDefaultJson = json.encodeToString(SendRoute())
        val decodedDefaultSend = json.decodeFromString<SendRoute>(sendDefaultJson)
        assertEquals(SendRoute(), decodedDefaultSend)

        val sendWithArgs = SendRoute(sharedText = "https://example.com", sharedType = "URL")
        val sendWithArgsJson = json.encodeToString(sendWithArgs)
        val decodedArgsSend = json.decodeFromString<SendRoute>(sendWithArgsJson)
        assertEquals(sendWithArgs, decodedArgsSend)

        val transmitJson = json.encodeToString(TransmitRoute)
        val decodedTransmit = json.decodeFromString<TransmitRoute>(transmitJson)
        assertEquals(TransmitRoute, decodedTransmit)

        val receiveJson = json.encodeToString(ReceiveRoute)
        val decodedReceive = json.decodeFromString<ReceiveRoute>(receiveJson)
        assertEquals(ReceiveRoute, decodedReceive)

        val vaultJson = json.encodeToString(VaultRoute)
        val decodedVault = json.decodeFromString<VaultRoute>(vaultJson)
        assertEquals(VaultRoute, decodedVault)

        val settingsJson = json.encodeToString(SettingsRoute)
        val decodedSettings = json.decodeFromString<SettingsRoute>(settingsJson)
        assertEquals(SettingsRoute, decodedSettings)

        val calibrationJson = json.encodeToString(CalibrationRoute)
        val decodedCalibration = json.decodeFromString<CalibrationRoute>(calibrationJson)
        assertEquals(CalibrationRoute, decodedCalibration)

        val linkTestJson = json.encodeToString(LinkTestRoute)
        val decodedLinkTest = json.decodeFromString<LinkTestRoute>(linkTestJson)
        assertEquals(LinkTestRoute, decodedLinkTest)

        val onboardingJson = json.encodeToString(OnboardingRoute)
        val decodedOnboarding = json.decodeFromString<OnboardingRoute>(onboardingJson)
        assertEquals(OnboardingRoute, decodedOnboarding)
    }

    @Test
    fun startDestinationDecisionLogic() {
        fun resolveStart(onboardingCompleted: Boolean, lastTab: String): Any {
            return if (!onboardingCompleted) {
                OnboardingRoute
            } else {
                TopLevelDestination.fromTabKey(lastTab).route
            }
        }

        assertEquals(OnboardingRoute, resolveStart(onboardingCompleted = false, lastTab = "send"))
        assertEquals(OnboardingRoute, resolveStart(onboardingCompleted = false, lastTab = "vault"))
        assertEquals(SendRoute(), resolveStart(onboardingCompleted = true, lastTab = "send"))
        assertEquals(ReceiveRoute, resolveStart(onboardingCompleted = true, lastTab = "receive"))
        assertEquals(VaultRoute, resolveStart(onboardingCompleted = true, lastTab = "vault"))
        assertEquals(ReceiveRoute, resolveStart(onboardingCompleted = true, lastTab = "unknown"))
    }
}
