package app.shoutsend.feature.send

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SendUiState(
    val isTransmitting: Boolean = false,
    val payloadText: String = "",
    val statusMessage: String = "Ready to transmit"
)

class SendViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()
}
