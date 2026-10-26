package app.shoutsend.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.core.data.vault.repository.VaultRepository
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.WifiSecurity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private class PreviewVaultRepository(initialItems: List<VaultItem> = emptyList()) : VaultRepository {
    private val _flow = MutableStateFlow(initialItems)
    override fun observeItems(): Flow<List<VaultItem>> = _flow.asStateFlow()
    override suspend fun save(
        direction: Direction,
        payload: Payload,
        profileSummary: String,
        durationMs: Long,
        snrDb: Float?,
        isSecure: Boolean
    ): Long = 101L

    override suspend fun delete(id: Long) {}
    override suspend fun deleteAll() {}
    override suspend fun setStarred(id: Long, isStarred: Boolean) {}
    override suspend fun prune(maxEntries: Int) {}
}

@Preview(name = "Vault Screen - Populated (Dark)", showBackground = true)
@Composable
fun VaultScreenPopulatedPreview() {
    val items = listOf(
        VaultItem(
            id = 1L,
            timestamp = System.currentTimeMillis() - 120_000L,
            direction = Direction.SENT,
            payload = Payload.Text("Cyber radio acoustic test broadcast."),
            isUnreadable = false,
            isSecure = false,
            isStarred = true,
            profileSummary = "Retro Normal",
            durationMs = 4500L,
            snrDb = null
        ),
        VaultItem(
            id = 2L,
            timestamp = System.currentTimeMillis() - 360_000L,
            direction = Direction.RECEIVED,
            payload = Payload.WiFi("NeonMesh-5G", "SuperSecurePassword99", WifiSecurity.WPA2),
            isUnreadable = false,
            isSecure = false,
            isStarred = false,
            profileSummary = "Stealth Fast",
            durationMs = 3800L,
            snrDb = 22.8f
        ),
        VaultItem(
            id = 3L,
            timestamp = System.currentTimeMillis() - 720_000L,
            direction = Direction.RECEIVED,
            payload = Payload.Contact("Elena Rostova", listOf("+15550198888"), "elena@shoutsend.app"),
            isUnreadable = false,
            isSecure = true,
            isStarred = true,
            profileSummary = "Retro Robust",
            durationMs = 6200L,
            snrDb = 19.4f
        )
    )

    val viewModel = VaultViewModel(PreviewVaultRepository(items))

    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            VaultScreen(
                viewModel = viewModel,
                onNavigateToDetail = {}
            )
        }
    }
}

@Preview(name = "Vault Screen - Empty State (Dark)", showBackground = true)
@Composable
fun VaultScreenEmptyPreview() {
    val viewModel = VaultViewModel(PreviewVaultRepository(emptyList()))

    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            VaultScreen(
                viewModel = viewModel,
                onNavigateToDetail = {}
            )
        }
    }
}

@Preview(name = "Vault Detail Screen - Wi-Fi (Dark)", showBackground = true)
@Composable
fun VaultDetailScreenPreview() {
    val item = VaultItem(
        id = 42L,
        timestamp = System.currentTimeMillis() - 60_000L,
        direction = Direction.RECEIVED,
        payload = Payload.WiFi("CyberRadio-Guest", "SecretKey12345", WifiSecurity.WPA3),
        isUnreadable = false,
        isSecure = false,
        isStarred = true,
        profileSummary = "Retro Normal",
        durationMs = 4200L,
        snrDb = 23.5f
    )
    val viewModel = VaultViewModel(PreviewVaultRepository(listOf(item)))

    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            VaultDetailScreen(
                id = 42L,
                viewModel = viewModel,
                onNavigateBack = {},
                onRetransmit = { _, _ -> }
            )
        }
    }
}
