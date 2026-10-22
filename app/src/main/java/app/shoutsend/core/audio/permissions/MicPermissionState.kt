package app.shoutsend.core.audio.permissions

/**
 * State representation for the microphone permission.
 */
enum class MicPermissionState {
    Granted,
    CanAsk,
    ShouldExplain,
    PermanentlyDenied;

    companion object {
        /**
         * Pure function resolving the microphone permission state based on system status.
         *
         * Priority order:
         * 1. If [isGranted] is true -> [Granted]
         * 2. Else if [shouldShowRationale] is true -> [ShouldExplain]
         * 3. Else if [hasRequestedBefore] is true -> [PermanentlyDenied]
         * 4. Else -> [CanAsk]
         */
        fun resolve(
            isGranted: Boolean,
            shouldShowRationale: Boolean,
            hasRequestedBefore: Boolean
        ): MicPermissionState {
            return when {
                isGranted -> Granted
                shouldShowRationale -> ShouldExplain
                hasRequestedBefore -> PermanentlyDenied
                else -> CanAsk
            }
        }
    }
}

/**
 * Top-level pure function resolving microphone permission state.
 */
fun resolve(
    isGranted: Boolean,
    shouldShowRationale: Boolean,
    hasRequestedBefore: Boolean
): MicPermissionState = MicPermissionState.resolve(
    isGranted = isGranted,
    shouldShowRationale = shouldShowRationale,
    hasRequestedBefore = hasRequestedBefore
)
