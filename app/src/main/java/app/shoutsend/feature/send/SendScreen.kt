package app.shoutsend.feature.send

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.R
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.designsystem.components.ByteMeter
import app.shoutsend.core.designsystem.components.CdAlertDialog
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdFilterChip
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.CdModalBottomSheet
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.CdOutlinedTextField
import app.shoutsend.core.designsystem.components.CdSwitch
import app.shoutsend.core.designsystem.components.ModeSelector
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.core.designsystem.components.SpeedSelector
import app.shoutsend.core.designsystem.components.StatusPill
import app.shoutsend.modem.crypto.PassphraseEvaluator
import app.shoutsend.modem.crypto.PassphraseStrengthClass
import kotlin.math.ceil

/**
 * Top-level Send destination screen allowing users to compose acoustic messages,
 * validate byte budgets live, tune modem frequency and speed profiles, and initiate acoustic transmission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    viewModel: SendViewModel,
    onNavigateToTransmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Clear credentials and sensitive contact details when leaving the screen (Rule 25)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSensitiveData()
        }
    }

    // Stealth Band First-Use Warning Dialog
    if (uiState.showStealthWarningDialog) {
        CdAlertDialog(
            onDismissRequest = { viewModel.onDismissStealthWarning() },
            title = {
                Text(
                    text = stringResource(R.string.send_stealth_dialog_title),
                    style = ChirpTheme.typography.titleMedium,
                    color = ChirpTheme.colors.onSurface
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.send_stealth_dialog_body),
                    style = ChirpTheme.typography.bodyMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
            },
            confirmButton = {
                CdButton(onClick = { viewModel.onAcceptStealthWarning() }) {
                    Text(stringResource(R.string.send_stealth_dialog_confirm))
                }
            },
            dismissButton = {
                CdOutlinedButton(onClick = { viewModel.onDismissStealthWarning() }) {
                    Text(stringResource(R.string.send_stealth_dialog_cancel))
                }
            }
        )
    }

    // Transmission Options Bottom Sheet (Repeats & Volume Tip)
    if (uiState.showSettingsBottomSheet) {
        CdModalBottomSheet(
            onDismissRequest = { viewModel.setSettingsBottomSheetVisible(false) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ChirpTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
            ) {
                Text(
                    text = stringResource(R.string.send_options_title),
                    style = ChirpTheme.typography.titleLarge,
                    color = ChirpTheme.colors.primary
                )

                Text(
                    text = stringResource(R.string.send_repeats_label),
                    style = ChirpTheme.typography.titleSmall,
                    color = ChirpTheme.colors.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
                ) {
                    listOf(1, 2, 3).forEach { rep ->
                        CdFilterChip(
                            selected = uiState.repeats == rep,
                            onClick = { viewModel.onRepeatsSelected(rep) },
                            label = { Text("${rep}x") }
                        )
                    }
                }

                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(ChirpTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        Text(
                            text = stringResource(R.string.send_volume_tip_title),
                            style = ChirpTheme.typography.titleSmall,
                            color = ChirpTheme.colors.warning
                        )
                        Text(
                            text = stringResource(R.string.send_volume_tip_body),
                            style = ChirpTheme.typography.bodySmall,
                            color = ChirpTheme.colors.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ChirpTheme.spacing.sm))

                CdButton(
                    onClick = { viewModel.setSettingsBottomSheetVisible(false) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.transmit_btn_done))
                }
            }
        }
    }

    // Secure Mode Passphrase Dialog (Step 46)
    if (uiState.showPassphraseDialog) {
        CdAlertDialog(
            onDismissRequest = { viewModel.dismissPassphraseDialog() },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔒",
                        style = ChirpTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.send_secure_dialog_title),
                        style = ChirpTheme.typography.titleMedium,
                        color = ChirpTheme.colors.onSurface
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.send_secure_dialog_description),
                        style = ChirpTheme.typography.bodyMedium,
                        color = ChirpTheme.colors.onSurfaceVariant
                    )

                    CdOutlinedTextField(
                        value = uiState.passphraseInput,
                        onValueChange = { viewModel.onPassphraseInputChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.send_secure_dialog_passphrase_label)) },
                        placeholder = { Text(stringResource(R.string.send_secure_dialog_passphrase_placeholder)) },
                        singleLine = true,
                        visualTransformation = if (uiState.isPassphraseVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (uiState.passphraseInput.length >= PassphraseEvaluator.MIN_LENGTH) {
                                    viewModel.confirmPassphrase()
                                }
                            }
                        ),
                        isError = uiState.passphraseError != null,
                        trailingIcon = {
                            CdIconButton(
                                onClick = { viewModel.togglePassphraseVisibility() }
                            ) {
                                Text(
                                    text = if (uiState.isPassphraseVisible) "👁" else "🙈",
                                    style = ChirpTheme.typography.titleMedium,
                                    color = ChirpTheme.colors.onSurfaceVariant
                                )
                            }
                        }
                    )

                    if (uiState.passphraseError != null) {
                        Text(
                            text = uiState.passphraseError!!,
                            style = ChirpTheme.typography.bodySmall,
                            color = ChirpTheme.colors.error
                        )
                    } else if (uiState.passphraseStrength != null && uiState.passphraseStrengthMessage != null) {
                        val strengthColor = when (uiState.passphraseStrength) {
                            PassphraseStrengthClass.STRONG -> ChirpTheme.colors.success
                            PassphraseStrengthClass.OK -> ChirpTheme.colors.primary
                            PassphraseStrengthClass.WEAK -> ChirpTheme.colors.warning
                            null -> ChirpTheme.colors.onSurfaceVariant
                        }
                        val strengthLabel = when (uiState.passphraseStrength) {
                            PassphraseStrengthClass.STRONG -> stringResource(R.string.send_secure_strength_strong)
                            PassphraseStrengthClass.OK -> stringResource(R.string.send_secure_strength_ok)
                            PassphraseStrengthClass.WEAK -> stringResource(R.string.send_secure_strength_weak)
                            null -> ""
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)) {
                            Text(
                                text = "Strength: $strengthLabel",
                                style = ChirpTheme.typography.labelSmall,
                                color = strengthColor
                            )
                            Text(
                                text = uiState.passphraseStrengthMessage!!,
                                style = ChirpTheme.typography.bodySmall,
                                color = ChirpTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                CdButton(
                    onClick = { viewModel.confirmPassphrase() },
                    enabled = uiState.passphraseInput.length >= PassphraseEvaluator.MIN_LENGTH
                ) {
                    Text(stringResource(R.string.send_secure_dialog_confirm))
                }
            },
            dismissButton = {
                CdOutlinedButton(onClick = { viewModel.dismissPassphraseDialog() }) {
                    Text(stringResource(R.string.send_secure_dialog_cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(ChirpTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
    ) {
        // --- 1. Type Selector Chips Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
        ) {
            CdFilterChip(
                selected = uiState.selectedType == SendPayloadType.TEXT,
                onClick = { viewModel.selectPayloadType(SendPayloadType.TEXT) },
                label = { Text(stringResource(R.string.send_type_text)) }
            )
            CdFilterChip(
                selected = uiState.selectedType == SendPayloadType.URL,
                onClick = { viewModel.selectPayloadType(SendPayloadType.URL) },
                label = { Text(stringResource(R.string.send_type_url)) }
            )
            CdFilterChip(
                selected = uiState.selectedType == SendPayloadType.WIFI,
                onClick = { viewModel.selectPayloadType(SendPayloadType.WIFI) },
                label = { Text(stringResource(R.string.send_type_wifi)) }
            )
            CdFilterChip(
                selected = uiState.selectedType == SendPayloadType.CONTACT,
                onClick = { viewModel.selectPayloadType(SendPayloadType.CONTACT) },
                label = { Text(stringResource(R.string.send_type_contact)) }
            )
        }

        // --- 2. Dynamic Input Composers ---
        if (uiState.isSecureEnabled) {
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openPassphraseDialog() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ChirpTheme.spacing.md, vertical = ChirpTheme.spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔒",
                            style = ChirpTheme.typography.titleMedium
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.send_secure_sealed_badge),
                                style = ChirpTheme.typography.labelMedium,
                                color = ChirpTheme.colors.primary
                            )
                            Text(
                                text = stringResource(R.string.send_secure_tap_to_edit),
                                style = ChirpTheme.typography.labelSmall,
                                color = ChirpTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                    StatusPill(
                        status = AcousticStatus.LOCKED,
                        customLabel = "AES-256"
                    )
                }
            }
        }

        when (uiState.selectedType) {
            SendPayloadType.TEXT -> {
                CdOutlinedTextField(
                    value = uiState.textValue,
                    onValueChange = { viewModel.onTextChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.send_label_text)) },
                    placeholder = { Text(stringResource(R.string.send_placeholder_text)) },
                    minLines = 4,
                    maxLines = 8,
                    isError = uiState.isTooLong || uiState.validationError != null,
                    trailingIcon = {
                        CdIconButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text ?: ""
                                val sanitized = ClipboardSanitizer.sanitizeText(clipText)
                                viewModel.onTextChanged(sanitized, debounceMs = 0L)
                            }
                        ) {
                            Text(
                                text = "📋",
                                style = ChirpTheme.typography.titleMedium,
                                color = ChirpTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            SendPayloadType.URL -> {
                CdOutlinedTextField(
                    value = uiState.urlValue,
                    onValueChange = { viewModel.onUrlChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.send_label_url)) },
                    placeholder = { Text(stringResource(R.string.send_placeholder_url)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    isError = uiState.isTooLong || uiState.validationError != null,
                    trailingIcon = {
                        CdIconButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text ?: ""
                                val sanitized = ClipboardSanitizer.sanitizeUrl(clipText)
                                viewModel.onUrlChanged(sanitized, debounceMs = 0L)
                            }
                        ) {
                            Text(
                                text = "📋",
                                style = ChirpTheme.typography.titleMedium,
                                color = ChirpTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            SendPayloadType.WIFI -> {
                WifiComposer(
                    uiState = uiState,
                    viewModel = viewModel,
                    clipboardManager = clipboardManager
                )
            }

            SendPayloadType.CONTACT -> {
                ContactComposer(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }

        // --- 3. Live ByteMeter & Validation Notices ---
        ByteMeter(
            bytesUsed = uiState.onAirBytes,
            maxBytes = 320,
            estimatedDurationSeconds = uiState.estimatedDurationSeconds,
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.isTooLong) {
            Text(
                text = stringResource(R.string.send_error_too_long),
                style = ChirpTheme.typography.bodySmall,
                color = ChirpTheme.colors.signalError
            )
        } else if (uiState.validationError != null) {
            Text(
                text = uiState.validationError!!,
                style = ChirpTheme.typography.bodySmall,
                color = ChirpTheme.colors.error
            )
        }

        if (uiState.truncatedNotice != null) {
            Text(
                text = uiState.truncatedNotice!!,
                style = ChirpTheme.typography.bodySmall,
                color = ChirpTheme.colors.warning
            )
        }

        // --- 4. Settings Strip ---
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
            ) {
                ModeSelector(
                    selectedBand = uiState.selectedBand,
                    onBandSelected = { viewModel.onBandSelected(it) }
                )

                SpeedSelector(
                    selectedSpeed = uiState.selectedSpeed,
                    onSpeedSelected = { viewModel.onSpeedSelected(it) }
                )

                // Secure Mode Switch Row (Step 46)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = stringResource(R.string.send_secure_label),
                            style = ChirpTheme.typography.bodyMedium,
                            color = ChirpTheme.colors.onSurface
                        )
                        Text(
                            text = stringResource(
                                if (uiState.isSecureEnabled) R.string.send_secure_active_subtitle
                                else R.string.send_secure_subtitle
                            ),
                            style = ChirpTheme.typography.bodySmall,
                            color = if (uiState.isSecureEnabled) ChirpTheme.colors.primary else ChirpTheme.colors.onSurfaceVariant.copy(alpha = ChirpTheme.opacity.muted)
                        )
                    }
                    CdSwitch(
                        checked = uiState.isSecureEnabled,
                        onCheckedChange = { viewModel.onSecureModeToggled(it) },
                        enabled = true
                    )
                }

                CdOutlinedButton(
                    onClick = { viewModel.setSettingsBottomSheetVisible(true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚙  ${stringResource(R.string.send_options_title)}",
                        style = ChirpTheme.typography.labelLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1.0f, fill = false))

        // --- 5. Primary "Chirp it" Action Button ---
        val estDuration = ceil(uiState.estimatedDurationSeconds).toInt()
        val buttonLabel = buildString {
            append(stringResource(R.string.send_action_chirp))
            if (estDuration > 0 && uiState.isValid) {
                append(" (")
                append(stringResource(R.string.send_duration_estimate, estDuration))
                append(")")
            }
        }

        CdButton(
            onClick = { viewModel.startTransmission(onNavigateToTransmit) },
            enabled = uiState.isValid && !uiState.isTransmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = buttonLabel,
                style = ChirpTheme.typography.labelLarge
            )
        }
    }
}
