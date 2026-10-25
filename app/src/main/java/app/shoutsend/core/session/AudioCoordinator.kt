package app.shoutsend.core.session

import app.shoutsend.core.audio.receiver.AcousticReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Operational mode of the acoustic hardware subsystem.
 */
enum class SessionMode {
    IDLE,
    TRANSMITTING,
    RECEIVING,
    CALIBRATING
}

/**
 * Coordinator guaranteeing half-duplex mutual exclusion between transmission and reception.
 */
interface AudioCoordinator {
    /** Current active acoustic session mode. */
    val activeMode: StateFlow<SessionMode>

    /**
     * Window flag recommendation: true while an acoustic session is active,
     * instructing the UI to keep the screen powered.
     */
    val keepScreenOn: StateFlow<Boolean>

    /**
     * Acquires exclusive lock for acoustic transmission.
     * Stops and releases [AcousticReceiver] if it is currently listening.
     */
    suspend fun acquireTransmitLock(): Boolean

    /**
     * Releases transmission lock, returning mode to [SessionMode.IDLE].
     */
    fun releaseTransmitLock()

    /**
     * Acquires exclusive lock for acoustic reception.
     * Rejects if transmission is actively in flight.
     */
    suspend fun acquireReceiveLock(): Boolean

    /**
     * Releases reception lock, returning mode to [SessionMode.IDLE].
     */
    fun releaseReceiveLock()

    /**
     * Acquires the loopback calibration lock (the explicit half-duplex exception).
     */
    suspend fun acquireCalibrationLock(): Boolean

    /**
     * Releases loopback calibration lock.
     */
    fun releaseCalibrationLock()
}

/**
 * Production implementation of [AudioCoordinator] enforcing mutual exclusion.
 */
class DefaultAudioCoordinator(
    private val receiver: AcousticReceiver
) : AudioCoordinator {

    private val mutex = Mutex()
    private val _activeMode = MutableStateFlow(SessionMode.IDLE)
    override val activeMode: StateFlow<SessionMode> = _activeMode.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(false)
    override val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    override suspend fun acquireTransmitLock(): Boolean = mutex.withLock {
        if (_activeMode.value == SessionMode.TRANSMITTING || _activeMode.value == SessionMode.CALIBRATING) {
            return false // Already active
        }

        // Enforce half-duplex: stop receiver if listening
        if (_activeMode.value == SessionMode.RECEIVING || receiver.isRunning) {
            receiver.stop()
        }

        updateMode(SessionMode.TRANSMITTING)
        true
    }

    override fun releaseTransmitLock() {
        synchronized(this) {
            if (_activeMode.value == SessionMode.TRANSMITTING) {
                updateMode(SessionMode.IDLE)
            }
        }
    }

    override suspend fun acquireReceiveLock(): Boolean = mutex.withLock {
        if (_activeMode.value == SessionMode.TRANSMITTING || _activeMode.value == SessionMode.CALIBRATING) {
            return false // Half-duplex collision
        }
        updateMode(SessionMode.RECEIVING)
        true
    }

    override fun releaseReceiveLock() {
        synchronized(this) {
            if (_activeMode.value == SessionMode.RECEIVING) {
                receiver.stop()
                updateMode(SessionMode.IDLE)
            }
        }
    }

    override suspend fun acquireCalibrationLock(): Boolean = mutex.withLock {
        if (_activeMode.value == SessionMode.TRANSMITTING || _activeMode.value == SessionMode.RECEIVING) {
            receiver.stop()
        }
        updateMode(SessionMode.CALIBRATING)
        true
    }

    override fun releaseCalibrationLock() {
        synchronized(this) {
            if (_activeMode.value == SessionMode.CALIBRATING) {
                receiver.stop()
                updateMode(SessionMode.IDLE)
            }
        }
    }

    private fun updateMode(mode: SessionMode) {
        _activeMode.value = mode
        _keepScreenOn.value = mode != SessionMode.IDLE
    }
}
