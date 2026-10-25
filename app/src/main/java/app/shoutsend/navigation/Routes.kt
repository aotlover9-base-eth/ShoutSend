package app.shoutsend.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for ShoutSend application destinations.
 */
@Serializable
data object OnboardingRoute

@Serializable
data class SendRoute(
    val sharedText: String? = null,
    val sharedType: String? = null
)

@Serializable
data object TransmitRoute

@Serializable
data object ReceiveRoute

@Serializable
data object VaultRoute

@Serializable
data class VaultDetailRoute(val id: Long)

@Serializable
data object SettingsRoute

@Serializable
data object CalibrationRoute

@Serializable
data object LinkTestRoute

/**
 * Top level bottom-bar / navigation-rail destinations.
 */
enum class TopLevelDestination(
    val route: Any,
    val tabKey: String,
    val label: String,
    val iconGlyph: String
) {
    SEND(SendRoute(), "send", "Send", "▲"),
    RECEIVE(ReceiveRoute, "receive", "Receive", "▼"),
    VAULT(VaultRoute, "vault", "Vault", "■");

    companion object {
        fun fromTabKey(key: String): TopLevelDestination {
            return entries.firstOrNull { it.tabKey.equals(key, ignoreCase = true) } ?: RECEIVE
        }
    }
}
