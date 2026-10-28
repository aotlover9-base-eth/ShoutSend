package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.shoutsend.core.audio.permissions.MicPermissionState
import app.shoutsend.core.designsystem.ShoutSendTheme

/**
 * Explanatory card rendered when RECORD_AUDIO permission is not granted.
 * Provides clear rationale explaining that microphone access is required purely for acoustic demodulation.
 */
@Composable
fun MicPermissionCard(
    state: MicPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    MicPermissionCard(
        isPermanentlyDenied = state == MicPermissionState.PermanentlyDenied,
        onRequestPermission = onRequestPermission,
        onOpenSettings = onOpenSettings,
        modifier = modifier
    )
}

@Composable
fun MicPermissionCard(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isPermanentlyDenied) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    ShoutCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ShoutSendTheme.spacing.md)
        ) {
            Text(
                text = if (isPermanentlyDenied) "Microphone permission required" else "Microphone access needed",
                style = MaterialTheme.typography.titleMedium,
                color = if (isPermanentlyDenied) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(ShoutSendTheme.spacing.xs))

            Text(
                text = if (isPermanentlyDenied) {
                    "Microphone access is permanently denied. ShoutSend needs the microphone to listen for acoustic data chirps. Please enable it in system settings."
                } else {
                    "ShoutSend decodes data transmitted through sound waves. Microphone permission is strictly required to detect acoustic signals. No audio is ever recorded or transmitted over the internet."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPermanentlyDenied) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(ShoutSendTheme.spacing.md))

            if (isPermanentlyDenied) {
                ShoutFilledButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open settings")
                }
            } else {
                ShoutFilledButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant microphone permission")
                }
            }
        }
    }
}
