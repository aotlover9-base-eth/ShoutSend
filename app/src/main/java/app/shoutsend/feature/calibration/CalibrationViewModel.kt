package app.shoutsend.feature.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shoutsend.core.calibration.CalibrationResult
import app.shoutsend.core.calibration.CalibrationRunner
import app.shoutsend.core.data.settings.SettingsRepository
import app.shoutsend.core.session.AudioCoordinator
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.Speed
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lifecycle stage of the acoustic calibration procedure.
 */
sealed interface CalibrationStage {
    data object Idle : CalibrationStage
    data object NoiseSampling : CalibrationStage
    data class TransmittingTones(val progress: Float) : CalibrationStage
    data object Analyzing : CalibrationStage
    data class Completed(val result: CalibrationResult) : CalibrationStage
    data class Failed(val reason: String) : CalibrationStage
}

/**
 * State of the acoustic link test.
 */
sealed interface LinkTestStage {
    data object Idle : LinkTestStage
    data object Testing : LinkTestStage
    data class Success(val snrDb: Double, val noiseDbfs: Double) : LinkTestStage
    data class Error(val message: String) : LinkTestStage
}

/**
 * Immutable UI state for Calibration and Link Test screens.
 */
data class CalibrationUiState(
    val stage: CalibrationStage = CalibrationStage.Idle,
    val lastResult: CalibrationResult? = null,
    val recommendationApplied: Boolean = false,
    val linkTestStage: LinkTestStage = LinkTestStage.Idle,
    val activeBand: Band = Band.RETRO
)

/**
 * ViewModel orchestrating acoustic loopback calibration and link testing (Step 45).
 */
class CalibrationViewModel(
    private val calibrationRunner: CalibrationRunner,
    private val settingsRepository: SettingsRepository,
    private val audioCoordinator: AudioCoordinator? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    private var calibrationJob: Job? = null

    init {
        loadLastResult()
    }

    fun loadLastResult() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val savedJson = settings.lastCalibration
            val savedResult = savedJson?.let { CalibrationResult.fromJson(it) }
            _uiState.update {
                it.copy(
                    lastResult = savedResult,
                    activeBand = settings.band
                )
            }
        }
    }

    fun startCalibration() {
        if (_uiState.value.stage !is CalibrationStage.Idle &&
            _uiState.value.stage !is CalibrationStage.Completed &&
            _uiState.value.stage !is CalibrationStage.Failed
        ) {
            return
        }

        calibrationJob?.cancel()
        calibrationJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = CalibrationStage.NoiseSampling,
                    recommendationApplied = false
                )
            }

            try {
                val result = calibrationRunner.runCalibration { progress ->
                    when {
                        progress < 0.15f -> {
                            _uiState.update { it.copy(stage = CalibrationStage.NoiseSampling) }
                        }
                        progress < 0.90f -> {
                            val chirpProgress = ((progress - 0.15f) / 0.75f).coerceIn(0f, 1f)
                            _uiState.update { it.copy(stage = CalibrationStage.TransmittingTones(chirpProgress)) }
                        }
                        else -> {
                            _uiState.update { it.copy(stage = CalibrationStage.Analyzing) }
                        }
                    }
                }

                // Persist to SettingsRepository
                settingsRepository.setLastCalibration(result.toJson())

                _uiState.update {
                    it.copy(
                        stage = CalibrationStage.Completed(result),
                        lastResult = result
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        stage = CalibrationStage.Failed(e.message ?: "Calibration aborted")
                    )
                }
            }
        }
    }

    fun cancelCalibration() {
        calibrationJob?.cancel()
        calibrationJob = null
        _uiState.update { it.copy(stage = CalibrationStage.Idle) }
    }

    fun applyRecommendations() {
        val result = _uiState.value.lastResult ?: return
        viewModelScope.launch {
            settingsRepository.setBand(result.recommendedBand)
            settingsRepository.setSpeed(result.recommendedSpeed)
            _uiState.update { it.copy(recommendationApplied = true, activeBand = result.recommendedBand) }
        }
    }

    fun resetState() {
        calibrationJob?.cancel()
        calibrationJob = null
        _uiState.update { it.copy(stage = CalibrationStage.Idle, recommendationApplied = false) }
    }
}
