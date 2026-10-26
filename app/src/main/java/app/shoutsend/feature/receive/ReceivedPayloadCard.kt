package app.shoutsend.feature.receive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import app.shoutsend.R
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.core.designsystem.components.StatusPill
import app.shoutsend.modem.payload.Payload
import java.util.Locale

/**
 * Inline cyber-radio card displaying a successfully received acoustic payload
 * and rich instant action cards (Step 42).
 *
 * Conforms to UI Gate specifications:
 * - Positioned inline below the live waterfall display.
 * - Wi-Fi: Connect action copies sensitive password with 60s auto-clear and opens Wi-Fi settings.
 * - Contact: Save Contact intent pre-filled with name, phones, email; individual Call & Text buttons per phone.
 * - URL: Domain highlighted, non-HTTPS warning badge, user-confirmed browser launch (never auto-open).
 * - Text: Instant Share and Copy actions.
 * - All touch targets meet minimum 48dp with accessible content descriptions.
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var copiedNotice by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }

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
                val starDesc = stringResource(R.string.receive_btn_star)
                CdIconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.semantics { contentDescription = starDesc }
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

                        Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)) {
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
                    }

                    is Payload.WiFi -> {
                        Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)) {
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
                    }

                    is Payload.Contact -> {
                        Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)) {
                            Text(
                                text = payload.displayName,
                                style = ChirpTheme.typography.titleSmall,
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
                    }
                }
            }

            // Feedback notice (e.g. copied password notification)
            AnimatedVisibility(
                visible = copiedNotice != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ChirpTheme.shapes.extraSmall)
                        .background(colors.surfaceContainerHigh)
                        .padding(ChirpTheme.spacing.xs)
                ) {
                    Text(
                        text = copiedNotice.orEmpty(),
                        style = ChirpTheme.typography.labelSmall,
                        color = colors.onSurface
                    )
                }
            }

            // Primary Instant Action Strip
            when (payload) {
                is Payload.Text -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val shareDesc = stringResource(R.string.receive_action_share_desc)
                        CdButton(
                            onClick = {
                                try {
                                    context.startActivity(PayloadActionHelper.createShareIntent(payload.value))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                                .semantics { contentDescription = shareDesc }
                        ) {
                            Text(stringResource(R.string.receive_btn_share))
                        }

                        val copyDesc = stringResource(R.string.receive_action_copy_desc)
                        CdOutlinedButton(
                            onClick = {
                                PayloadActionHelper.copyToClipboard(context, payload.value, "ShoutSend Text")
                                copiedNotice = context.getString(R.string.receive_btn_copied)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                                .semantics { contentDescription = copyDesc }
                        ) {
                            Text(stringResource(R.string.receive_btn_copy))
                        }
                    }
                }

                is Payload.Url -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val openDesc = String.format(Locale.US, stringResource(R.string.receive_action_open_link_desc), payload.value)
                        CdButton(
                            onClick = {
                                try {
                                    context.startActivity(PayloadActionHelper.createBrowserIntent(payload.value))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                                .semantics { contentDescription = openDesc }
                        ) {
                            Text(stringResource(R.string.receive_btn_open_link))
                        }

                        val shareDesc = stringResource(R.string.receive_action_share_desc)
                        CdOutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(PayloadActionHelper.createShareIntent(payload.value))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                                .semantics { contentDescription = shareDesc }
                        ) {
                            Text(stringResource(R.string.receive_btn_share))
                        }

                        val copyDesc = stringResource(R.string.receive_action_copy_desc)
                        CdOutlinedButton(
                            onClick = {
                                PayloadActionHelper.copyToClipboard(context, payload.value, "ShoutSend URL")
                                copiedNotice = context.getString(R.string.receive_btn_copied)
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                                .semantics { contentDescription = copyDesc }
                        ) {
                            Text(stringResource(R.string.receive_btn_copy))
                        }
                    }
                }

                is Payload.WiFi -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        val connectDesc = stringResource(R.string.receive_action_connect_wifi_desc)
                        val copyNoticeText = stringResource(R.string.receive_wifi_copy_notice)
                        CdButton(
                            onClick = {
                                val password = payload.password
                                if (!password.isNullOrBlank()) {
                                    PayloadActionHelper.copyToClipboard(
                                        context = context,
                                        text = password,
                                        label = "Wi-Fi Password",
                                        isSensitive = true,
                                        scope = scope,
                                        autoClearSeconds = 60L
                                    )
                                    copiedNotice = copyNoticeText
                                }
                                try {
                                    context.startActivity(PayloadActionHelper.createWifiSettingsIntent())
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                                .semantics { contentDescription = connectDesc }
                        ) {
                            Text(stringResource(R.string.receive_btn_connect_wifi))
                        }

                        val copyPassDesc = stringResource(R.string.receive_action_copy_desc)
                        CdOutlinedButton(
                            onClick = {
                                val toCopy = payload.password ?: payload.ssid
                                PayloadActionHelper.copyToClipboard(
                                    context = context,
                                    text = toCopy,
                                    label = "Wi-Fi Credential",
                                    isSensitive = payload.password != null,
                                    scope = scope,
                                    autoClearSeconds = 60L
                                )
                                copiedNotice = context.getString(R.string.receive_btn_copied)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                                .semantics { contentDescription = copyPassDesc }
                        ) {
                            Text(
                                if (!payload.password.isNullOrBlank()) {
                                    stringResource(R.string.receive_btn_copy_password)
                                } else {
                                    stringResource(R.string.receive_btn_copy)
                                }
                            )
                        }
                    }
                }

                is Payload.Contact -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val saveDesc = String.format(Locale.US, stringResource(R.string.receive_action_save_contact_desc), payload.displayName)
                        CdButton(
                            onClick = {
                                try {
                                    context.startActivity(PayloadActionHelper.createSaveContactIntent(payload))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                                .semantics { contentDescription = saveDesc }
                        ) {
                            Text(stringResource(R.string.receive_btn_save_contact))
                        }

                        val shareDesc = stringResource(R.string.receive_action_share_desc)
                        CdOutlinedButton(
                            onClick = {
                                val summary = PayloadActionHelper.formatContactSummary(payload)
                                try {
                                    context.startActivity(PayloadActionHelper.createShareIntent(summary))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                                .semantics { contentDescription = shareDesc }
                        ) {
                            Text(stringResource(R.string.receive_btn_share))
                        }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
            ) {
                Text(stringResource(R.string.receive_btn_receive_another))
            }
        }
    }
}
