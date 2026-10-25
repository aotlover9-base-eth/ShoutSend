package app.shoutsend.core.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Coordinates session lifecycles with Android application lifecycle state.
 *
 * Automatically shuts down active acoustic listening if the application remains in the
 * background beyond a configurable grace period (default 5 seconds), preventing unauthorized
 * background microphone use and battery drain.
 */
class SessionLifecycleManager(
    private val receiveSession: ReceiveSession,
    private val defaultGraceSeconds: Int = 5,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
) {

    private var backgroundGraceJob: Job? = null

    var isBackgrounded: Boolean = false
        private set

    /**
     * Called when the application transitions to the background.
     * Starts the grace period timer before stopping the active receive session.
     */
    fun onAppBackgrounded(graceSeconds: Int = defaultGraceSeconds) {
        isBackgrounded = true
        backgroundGraceJob?.cancel()
        backgroundGraceJob = scope.launch {
            delay(graceSeconds * 1000L)
            if (receiveSession.state.value !is ReceiveState.Idle) {
                receiveSession.stop()
            }
        }
    }

    /**
     * Called when the application returns to the foreground.
     * Cancels any pending background shutdown timer.
     */
    fun onAppForegrounded() {
        isBackgrounded = false
        backgroundGraceJob?.cancel()
        backgroundGraceJob = null
    }
}
