package app.shoutsend.core.audio.permissions

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Provider interface for tracking whether the user has previously been prompted for the microphone permission.
 */
interface MicPermissionRequestedProvider {
    /**
     * Returns true if permission request was previously presented to the user.
     */
    suspend fun hasRequestedBefore(): Boolean

    /**
     * Updates the requested-before state.
     */
    suspend fun setRequestedBefore(requested: Boolean)
}

/**
 * In-memory implementation of [MicPermissionRequestedProvider] for unit testing and default fallback.
 */
class InMemoryMicPermissionRequestedProvider(
    initialValue: Boolean = false
) : MicPermissionRequestedProvider {
    private val requested = AtomicBoolean(initialValue)

    override suspend fun hasRequestedBefore(): Boolean = requested.get()

    override suspend fun setRequestedBefore(requested: Boolean) {
        this.requested.set(requested)
    }
}
