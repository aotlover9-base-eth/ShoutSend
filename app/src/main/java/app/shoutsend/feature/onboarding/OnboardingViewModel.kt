package app.shoutsend.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shoutsend.core.audio.permissions.MicPermissionState
import app.shoutsend.core.audio.permissions.PermissionRepository
import app.shoutsend.core.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 4,
    val micPermissionState: MicPermissionState = MicPermissionState.CanAsk,
    val isOnboardingFinished: Boolean = false
)

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
    private val permissionRepository: PermissionRepository? = null
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _localMicState = MutableStateFlow(MicPermissionState.CanAsk)

    val uiState: StateFlow<OnboardingUiState> = combine(
        _currentPage,
        settingsRepository.settings,
        permissionRepository?.micState ?: MutableStateFlow(MicPermissionState.CanAsk),
        _localMicState
    ) { page, settings, repoMicState, localState ->
        val resolvedMic = when {
            repoMicState == MicPermissionState.Granted -> MicPermissionState.Granted
            localState != MicPermissionState.CanAsk -> localState
            settings.micPermissionRequested && repoMicState == MicPermissionState.CanAsk -> MicPermissionState.PermanentlyDenied
            else -> repoMicState
        }
        OnboardingUiState(
            currentPage = page,
            totalPages = 4,
            micPermissionState = resolvedMic,
            isOnboardingFinished = settings.onboardingCompleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = OnboardingUiState()
    )

    fun nextPage() {
        if (_currentPage.value < 3) {
            _currentPage.value += 1
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value -= 1
        }
    }

    fun skipToPermission() {
        _currentPage.value = 3
    }

    fun setPage(page: Int) {
        _currentPage.value = page.coerceIn(0, 3)
    }

    fun onPermissionRequested() {
        viewModelScope.launch {
            settingsRepository.setMicPermissionRequested(true)
            permissionRepository?.refresh()
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMicPermissionRequested(true)
            if (isGranted) {
                _localMicState.value = MicPermissionState.Granted
            } else {
                _localMicState.value = MicPermissionState.PermanentlyDenied
            }
            permissionRepository?.refresh()
        }
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            onSuccess()
        }
    }
}
