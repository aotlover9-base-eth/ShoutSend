package app.shoutsend.feature.onboarding

import androidx.lifecycle.ViewModel
import app.shoutsend.core.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 4
)

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    suspend fun completeOnboarding() {
        settingsRepository.setOnboardingCompleted(true)
    }
}
