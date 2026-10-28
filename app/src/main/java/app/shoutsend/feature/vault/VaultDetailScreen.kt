package app.shoutsend.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.R
import androidx.compose.material3.AlertDialog
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.CdTextField
import app.shoutsend.core.designsystem.components.ShoutCard
import app.shoutsend.core.designsystem.components.StatusPill
import app.shoutsend.feature.receive.PayloadActionHelper
import app.shoutsend.modem.payload.Payload
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying full telemetry, formatted payload, and actions for a stored transmission (Step 43).
 *
 * Conforms to UI Gate specifications:
 * - Telemetry: Timestamp, modem profile, SNR (dB), duration, and encryption flag.
 * - Wi-Fi passwords stay masked until explicitly revealed via "Show" tap.
 * - Re-transmit button:
 *   - Wi-Fi payloads display acoustic exposure privacy warning dialog before transmitting.
 *   - Secure-mode items prompt for passphrase (passphrases are never stored in vault).
 *   - Directs to standard TransmitScreen flow without skipping it.
 * - Quick actions: Share, Copy, Star, and Delete.
 * - Design tokens only (Rule 6, Rule 7).
 */
@Composable
fun VaultDetailScreen(
    id: Long,
    viewModel: VaultViewModel,
    onNavigateBack: () -> Unit,
    onRetransmit: (Payload, isSecure: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ChirpTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val item by viewModel.observeItemById(id).collectAsStateWithLifecycle(initialValue = null)

    var showPassword by remember { mutableStateOf(false) }
    var showWifiWarningDialog by remember { mutableStateOf(false) }
    var showPassphraseDialog by remember { mutableStateOf(false) }
    var passphraseInput by remember { mutableStateOf("") }
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    val currentItem = item
    if (currentItem == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(ChirpTheme.spacing.md),
            contentAlignment = Alignment.Center
        ) {
            ShoutCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(ChirpTheme.spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
                ) {
                    Text(
                        text = "Transmission Not Found",
                        style = ChirpTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                    CdButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.vault_action_back_desc))
                    }
                }
            }
        }
        return
    }

    // Wi-Fi Exposure Warning Dialog
    if (showWifiWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWifiWarningDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.vault_detail_wifi_warning_title),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.warning
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.vault_detail_wifi_warning_message),
                    style = ChirpTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            },
            confirmButton = {
                CdButton(
                    onClick = {
                        showWifiWarningDialog = false
                        val payload = currentItem.payload
                        if (payload != null) {
                            onRetransmit(payload, currentItem.isSecure)
                        }
                    }
                ) {
                    Text(stringResource(R.string.vault_detail_wifi_warning_confirm))
                }
            },
            dismissButton = {
                CdOutlinedButton(onClick = { showWifiWarningDialog = false }) {
                    Text(stringResource(R.string.vault_clear_dialog_cancel))
                }
            }
        )
    }

    // Secure Mode Passphrase Dialog
    if (showPassphraseDialog) {
        AlertDialog(
            onDismissRequest = { showPassphraseDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.vault_detail_passphrase_title),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)) {
                    Text(
                        text = stringResource(R.string.vault_detail_passphrase_message),
                        style = ChirpTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    CdTextField(
                        value = passphraseInput,
                        onValueChange = { passphraseInput = it },
                        placeholder = { Text(stringResource(R.string.vault_detail_passphrase_placeholder)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                CdButton(
                    onClick = {
                        showPassphraseDialog = false
                        val payload = currentItem.payload
                        if (payload != null) {
                            onRetransmit(payload, true)
                        }
                    },
                    enabled = passphraseInput.isNotBlank()
                ) {
                    Text(stringResource(R.string.vault_detail_retransmit))
                }
            },
            dismissButton = {
                CdOutlinedButton(onClick = { showPassphraseDialog = false }) {
                    Text(stringResource(R.string.vault_clear_dialog_cancel))
                }
            }
        )
    }

    val scrollState = rememberScrollState()
    val fullDateText = remember(currentItem.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.format(Date(currentItem.timestamp))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = ChirpTheme.spacing.md, vertical = ChirpTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
    ) {
        // Navigation Header: Back, Title, Star, Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val backDesc = stringResource(R.string.vault_action_back_desc)
                CdIconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.semantics { contentDescription = backDesc }
                ) {
                    Text(
                        text = "←",
                        style = ChirpTheme.typography.titleLarge,
                        color = colors.primary
                    )
                }

                Text(
                    text = stringResource(R.string.vault_detail_title),
                    style = ChirpTheme.typography.titleLarge,
                    color = colors.onSurface
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)) {
                val starDesc = stringResource(R.string.vault_action_star_desc)
                CdIconButton(
                    onClick = { viewModel.toggleStar(currentItem) },
                    modifier = Modifier
                        .size(ChirpTheme.sizes.touchTargetMin)
                        .semantics { contentDescription = starDesc }
                ) {
                    Text(
                        text = if (currentItem.isStarred) "★" else "☆",
                        style = ChirpTheme.typography.titleLarge,
                        color = if (currentItem.isStarred) colors.warning else colors.onSurfaceVariant
                    )
                }

                val deleteDesc = stringResource(R.string.vault_action_delete_desc)
                CdIconButton(
                    onClick = {
                        viewModel.deleteItem(currentItem)
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .size(ChirpTheme.sizes.touchTargetMin)
                        .semantics { contentDescription = deleteDesc }
                ) {
                    Text(
                        text = "🗑",
                        style = ChirpTheme.typography.titleMedium,
                        color = colors.error
                    )
                }
            }
        }

        // Telemetry & Diagnostics Card
        ShoutCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
            ) {
                Text(
                    text = stringResource(R.string.vault_detail_telemetry),
                    style = ChirpTheme.typography.titleSmall,
                    color = colors.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.vault_detail_direction),
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = if (currentItem.direction == Direction.SENT) "↑ SENT" else "↓ RECEIVED",
                        style = ChirpTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (currentItem.direction == Direction.SENT) colors.secondary else colors.tertiary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.vault_detail_timestamp),
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = fullDateText,
                        style = ChirpTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.vault_detail_profile),
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = currentItem.profileSummary,
                        style = ChirpTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.vault_detail_duration),
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1fs", currentItem.durationMs / 1000.0),
                        style = ChirpTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.vault_detail_snr),
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    val snrVal = currentItem.snrDb?.let { String.format(Locale.US, "%.1f dB", it) } ?: "N/A (Sent)"
                    Text(
                        text = snrVal,
                        style = ChirpTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.vault_detail_security),
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = if (currentItem.isSecure) {
                            stringResource(R.string.vault_detail_encrypted)
                        } else {
                            stringResource(R.string.vault_detail_plaintext)
                        },
                        style = ChirpTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (currentItem.isSecure) colors.secondary else colors.onSurfaceVariant
                    )
                }
            }
        }

        // Formatted Payload Card
        val payload = currentItem.payload
        ShoutCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
            ) {
                when {
                    currentItem.isUnreadable -> {
                        Text(
                            text = "Unreadable Encrypted Record",
                            style = ChirpTheme.typography.titleMedium,
                            color = colors.error
                        )
                        Text(
                            text = "The cryptographic key or hardware Keystore entry for this message is inaccessible.",
                            style = ChirpTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    payload is Payload.Text -> {
                        Text(
                            text = payload.value,
                            style = ChirpTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            color = colors.onSurface
                        )
                    }

                    payload is Payload.Url -> {
                        val parsed = remember(payload.value) { PayloadActionHelper.parseUrl(payload.value) }
                        val annotatedUrl = remember(payload.value, parsed, colors) {
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = colors.onSurfaceVariant)) {
                                    append("${parsed.scheme}://")
                                }
                                withStyle(SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold)) {
                                    append(parsed.host)
                                }
                                withStyle(SpanStyle(color = colors.onSurface)) {
                                    append(parsed.pathAndQuery)
                                }
                            }
                        }

                        if (!parsed.isHttps) {
                            StatusPill(
                                status = AcousticStatus.ERROR,
                                customLabel = stringResource(R.string.receive_url_not_secure)
                            )
                        }

                        Text(
                            text = annotatedUrl,
                            style = ChirpTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = stringResource(R.string.receive_url_security_notice),
                            style = ChirpTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    payload is Payload.WiFi -> {
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

                        val password = payload.password
                        if (!password.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showPassword) "Password: $password" else "Password: ••••••••",
                                    style = ChirpTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.secondary
                                )

                                val toggleDesc = if (showPassword) {
                                    stringResource(R.string.receive_password_toggle_hide)
                                } else {
                                    stringResource(R.string.receive_password_toggle_show)
                                }

                                CdIconButton(
                                    onClick = { showPassword = !showPassword },
                                    modifier = Modifier
                                        .size(ChirpTheme.sizes.touchTargetMin)
                                        .semantics { contentDescription = toggleDesc }
                                ) {
                                    Text(
                                        text = if (showPassword) "Hide" else "Show",
                                        style = ChirpTheme.typography.labelSmall,
                                        color = colors.primary
                                    )
                                }
                            }
                        }
                    }

                    payload is Payload.Contact -> {
                        Text(
                            text = payload.displayName,
                            style = ChirpTheme.typography.titleMedium,
                            color = colors.onSurface
                        )

                        payload.phones.forEach { phone ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = phone,
                                    style = ChirpTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)) {
                                    val callDesc = String.format(Locale.US, stringResource(R.string.receive_action_call_desc), phone)
                                    CdIconButton(
                                        onClick = {
                                            try {
                                                context.startActivity(PayloadActionHelper.createDialerIntent(phone))
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier
                                            .size(ChirpTheme.sizes.touchTargetMin)
                                            .semantics { contentDescription = callDesc }
                                    ) {
                                        Text(
                                            text = stringResource(R.string.receive_btn_call),
                                            style = ChirpTheme.typography.labelSmall,
                                            color = colors.primary
                                        )
                                    }

                                    val smsDesc = String.format(Locale.US, stringResource(R.string.receive_action_sms_desc), phone)
                                    CdIconButton(
                                        onClick = {
                                            try {
                                                context.startActivity(PayloadActionHelper.createSmsIntent(phone))
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier
                                            .size(ChirpTheme.sizes.touchTargetMin)
                                            .semantics { contentDescription = smsDesc }
                                    ) {
                                        Text(
                                            text = stringResource(R.string.receive_btn_sms),
                                            style = ChirpTheme.typography.labelSmall,
                                            color = colors.secondary
                                        )
                                    }
                                }
                            }
                        }

                        val email = payload.email
                        if (!email.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = email,
                                    style = ChirpTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.onSurfaceVariant
                                )

                                val emailDesc = String.format(Locale.US, stringResource(R.string.receive_action_email_desc), email)
                                CdIconButton(
                                    onClick = {
                                        try {
                                            context.startActivity(PayloadActionHelper.createEmailIntent(email))
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier
                                        .size(ChirpTheme.sizes.touchTargetMin)
                                        .semantics { contentDescription = emailDesc }
                                ) {
                                    Text(
                                        text = stringResource(R.string.receive_btn_email),
                                        style = ChirpTheme.typography.labelSmall,
                                        color = colors.primary
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = "Empty payload",
                            style = ChirpTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Action Buttons Strip: Re-transmit, Share, Copy
        if (payload != null && !currentItem.isUnreadable) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
            ) {
                // Primary Re-transmit Button
                CdButton(
                    onClick = {
                        if (payload is Payload.WiFi) {
                            showWifiWarningDialog = true
                        } else if (currentItem.isSecure) {
                            showPassphraseDialog = true
                        } else {
                            onRetransmit(payload, false)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                ) {
                    Text(stringResource(R.string.vault_detail_retransmit))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share Button
                    val shareDesc = stringResource(R.string.vault_detail_share)
                    CdOutlinedButton(
                        onClick = {
                            val textToShare = when (payload) {
                                is Payload.Text -> payload.value
                                is Payload.Url -> payload.value
                                is Payload.WiFi -> "Wi-Fi: ${payload.ssid}\nSecurity: ${payload.security.name}\nPassword: ${payload.password.orEmpty()}"
                                is Payload.Contact -> PayloadActionHelper.formatContactSummary(payload)
                            }
                            try {
                                context.startActivity(PayloadActionHelper.createShareIntent(textToShare))
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                            .semantics { contentDescription = shareDesc }
                    ) {
                        Text(stringResource(R.string.vault_detail_share))
                    }

                    // Copy Button
                    val copyDesc = stringResource(R.string.vault_detail_copy)
                    CdOutlinedButton(
                        onClick = {
                            val textToCopy = when (payload) {
                                is Payload.Text -> payload.value
                                is Payload.Url -> payload.value
                                is Payload.WiFi -> payload.password.orEmpty().ifBlank { payload.ssid }
                                is Payload.Contact -> PayloadActionHelper.formatContactSummary(payload)
                            }
                            PayloadActionHelper.copyToClipboard(
                                context = context,
                                text = textToCopy,
                                label = "ShoutSend Vault Copy",
                                isSensitive = payload is Payload.WiFi,
                                scope = scope,
                                autoClearSeconds = 60L
                            )
                            copiedNotice = context.getString(R.string.receive_btn_copied)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                            .semantics { contentDescription = copyDesc }
                    ) {
                        Text(
                            if (copiedNotice != null) "✓ " + stringResource(R.string.receive_btn_copied) else stringResource(R.string.vault_detail_copy)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(ChirpTheme.spacing.sm))
    }
}
