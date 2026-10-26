package app.shoutsend.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.shoutsend.core.data.settings.FakeSettingsRepository
import app.shoutsend.core.data.settings.Settings
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.core.data.vault.repository.VaultRepository
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.modem.payload.Payload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private class PreviewVaultRepository : VaultRepository {
    override fun observeItems(): Flow<List<VaultItem>> = MutableStateFlow<List<VaultItem>>(emptyList()).asStateFlow()
    override suspend fun save(
        direction: Direction,
        payload: Payload,
        profileSummary: String,
        durationMs: Long,
        snrDb: Float?,
        isSecure: Boolean
    ): Long = 1L
    override suspend fun delete(id: Long) {}
    override suspend fun deleteAll() {}
    override suspend fun setStarred(id: Long, isStarred: Boolean) {}
    override suspend fun prune(maxEntries: Int) {}
}

@Preview(name = "Settings Screen - Dark", showBackground = true)
@Composable
fun SettingsScreenDarkPreview() {
    val settingsRepo = FakeSettingsRepository(Settings())
    val vaultRepo = PreviewVaultRepository()
    val viewModel = SettingsViewModel(settingsRepo, vaultRepo)

    ChirpTheme(tokens = DesignTokens.defaultDark()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onNavigateToCalibration = {},
                onNavigateToOnboarding = {}
            )
        }
    }
}

@Preview(name = "Settings Screen - Light", showBackground = true)
@Composable
fun SettingsScreenLightPreview() {
    val settingsRepo = FakeSettingsRepository(Settings())
    val vaultRepo = PreviewVaultRepository()
    val viewModel = SettingsViewModel(settingsRepo, vaultRepo)

    ChirpTheme(tokens = DesignTokens.defaultLight()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChirpTheme.colors.background)
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onNavigateToCalibration = {},
                onNavigateToOnboarding = {}
            )
        }
    }
}
