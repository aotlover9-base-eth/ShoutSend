package app.shoutsend.feature.send

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.FilterChip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import app.shoutsend.R
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.CdOutlinedTextField
import app.shoutsend.core.designsystem.components.CdSwitch
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.modem.payload.WifiSecurity

/**
 * Wi-Fi network credential composer form (Step 38).
 * Features SSID input, security mode selector, masked password with paste,
 * hidden network switch, and acoustic exposure privacy warning card.
 */
@Composable
fun WifiComposer(
    uiState: SendUiState,
    viewModel: SendViewModel,
    clipboardManager: ClipboardManager,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
    ) {
        // SSID field
        CdOutlinedTextField(
            value = uiState.wifiSsid,
            onValueChange = { viewModel.onWifiSsidChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.send_label_wifi_ssid)) },
            placeholder = { Text(stringResource(R.string.send_placeholder_wifi_ssid)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        // Security selector chips
        Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)) {
            Text(
                text = stringResource(R.string.send_label_wifi_security),
                style = ChirpTheme.typography.labelMedium,
                color = ChirpTheme.colors.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                WifiSecurity.entries.forEach { security ->
                    val labelText = when (security) {
                        WifiSecurity.WPA2 -> stringResource(R.string.send_wifi_sec_wpa2)
                        WifiSecurity.WPA3 -> stringResource(R.string.send_wifi_sec_wpa3)
                        WifiSecurity.OPEN -> stringResource(R.string.send_wifi_sec_open)
                    }
                    FilterChip(
                        selected = uiState.wifiSecurity == security,
                        onClick = { viewModel.onWifiSecurityChanged(security) },
                        label = { Text(labelText) }
                    )
                }
            }
        }

        // Password field (hidden entirely when security is Open)
        if (uiState.wifiSecurity != WifiSecurity.OPEN) {
            CdOutlinedTextField(
                value = uiState.wifiPassword,
                onValueChange = { viewModel.onWifiPasswordChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { password() },
                label = { Text(stringResource(R.string.send_label_wifi_password)) },
                placeholder = { Text(stringResource(R.string.send_placeholder_wifi_password)) },
                singleLine = true,
                visualTransformation = if (uiState.wifiPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CdIconButton(
                            onClick = { viewModel.toggleWifiPasswordVisibility() }
                        ) {
                            Text(
                                text = if (uiState.wifiPasswordVisible) "👁" else "🙈",
                                style = ChirpTheme.typography.titleMedium,
                                color = ChirpTheme.colors.onSurfaceVariant
                            )
                        }
                        CdIconButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text ?: ""
                                val sanitized = ClipboardSanitizer.sanitizeText(clipText).trim()
                                viewModel.onWifiPasswordChanged(sanitized, debounceMs = 0L)
                            }
                        ) {
                            Text(
                                text = "📋",
                                style = ChirpTheme.typography.titleMedium,
                                color = ChirpTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }

        // Hidden network toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.send_label_wifi_hidden),
                style = ChirpTheme.typography.bodyLarge,
                color = ChirpTheme.colors.onSurface
            )
            CdSwitch(
                checked = uiState.wifiIsHidden,
                onCheckedChange = { viewModel.onWifiIsHiddenChanged(it) }
            )
        }

        // Wi-Fi privacy cue warning card (hidden only when security is Open)
        if (uiState.wifiSecurity != WifiSecurity.OPEN) {
            NeonCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(ChirpTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.send_wifi_warning_title),
                        style = ChirpTheme.typography.titleSmall,
                        color = ChirpTheme.colors.warning
                    )
                    Text(
                        text = stringResource(R.string.send_wifi_warning_body),
                        style = ChirpTheme.typography.bodyMedium,
                        color = ChirpTheme.colors.onSurfaceVariant
                    )
                    if (!uiState.isSecureEnabled) {
                        CdButton(
                            onClick = { viewModel.enableSecureMode() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.send_wifi_warning_action))
                        }
                    }
                }
            }
        }
    }
}
