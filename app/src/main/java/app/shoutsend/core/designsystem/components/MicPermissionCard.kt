package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.shoutsend.R
import app.shoutsend.core.audio.permissions.MicPermissionState
import app.shoutsend.core.designsystem.ChirpTheme

/**
 * Reusable tokenized card prompting users for microphone access across Receive and Calibration screens.
 */
@Composable
fun MicPermissionCard(
    state: MicPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state == MicPermissionState.Granted) return

    val isPermanentlyDenied = state == MicPermissionState.PermanentlyDenied
    val borderColor = if (isPermanentlyDenied) ChirpTheme.colors.warning else ChirpTheme.colors.primary
    val glowColor = if (isPermanentlyDenied) ChirpTheme.colors.warning else ChirpTheme.colors.glowPrimary

    NeonCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = borderColor,
        glowColor = glowColor
    ) {
        Column(
            modifier = Modifier.padding(ChirpTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        if (isPermanentlyDenied) R.string.permission_denied_title
                        else R.string.permission_rationale_title
                    ),
                    style = ChirpTheme.typography.titleMedium,
                    color = borderColor
                )

                StatusPill(
                    status = if (isPermanentlyDenied) AcousticStatus.ERROR else AcousticStatus.IDLE,
                    customLabel = if (isPermanentlyDenied) "BLOCKED" else "REQUIRED"
                )
            }

            Text(
                text = stringResource(
                    if (isPermanentlyDenied) R.string.permission_denied_body
                    else R.string.permission_rationale_body
                ),
                style = ChirpTheme.typography.bodyMedium,
                color = ChirpTheme.colors.onSurfaceVariant
            )

            if (isPermanentlyDenied) {
                CdButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.permission_btn_open_settings))
                }
            } else {
                CdButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.permission_btn_grant))
                }
            }
        }
    }
}
