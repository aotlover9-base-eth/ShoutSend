package app.shoutsend.feature.receive

import androidx.lifecycle.ViewModel
import app.shoutsend.core.designsystem.components.AcousticStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReceiveUiState(
    val status: AcousticStatus = AcousticStatus.LISTENING,
    val snrDb: Double = 14.5,
    val levelDbfs: Float = -24.0f,
    val peakDbfs: Float = -12.0f
)

class ReceiveViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiveUiState())
    val uiState: StateFlow<ReceiveUiState> = _uiState.asStateFlow()
}
