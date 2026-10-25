package app.shoutsend.feature.send

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.R
import app.shoutsend.core.designsystem.ChirpTheme
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
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(ChirpTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        Text(
                            text = stringResource(R.string.send_coming_next_wifi_title),
                            style = ChirpTheme.typography.titleMedium,
                            color = ChirpTheme.colors.secondary
                        )
                        Text(
                            text = stringResource(R.string.send_coming_next_wifi_body),
                            style = ChirpTheme.typography.bodyMedium,
                            color = ChirpTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }

            SendPayloadType.CONTACT -> {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(ChirpTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        Text(
                            text = stringResource(R.string.send_coming_next_contact_title),
                            style = ChirpTheme.typography.titleMedium,
                            color = ChirpTheme.colors.secondary
                        )
                        Text(
                            text = stringResource(R.string.send_coming_next_contact_body),
                            style = ChirpTheme.typography.bodyMedium,
                            color = ChirpTheme.colors.onSurfaceVariant
                        )
                    }
                }
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

                // Secure Mode Switch Row (greyed out until Step 46)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text(
                            text = stringResource(R.string.send_secure_label),
                            style = ChirpTheme.typography.bodyMedium,
                            color = ChirpTheme.colors.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.send_secure_subtitle),
                            style = ChirpTheme.typography.bodySmall,
                            color = ChirpTheme.colors.onSurfaceVariant.copy(alpha = ChirpTheme.opacity.muted)
                        )
                    }
                    CdSwitch(
                        checked = false,
                        onCheckedChange = null,
                        enabled = false
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
