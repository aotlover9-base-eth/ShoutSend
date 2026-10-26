package app.shoutsend.feature.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.shoutsend.core.data.settings.SettingsRepository
import app.shoutsend.core.session.TransmitSession
import app.shoutsend.core.session.TransmitState
import app.shoutsend.modem.packet.AirTime
import app.shoutsend.modem.payload.Payload
import app.shoutsend.modem.payload.PayloadCompactor
import app.shoutsend.modem.payload.PayloadValidationResult
import app.shoutsend.modem.payload.WifiSecurity
import app.shoutsend.modem.profile.Band
import app.shoutsend.modem.profile.ModemProfile
import app.shoutsend.modem.profile.Scheme
import app.shoutsend.modem.profile.Speed
import app.shoutsend.modem.protocol.ProtocolConstants
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state model for the Send screen and transmission controller.
 */
data class SendUiState(
    val selectedType: SendPayloadType = SendPayloadType.TEXT,
    val textValue: String = "",
    val urlValue: String = "",
    // Wi-Fi form fields (Step 38)
    val wifiSsid: String = "",
    val wifiSecurity: WifiSecurity = WifiSecurity.WPA2,
    val wifiPassword: String = "",
    val wifiIsHidden: Boolean = false,
    val wifiPasswordVisible: Boolean = false,
    // Contact form fields (Step 38)
    val isContactManualMode: Boolean = false,
    val contactName: String = "",
    val contactPhone1: String = "",
    val contactPhone2: String = "",
    val contactEmail: String = "",
    val contactIncludePhone1: Boolean = true,
    val contactIncludePhone2: Boolean = true,
    val contactIncludeEmail: Boolean = true,
    val contactNotice: String? = null,
    // Tuning and hardware
    val selectedBand: Band = Band.RETRO,
    val selectedSpeed: Speed = Speed.NORMAL,
    val repeats: Int = 2,
    val isSecureEnabled: Boolean = false,
    val stealthWarningAccepted: Boolean = false,
    val showStealthWarningDialog: Boolean = false,
    val showSettingsBottomSheet: Boolean = false,
    val onAirBytes: Int = 0,
    val estimatedDurationSeconds: Double = 0.0,
    val isTooLong: Boolean = false,
    val validationError: String? = null,
    val isValid: Boolean = false,
    val truncatedNotice: String? = null,
    val isTransmitting: Boolean = false,
    val transmitState: TransmitState = TransmitState.Idle,
    val snackbarMessage: String? = null
)

/**
 * ViewModel orchestrating acoustic payload composition, live size & duration estimation,
 * transmission parameters, and acoustic session execution.
 */
class SendViewModel(
    private val settingsRepository: SettingsRepository,
    private val transmitSession: TransmitSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    private var calculationJob: Job? = null

    init {
        // Load initial settings and observe changes
        val initialSettings = settingsRepository.settings.value
        val initialType = try {
            SendPayloadType.valueOf(initialSettings.lastSendType.uppercase())
        } catch (_: Exception) {
            SendPayloadType.TEXT
        }

        _uiState.update { current ->
            current.copy(
                selectedType = initialType,
                selectedBand = initialSettings.band,
                selectedSpeed = initialSettings.speed,
                repeats = initialSettings.repeats,
                stealthWarningAccepted = initialSettings.stealthWarningAccepted
            )
        }

        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        selectedBand = settings.band,
                        selectedSpeed = settings.speed,
                        repeats = settings.repeats,
                        stealthWarningAccepted = settings.stealthWarningAccepted
                    )
                }
                recalculate(state = _uiState.value)
            }
        }

        viewModelScope.launch {
            transmitSession.state.collect { sessionState ->
                _uiState.update { current ->
                    current.copy(
                        transmitState = sessionState,
                        isTransmitting = sessionState is TransmitState.Preparing ||
                            sessionState is TransmitState.Transmitting
                    )
                }
            }
        }
    }

    /**
     * User selected a payload category chip.
     */
    fun selectPayloadType(type: SendPayloadType) {
        _uiState.update { it.copy(selectedType = type, truncatedNotice = null) }
        viewModelScope.launch {
            settingsRepository.setLastSendType(type.name.lowercase())
        }
        recalculate(state = _uiState.value)
    }

    /**
     * Text composer content changed.
     */
    fun onTextChanged(newText: String, debounceMs: Long = 150L) {
        _uiState.update { it.copy(textValue = newText, truncatedNotice = null) }
        scheduleRecalculation(debounceMs)
    }

    /**
     * URL composer content changed.
     */
    fun onUrlChanged(newUrl: String, debounceMs: Long = 150L) {
        _uiState.update { it.copy(urlValue = newUrl, truncatedNotice = null) }
        scheduleRecalculation(debounceMs)
    }

    /**
     * Wi-Fi SSID changed.
     */
    fun onWifiSsidChanged(newSsid: String, debounceMs: Long = 150L) {
        _uiState.update { it.copy(wifiSsid = newSsid) }
        scheduleRecalculation(debounceMs)
    }

    /**
     * Wi-Fi Security mode changed.
     */
    fun onWifiSecurityChanged(security: WifiSecurity) {
        _uiState.update { it.copy(wifiSecurity = security) }
        recalculate(state = _uiState.value)
    }

    /**
     * Wi-Fi Password changed.
     */
    fun onWifiPasswordChanged(newPassword: String, debounceMs: Long = 150L) {
        _uiState.update { it.copy(wifiPassword = newPassword) }
        scheduleRecalculation(debounceMs)
    }

    /**
     * Wi-Fi Hidden Network toggle changed.
     */
    fun onWifiIsHiddenChanged(hidden: Boolean) {
        _uiState.update { it.copy(wifiIsHidden = hidden) }
        recalculate(state = _uiState.value)
    }

    /**
     * Toggles Wi-Fi password masking visibility.
     */
    fun toggleWifiPasswordVisibility() {
        _uiState.update { it.copy(wifiPasswordVisible = !it.wifiPasswordVisible) }
    }

    /**
     * Sets Contact entry mode (system picker vs manual entry).
     */
    fun setContactManualMode(manual: Boolean) {
        _uiState.update { it.copy(isContactManualMode = manual) }
        recalculate(state = _uiState.value)
    }

    /**
     * Contact manual fields changed.
     */
    fun onContactNameChanged(name: String, debounceMs: Long = 150L) {
        _uiState.update { it.copy(contactName = name) }
        scheduleRecalculation(debounceMs)
    }

    fun onContactPhone1Changed(phone: String, debounceMs: Long = 150L) {
        _uiState.update { it.copy(contactPhone1 = phone) }
        scheduleRecalculation(debounceMs)
    }

    fun onContactPhone2Changed(phone: String, debounceMs: Long = 150L) {
        _uiState.update { it.copy(contactPhone2 = phone) }
        scheduleRecalculation(debounceMs)
    }

    fun onContactEmailChanged(email: String, debounceMs: Long = 150L) {
        _uiState.update { it.copy(contactEmail = email) }
        scheduleRecalculation(debounceMs)
    }

    fun toggleContactIncludePhone1() {
        _uiState.update { it.copy(contactIncludePhone1 = !it.contactIncludePhone1) }
        recalculate(state = _uiState.value)
    }

    fun toggleContactIncludePhone2() {
        _uiState.update { it.copy(contactIncludePhone2 = !it.contactIncludePhone2) }
        recalculate(state = _uiState.value)
    }

    fun toggleContactIncludeEmail() {
        _uiState.update { it.copy(contactIncludeEmail = !it.contactIncludeEmail) }
        recalculate(state = _uiState.value)
    }

    /**
     * Applies resolved contact data from system contact picker.
     */
    fun applyPickedContact(data: PickedContactData) {
        val p1 = data.phones.getOrNull(0) ?: ""
        val p2 = data.phones.getOrNull(1) ?: ""
        val em = data.email ?: ""

        _uiState.update { current ->
            current.copy(
                isContactManualMode = false,
                contactName = data.displayName,
                contactPhone1 = p1,
                contactPhone2 = p2,
                contactEmail = em,
                contactIncludePhone1 = p1.isNotBlank(),
                contactIncludePhone2 = p2.isNotBlank(),
                contactIncludeEmail = em.isNotBlank(),
                contactNotice = data.readError
            )
        }
        recalculate(state = _uiState.value)
    }

    /**
     * Enables Secure Mode with notification banner (Step 38 privacy cue).
     */
    fun enableSecureMode() {
        _uiState.update { current ->
            current.copy(
                isSecureEnabled = true,
                snackbarMessage = "Secure mode enabled. Passphrase entry arrives in Step 46."
            )
        }
    }

    /**
     * Loads a payload from the Vault for re-transmission.
     */
    fun loadPayloadForRetransmit(payload: Payload, isSecure: Boolean = false) {
        when (payload) {
            is Payload.Text -> {
                _uiState.update {
                    it.copy(
                        selectedType = SendPayloadType.TEXT,
                        textValue = payload.value,
                        isSecureEnabled = isSecure
                    )
                }
            }
            is Payload.Url -> {
                _uiState.update {
                    it.copy(
                        selectedType = SendPayloadType.URL,
                        urlValue = payload.value,
                        isSecureEnabled = isSecure
                    )
                }
            }
            is Payload.WiFi -> {
                _uiState.update {
                    it.copy(
                        selectedType = SendPayloadType.WIFI,
                        wifiSsid = payload.ssid,
                        wifiSecurity = payload.security,
                        wifiPassword = payload.password.orEmpty(),
                        wifiIsHidden = payload.isHidden,
                        isSecureEnabled = isSecure
                    )
                }
            }
            is Payload.Contact -> {
                _uiState.update {
                    it.copy(
                        selectedType = SendPayloadType.CONTACT,
                        isContactManualMode = true,
                        contactName = payload.displayName,
                        contactPhone1 = payload.phones.getOrNull(0).orEmpty(),
                        contactPhone2 = payload.phones.getOrNull(1).orEmpty(),
                        contactEmail = payload.email.orEmpty(),
                        contactIncludePhone1 = payload.phones.isNotEmpty(),
                        contactIncludePhone2 = payload.phones.size > 1,
                        contactIncludeEmail = !payload.email.isNullOrBlank(),
                        isSecureEnabled = isSecure
                    )
                }
            }
        }
        recalculate(state = _uiState.value)
    }

    /**
     * Wipes credentials and contact data from memory (Rule 25).
     * Invoked on screen departure or upon successful transmission.
     */
    fun clearSensitiveData() {
        _uiState.update { current ->
            current.copy(
                wifiPassword = "",
                wifiPasswordVisible = false,
                contactName = "",
                contactPhone1 = "",
                contactPhone2 = "",
                contactEmail = ""
            )
        }
        recalculate(state = _uiState.value)
    }

    /**
     * Band selection requested by user.
     * Stealth band enforces a one-time audible frequency notification dialog before activating.
     */
    fun onBandSelected(band: Band) {
        if (band == Band.STEALTH && !_uiState.value.stealthWarningAccepted) {
            _uiState.update { it.copy(showStealthWarningDialog = true) }
        } else {
            viewModelScope.launch {
                settingsRepository.setBand(band)
            }
        }
    }

    /**
     * User confirmed understanding the near-ultrasound Stealth frequency disclosure.
     */
    fun onAcceptStealthWarning() {
        _uiState.update { it.copy(showStealthWarningDialog = false) }
        viewModelScope.launch {
            settingsRepository.setStealthWarningAccepted(true)
            settingsRepository.setBand(Band.STEALTH)
        }
    }

    /**
     * User dismissed the Stealth warning dialog without accepting.
     */
    fun onDismissStealthWarning() {
        _uiState.update { it.copy(showStealthWarningDialog = false) }
    }

    /**
     * Speed selection changed.
     */
    fun onSpeedSelected(speed: Speed) {
        viewModelScope.launch {
            settingsRepository.setSpeed(speed)
        }
    }

    /**
     * Frame repetitions changed.
     */
    fun onRepeatsSelected(repeats: Int) {
        viewModelScope.launch {
            settingsRepository.setRepeats(repeats)
        }
    }

    /**
     * Toggles visibility of the transmission options bottom sheet.
     */
    fun setSettingsBottomSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showSettingsBottomSheet = visible) }
    }

    /**
     * Clears one-shot snackbar message.
     */
    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    /**
     * Applies parsed shared content from external ACTION_SEND intents.
     */
    fun consumeSharedRouteArgs(sharedText: String?, sharedTypeName: String?) {
        if (sharedText.isNullOrBlank()) return

        val parsed = SharedTextParser.parse(sharedText)
        val finalType = if (!sharedTypeName.isNullOrBlank()) {
            try {
                SendPayloadType.valueOf(sharedTypeName.uppercase())
            } catch (_: Exception) {
                parsed.type
            }
        } else {
            parsed.type
        }

        _uiState.update { current ->
            when (finalType) {
                SendPayloadType.TEXT -> current.copy(
                    selectedType = SendPayloadType.TEXT,
                    textValue = parsed.content,
                    truncatedNotice = if (parsed.isTruncated) "Text was shortened to fit acoustic limit (280 bytes)" else null
                )
                SendPayloadType.URL -> current.copy(
                    selectedType = SendPayloadType.URL,
                    urlValue = parsed.content,
                    truncatedNotice = if (parsed.isTruncated) "URL was shortened to fit acoustic limit (280 bytes)" else null
                )
                SendPayloadType.WIFI, SendPayloadType.CONTACT -> current.copy(
                    selectedType = finalType
                )
            }
        }

        viewModelScope.launch {
            settingsRepository.setLastSendType(finalType.name.lowercase())
        }
        recalculate(state = _uiState.value)
    }

    /**
     * Starts acoustic transmission session and triggers UI navigation.
     */
    fun startTransmission(onNavigateToTransmit: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid || state.isTransmitting) return

        val payload: Payload = when (state.selectedType) {
            SendPayloadType.TEXT -> Payload.Text(state.textValue)
            SendPayloadType.URL -> Payload.Url(state.urlValue)
            SendPayloadType.WIFI -> {
                val pass = if (state.wifiSecurity == WifiSecurity.OPEN) null else state.wifiPassword
                Payload.WiFi(
                    ssid = state.wifiSsid,
                    password = pass,
                    security = state.wifiSecurity,
                    isHidden = state.wifiIsHidden
                )
            }
            SendPayloadType.CONTACT -> {
                val phones = mutableListOf<String>()
                if (state.contactIncludePhone1 && state.contactPhone1.isNotBlank()) {
                    phones.add(state.contactPhone1.trim())
                }
                if (state.contactIncludePhone2 && state.contactPhone2.isNotBlank()) {
                    phones.add(state.contactPhone2.trim())
                }
                val email = if (state.contactIncludeEmail && state.contactEmail.isNotBlank()) {
                    state.contactEmail.trim()
                } else {
                    null
                }
                Payload.Contact(
                    displayName = state.contactName.trim(),
                    phones = phones,
                    email = email
                )
            }
        }

        val profile = ModemProfile(
            band = state.selectedBand,
            scheme = Scheme.MFSK16,
            speed = state.selectedSpeed,
            repeats = state.repeats
        )

        onNavigateToTransmit()

        viewModelScope.launch {
            try {
                transmitSession.send(
                    payload = payload,
                    profile = profile,
                    securePassphrase = null // Step 46 wires Secure Mode passphrase
                )
            } finally {
                clearSensitiveData()
            }
        }
    }

    /**
     * Cancels active transmission immediately.
     */
    fun cancelTransmission() {
        transmitSession.cancel("CancelledByUser")
    }

    private fun scheduleRecalculation(debounceMs: Long) {
        calculationJob?.cancel()
        if (debounceMs <= 0L) {
            recalculate(state = _uiState.value)
        } else {
            calculationJob = viewModelScope.launch {
                delay(debounceMs)
                recalculate(state = _uiState.value)
            }
        }
    }

    /**
     * Pure recalculation of payload validation, on-air compaction, and transmission air-time.
     */
    fun recalculate(state: SendUiState = _uiState.value) {
        val profile = ModemProfile(
            band = state.selectedBand,
            scheme = Scheme.MFSK16,
            speed = state.selectedSpeed,
            repeats = state.repeats
        )

        when (state.selectedType) {
            SendPayloadType.TEXT -> {
                if (state.textValue.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            onAirBytes = 0,
                            estimatedDurationSeconds = 0.0,
                            isTooLong = false,
                            validationError = null,
                            isValid = false
                        )
                    }
                    return
                }

                val rawBytes = state.textValue.toByteArray(Charsets.UTF_8).size
                val isByteLimitExceeded = rawBytes > 280
                val payload = Payload.Text(state.textValue)
                val validation = payload.validate()

                if (isByteLimitExceeded || validation is PayloadValidationResult.Invalid) {
                    val onAirBytes = rawBytes
                    val isTooLong = isByteLimitExceeded || onAirBytes > ProtocolConstants.MAX_AIR_PAYLOAD_BYTES
                    val duration = AirTime.estimate(
                        profile = profile,
                        onAirBytes = onAirBytes,
                        repeats = state.repeats
                    )
                    val error = if (isTooLong) null else (validation as? PayloadValidationResult.Invalid)?.error?.reason

                    _uiState.update {
                        it.copy(
                            onAirBytes = onAirBytes,
                            estimatedDurationSeconds = duration,
                            isTooLong = isTooLong,
                            validationError = error,
                            isValid = false
                        )
                    }
                } else {
                    val compactResult = PayloadCompactor.compact(payload)
                    val onAirBytes = compactResult.bytes.size
                    val isTooLong = onAirBytes > ProtocolConstants.MAX_AIR_PAYLOAD_BYTES

                    val duration = AirTime.estimate(
                        profile = profile,
                        onAirBytes = onAirBytes,
                        repeats = state.repeats
                    )

                    _uiState.update {
                        it.copy(
                            onAirBytes = onAirBytes,
                            estimatedDurationSeconds = duration,
                            isTooLong = isTooLong,
                            validationError = null,
                            isValid = !isTooLong
                        )
                    }
                }
            }
            SendPayloadType.URL -> {
                if (state.urlValue.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            onAirBytes = 0,
                            estimatedDurationSeconds = 0.0,
                            isTooLong = false,
                            validationError = null,
                            isValid = false
                        )
                    }
                    return
                }

                val trimmed = state.urlValue.trim()
                val rawBytes = trimmed.toByteArray(Charsets.UTF_8).size
                val isByteLimitExceeded = rawBytes > 280
                val payload = Payload.Url(state.urlValue)
                val validation = payload.validate()

                if (isByteLimitExceeded || validation is PayloadValidationResult.Invalid) {
                    val onAirBytes = rawBytes
                    val isTooLong = isByteLimitExceeded || onAirBytes > ProtocolConstants.MAX_AIR_PAYLOAD_BYTES
                    val duration = AirTime.estimate(
                        profile = profile,
                        onAirBytes = onAirBytes,
                        repeats = state.repeats
                    )
                    val error = if (isTooLong) null else (validation as? PayloadValidationResult.Invalid)?.error?.reason

                    _uiState.update {
                        it.copy(
                            onAirBytes = onAirBytes,
                            estimatedDurationSeconds = duration,
                            isTooLong = isTooLong,
                            validationError = error,
                            isValid = false
                        )
                    }
                } else {
                    val compactResult = PayloadCompactor.compact(payload)
                    val onAirBytes = compactResult.bytes.size
                    val isTooLong = onAirBytes > ProtocolConstants.MAX_AIR_PAYLOAD_BYTES

                    val duration = AirTime.estimate(
                        profile = profile,
                        onAirBytes = onAirBytes,
                        repeats = state.repeats
                    )

                    _uiState.update {
                        it.copy(
                            onAirBytes = onAirBytes,
                            estimatedDurationSeconds = duration,
                            isTooLong = isTooLong,
                            validationError = null,
                            isValid = !isTooLong
                        )
                    }
                }
            }
            SendPayloadType.WIFI -> {
                if (state.wifiSsid.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            onAirBytes = 0,
                            estimatedDurationSeconds = 0.0,
                            isTooLong = false,
                            validationError = null,
                            isValid = false
                        )
                    }
                    return
                }

                val pass = if (state.wifiSecurity == WifiSecurity.OPEN) null else state.wifiPassword
                val payload = Payload.WiFi(
                    ssid = state.wifiSsid,
                    password = pass,
                    security = state.wifiSecurity,
                    isHidden = state.wifiIsHidden
                )
                val validation = payload.validate()

                if (validation is PayloadValidationResult.Invalid) {
                    val rawBytes = state.wifiSsid.toByteArray(Charsets.UTF_8).size +
                        (pass?.toByteArray(Charsets.UTF_8)?.size ?: 0) + 4
                    val isTooLong = rawBytes > ProtocolConstants.MAX_AIR_PAYLOAD_BYTES
                    val duration = AirTime.estimate(
                        profile = profile,
                        onAirBytes = rawBytes,
                        repeats = state.repeats
                    )
                    val error = validation.error.reason

                    _uiState.update {
                        it.copy(
                            onAirBytes = rawBytes,
                            estimatedDurationSeconds = duration,
                            isTooLong = isTooLong,
                            validationError = error,
                            isValid = false
                        )
                    }
                } else {
                    val compactResult = PayloadCompactor.compact(payload)
                    val onAirBytes = compactResult.bytes.size
                    val isTooLong = onAirBytes > ProtocolConstants.MAX_AIR_PAYLOAD_BYTES

                    val duration = AirTime.estimate(
                        profile = profile,
                        onAirBytes = onAirBytes,
                        repeats = state.repeats
                    )

                    _uiState.update {
                        it.copy(
                            onAirBytes = onAirBytes,
                            estimatedDurationSeconds = duration,
                            isTooLong = isTooLong,
                            validationError = null,
                            isValid = !isTooLong
                        )
                    }
                }
            }
            SendPayloadType.CONTACT -> {
                if (state.contactName.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            onAirBytes = 0,
                            estimatedDurationSeconds = 0.0,
                            isTooLong = false,
                            validationError = null,
                            isValid = false
                        )
                    }
                    return
                }

                val phones = mutableListOf<String>()
                if (state.contactIncludePhone1 && state.contactPhone1.isNotBlank()) {
                    phones.add(state.contactPhone1.trim())
                }
                if (state.contactIncludePhone2 && state.contactPhone2.isNotBlank()) {
                    phones.add(state.contactPhone2.trim())
                }
                val email = if (state.contactIncludeEmail && state.contactEmail.isNotBlank()) {
                    state.contactEmail.trim()
                } else {
                    null
                }

                val payload = Payload.Contact(
                    displayName = state.contactName.trim(),
                    phones = phones,
                    email = email
                )
                val validation = payload.validate()

                if (validation is PayloadValidationResult.Invalid) {
                    val rawBytes = state.contactName.toByteArray(Charsets.UTF_8).size +
                        phones.sumOf { it.toByteArray(Charsets.UTF_8).size } +
                        (email?.toByteArray(Charsets.UTF_8)?.size ?: 0) + 4
                    val isTooLong = rawBytes > ProtocolConstants.MAX_AIR_PAYLOAD_BYTES
                    val duration = AirTime.estimate(
                        profile = profile,
                        onAirBytes = rawBytes,
                        repeats = state.repeats
                    )
                    val error = validation.error.reason

                    _uiState.update {
                        it.copy(
                            onAirBytes = rawBytes,
                            estimatedDurationSeconds = duration,
                            isTooLong = isTooLong,
                            validationError = error,
                            isValid = false
                        )
                    }
                } else {
                    val compactResult = PayloadCompactor.compact(payload)
                    val onAirBytes = compactResult.bytes.size
                    val isTooLong = onAirBytes > ProtocolConstants.MAX_AIR_PAYLOAD_BYTES

                    val duration = AirTime.estimate(
                        profile = profile,
                        onAirBytes = onAirBytes,
                        repeats = state.repeats
                    )

                    _uiState.update {
                        it.copy(
                            onAirBytes = onAirBytes,
                            estimatedDurationSeconds = duration,
                            isTooLong = isTooLong,
                            validationError = null,
                            isValid = !isTooLong
                        )
                    }
                }
            }
        }
    }
}
