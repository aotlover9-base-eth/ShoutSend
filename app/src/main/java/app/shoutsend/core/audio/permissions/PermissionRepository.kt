package app.shoutsend.core.audio.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for observing and refreshing microphone permission state.
 */
interface PermissionRepository {
    /**
     * Observable microphone permission state.
     */
    val micState: StateFlow<MicPermissionState>

    /**
     * Refreshes the microphone permission state by querying the system and persistent request tracking.
     */
    suspend fun refresh()
}

/**
 * Android implementation of [PermissionRepository].
 *
 * @param context Context used to inspect permission grant status and package metadata.
 * @param activityProvider Optional provider supplying the current active foreground Activity to check rationale.
 * @param provider Provider tracking whether permission request was previously presented to the user.
 */
class AndroidPermissionRepository(
    private val context: Context,
    private val activityProvider: () -> Activity? = { null },
    val provider: MicPermissionRequestedProvider = InMemoryMicPermissionRequestedProvider()
) : PermissionRepository {

    private val isGranted: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private val _micState = MutableStateFlow(
        if (isGranted) MicPermissionState.Granted else MicPermissionState.CanAsk
    )
    override val micState: StateFlow<MicPermissionState> = _micState.asStateFlow()

    override suspend fun refresh() {
        val granted = isGranted
        val shouldShowRationale = if (!granted) {
            activityProvider()?.let { activity ->
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.RECORD_AUDIO
                )
            } ?: false
        } else {
            false
        }
        val hasRequestedBefore = provider.hasRequestedBefore()

        _micState.value = MicPermissionState.resolve(
            isGranted = granted,
            shouldShowRationale = shouldShowRationale,
            hasRequestedBefore = hasRequestedBefore
        )
    }
}

/**
 * Creates an [Intent] to open the application's details settings page in system Settings.
 *
 * Configured with `package:${context.packageName}` URI and [Intent.FLAG_ACTIVITY_NEW_TASK].
 */
fun createAppSettingsIntent(context: Context): Intent {
    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
