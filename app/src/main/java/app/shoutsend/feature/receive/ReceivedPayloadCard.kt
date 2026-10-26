package app.shoutsend.feature.receive

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import app.shoutsend.R
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.modem.payload.Payload
import java.util.Locale

/**
 * Inline cyber-radio card displaying a successfully received acoustic payload.
 *
 * Conforms to UI Gate specifications:
 * - Positioned inline below the live waterfall display.
 * - Displays payload content according to type (Text, URL, Wi-Fi, Contact).
 * - Provides immediate one-tap starring to encrypted Vault storage.
 * - Displays signal metrics (SNR, active band) and error correction status.
 * - Never logs or exposes raw payload data outside the UI.
 */
@Composable
fun ReceivedPayloadCard(
    payload: Payload,
    snrDb: Double,
    profileSummary: String,
    isStarred: Boolean,
    isSavedToVault: Boolean,
    hasAutoRepairedNoise: Boolean,
    onToggleStar: () -> Unit,
    onReceiveAnother: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ChirpTheme.colors
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    NeonCard(
        modifier = modifier.fillMaxWidth(),
        glowColor = colors.success,
        borderColor = colors.success
    ) {
        Column(
            modifier = Modifier.padding(ChirpTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
        ) {
            // Header: Title and One-Tap Star Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✓",
                        style = ChirpTheme.typography.titleMedium,
                        color = colors.success
                    )
                    Text(
                        text = stringResource(R.string.receive_success_title),
                        style = ChirpTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                }

                // One-tap Star Button for Encrypted Vault
                CdIconButton(
                    onClick = onToggleStar
                ) {
                    Text(
                        text = if (isStarred) "★" else "☆",
                        style = ChirpTheme.typography.titleLarge,
                        color = if (isStarred) colors.warning else colors.onSurfaceVariant
                    )
                }
            }

            // Payload Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ChirpTheme.shapes.small)
                    .background(colors.surfaceContainerLowest)
                    .padding(ChirpTheme.spacing.md)
            ) {
                when (payload) {
                    is Payload.Text -> {
                        Text(
                            text = payload.value,
                            style = ChirpTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                    }
                    is Payload.Url -> {
                        Text(
                            text = payload.value,
                            style = ChirpTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = colors.primary
                        )
                    }
                    is Payload.WiFi -> {
                        Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)) {
                            Text(
                                text = "SSID: ${payload.ssid}",
                                style = ChirpTheme.typography.titleSmall,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Security: ${payload.security.name}",
                                style = ChirpTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                            if (!payload.password.isNullOrBlank()) {
                                Text(
                                    text = "Password: ${payload.password}",
                                    style = ChirpTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.secondary
                                )
                            }
                        }
                    }
                    is Payload.Contact -> {
                        Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)) {
                            Text(
                                text = payload.displayName,
                                style = ChirpTheme.typography.titleSmall,
                                color = colors.onSurface
                            )
                            payload.phones.forEach { phone ->
                                Text(
                                    text = "Phone: $phone",
                                    style = ChirpTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            if (!payload.email.isNullOrBlank()) {
                                Text(
                                    text = "Email: ${payload.email}",
                                    style = ChirpTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons Strip (Copy, Open Link)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CdOutlinedButton(
                    onClick = {
                        val textToCopy = when (payload) {
                            is Payload.Text -> payload.value
                            is Payload.Url -> payload.value
                            is Payload.WiFi -> payload.password.orEmpty().ifBlank { payload.ssid }
                            is Payload.Contact -> "${payload.displayName}\n${payload.phones.joinToString("\n")}\n${payload.email ?: ""}".trim()
                        }
                        clipboardManager.setText(AnnotatedString(textToCopy))
                        copiedNotice = textToCopy
                    }
                ) {
                    Text(
                        text = if (copiedNotice != null) "✓ " + stringResource(R.string.receive_btn_copied) else stringResource(R.string.receive_btn_copy)
                    )
                }

                if (payload is Payload.Url) {
                    CdButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(payload.value))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    ) {
                        Text(stringResource(R.string.receive_btn_open_link))
                    }
                }
            }

            Spacer(modifier = Modifier.height(ChirpTheme.spacing.xxs))

            // Footer Diagnostics & History Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (isSavedToVault) {
                        Text(
                            text = "✓ " + stringResource(R.string.receive_saved_to_vault),
                            style = ChirpTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "SNR ${String.format(Locale.US, "%.1f", snrDb)} dB · $profileSummary",
                        style = ChirpTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurfaceVariant
                    )
                }

                if (hasAutoRepairedNoise) {
                    Text(
                        text = "✓ " + stringResource(R.string.receive_noise_repaired),
                        style = ChirpTheme.typography.labelSmall,
                        color = colors.secondary
                    )
                }
            }

            // Bottom Return Action
            CdButton(
                onClick = onReceiveAnother,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.receive_btn_receive_another))
            }
        }
    }
}
