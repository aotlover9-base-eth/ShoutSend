package app.shoutsend.feature.send

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.shoutsend.R
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdCheckbox
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.CdOutlinedTextField
import app.shoutsend.core.designsystem.components.CdTextButton
import app.shoutsend.core.designsystem.components.ShoutCard

/**
 * Contact card composer form (Step 38).
 * Supports system contact picker (ActivityResultContracts.PickContact) without READ_CONTACTS,
 * and manual entry mode.
 * Displays "this is exactly what will be sent" review card with individual checkboxes
 * to include/exclude phone numbers and email. Photo is never transmitted (Rule 24).
 */
@Composable
fun ContactComposer(
    uiState: SendUiState,
    viewModel: SendViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val pickedData = ContactPickerReader.read(context, uri)
            viewModel.applyPickedContact(pickedData)
        }
    }

    val hasContactData = uiState.contactName.isNotBlank() ||
        uiState.contactPhone1.isNotBlank() ||
        uiState.contactPhone2.isNotBlank() ||
        uiState.contactEmail.isNotBlank()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
    ) {
        if (!uiState.isContactManualMode && !hasContactData) {
            // Initial State: Pick Contact or Manual Entry Prompt
            ShoutCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(ChirpTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.send_type_contact),
                        style = ChirpTheme.typography.titleMedium,
                        color = ChirpTheme.colors.primary
                    )
                    Text(
                        text = stringResource(R.string.send_contact_no_photo_notice),
                        style = ChirpTheme.typography.bodySmall,
                        color = ChirpTheme.colors.onSurfaceVariant
                    )
                    CdButton(
                        onClick = { contactPickerLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.send_contact_btn_pick))
                    }
                    CdTextButton(
                        onClick = { viewModel.setContactManualMode(true) }
                    ) {
                        Text(stringResource(R.string.send_contact_btn_manual))
                    }
                }
            }
        } else {
            // Review & Editing Card ("This is exactly what will be sent")
            ShoutCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(ChirpTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
                ) {
                    Text(
                        text = stringResource(R.string.send_contact_review_title),
                        style = ChirpTheme.typography.titleSmall,
                        color = ChirpTheme.colors.secondary
                    )
                    Text(
                        text = stringResource(R.string.send_contact_no_photo_notice),
                        style = ChirpTheme.typography.bodySmall,
                        color = ChirpTheme.colors.onSurfaceVariant
                    )

                    // Contact Name
                    CdOutlinedTextField(
                        value = uiState.contactName,
                        onValueChange = { viewModel.onContactNameChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.send_contact_label_name)) },
                        placeholder = { Text(stringResource(R.string.send_contact_placeholder_name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Primary Phone with Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        CdCheckbox(
                            checked = uiState.contactIncludePhone1,
                            onCheckedChange = { viewModel.toggleContactIncludePhone1() }
                        )
                        CdOutlinedTextField(
                            value = uiState.contactPhone1,
                            onValueChange = { viewModel.onContactPhone1Changed(it) },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.send_contact_label_phone1)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    // Secondary Phone with Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        CdCheckbox(
                            checked = uiState.contactIncludePhone2,
                            onCheckedChange = { viewModel.toggleContactIncludePhone2() }
                        )
                        CdOutlinedTextField(
                            value = uiState.contactPhone2,
                            onValueChange = { viewModel.onContactPhone2Changed(it) },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.send_contact_label_phone2)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    // Email with Checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                    ) {
                        CdCheckbox(
                            checked = uiState.contactIncludeEmail,
                            onCheckedChange = { viewModel.toggleContactIncludeEmail() }
                        )
                        CdOutlinedTextField(
                            value = uiState.contactEmail,
                            onValueChange = { viewModel.onContactEmailChanged(it) },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.send_contact_label_email)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done
                            )
                        )
                    }

                    // Optional OEM permission warning/notice
                    if (uiState.contactNotice != null) {
                        Text(
                            text = uiState.contactNotice,
                            style = ChirpTheme.typography.bodySmall,
                            color = ChirpTheme.colors.warning
                        )
                    }

                    // Mode Switch / Repick actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isContactManualMode) {
                            CdOutlinedButton(
                                onClick = { contactPickerLauncher.launch(null) }
                            ) {
                                Text(stringResource(R.string.send_contact_btn_pick))
                            }
                            CdTextButton(
                                onClick = { viewModel.clearSensitiveData() }
                            ) {
                                Text(stringResource(R.string.send_contact_clear))
                            }
                        } else {
                            CdOutlinedButton(
                                onClick = { contactPickerLauncher.launch(null) }
                            ) {
                                Text(stringResource(R.string.send_contact_btn_repick))
                            }
                            CdTextButton(
                                onClick = { viewModel.setContactManualMode(true) }
                            ) {
                                Text(stringResource(R.string.send_contact_switch_to_manual))
                            }
                        }
                    }
                }
            }
        }
    }
}
