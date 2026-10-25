package app.shoutsend.feature.send

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.R
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.designsystem.components.CdAlertDialog
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.CdScaffold
import app.shoutsend.core.designsystem.components.CdTopAppBar
import app.shoutsend.core.designsystem.components.NeonCard
import app.shoutsend.core.designsystem.components.RadioReadout
import app.shoutsend.core.designsystem.components.ReadoutScale
import app.shoutsend.core.designsystem.components.StatusPill
import app.shoutsend.core.session.TransmitState

/**
 * Acoustic transmission monitoring screen displaying real-time modulation state.
 * Placeholder visual shell for Step 37 (replaced by full cyber radio visuals in Step 39).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransmitScreen(
    viewModel: SendViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCancelConfirmDialog by remember { mutableStateOf(false) }

    // Intercept back presses during active transmission
    BackHandler(enabled = uiState.isTransmitting) {
        showCancelConfirmDialog = true
    }

    if (showCancelConfirmDialog) {
        CdAlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = {
                Text(
                    text = "Cancel Transmission?",
                    style = ChirpTheme.typography.titleMedium,
                    color = ChirpTheme.colors.onSurface
                )
            },
            text = {
                Text(
                    text = "Stopping will interrupt the active acoustic sound wave. The receiver may not restore the payload.",
                    style = ChirpTheme.typography.bodyMedium,
                    color = ChirpTheme.colors.onSurfaceVariant
                )
            },
            confirmButton = {
                CdButton(
                    onClick = {
                        showCancelConfirmDialog = false
                        viewModel.cancelTransmission()
                        onNavigateBack()
                    }
                ) {
                    Text("Stop Transmission")
                }
            },
            dismissButton = {
                CdOutlinedButton(
                    onClick = { showCancelConfirmDialog = false }
                ) {
                    Text("Keep Playing")
                }
            }
        )
    }

    CdScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CdTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.transmit_screen_title),
                        style = ChirpTheme.typography.titleMedium,
                        color = ChirpTheme.colors.primary
                    )
                },
                navigationIcon = {
                    CdIconButton(
                        onClick = {
                            if (uiState.isTransmitting) {
                                showCancelConfirmDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text(
                            text = "←",
                            style = ChirpTheme.typography.titleMedium,
                            color = ChirpTheme.colors.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(ChirpTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState.transmitState) {
                is TransmitState.Idle -> {
                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(ChirpTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
                        ) {
                            Text(
                                text = "Ready to transmit",
                                style = ChirpTheme.typography.titleMedium,
                                color = ChirpTheme.colors.primary
                            )
                        }
                    }
                }

                is TransmitState.Preparing -> {
                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(ChirpTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
                        ) {
                            StatusPill(status = AcousticStatus.LOCKED)
                            Text(
                                text = stringResource(R.string.transmit_screen_preparing),
                                style = ChirpTheme.typography.bodyLarge,
                                color = ChirpTheme.colors.onSurface
                            )
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = ChirpTheme.colors.primary,
                                trackColor = ChirpTheme.colors.surfaceContainerHighest
                            )
                        }
                    }
                }

                is TransmitState.Transmitting -> {
                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(ChirpTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(status = AcousticStatus.LOCKED)
                                Text(
                                    text = "Frame ${state.currentFrame} / ${state.totalFrames}",
                                    style = ChirpTheme.typography.radioReadoutSmall,
                                    color = ChirpTheme.colors.primary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = ChirpTheme.colors.primary,
                                trackColor = ChirpTheme.colors.surfaceContainerHighest
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                RadioReadout(
                                    label = "Elapsed",
                                    value = "%.1f s".format(state.elapsedSeconds),
                                    scale = ReadoutScale.MEDIUM
                                )
                                RadioReadout(
                                    label = "Remaining",
                                    value = "%.1f s".format(state.remainingSeconds),
                                    scale = ReadoutScale.MEDIUM
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1.0f))

                    CdOutlinedButton(
                        onClick = { showCancelConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.transmit_btn_cancel))
                    }
                }

                is TransmitState.Completed -> {
                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(ChirpTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
                        ) {
                            StatusPill(status = AcousticStatus.SUCCESS)
                            Text(
                                text = stringResource(R.string.transmit_screen_completed),
                                style = ChirpTheme.typography.titleLarge,
                                color = ChirpTheme.colors.success
                            )
                            Text(
                                text = "Acoustic audio wave played in full (${state.durationMs} ms).",
                                style = ChirpTheme.typography.bodyMedium,
                                color = ChirpTheme.colors.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1.0f))

                    CdButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.transmit_btn_done))
                    }
                }

                is TransmitState.Cancelled -> {
                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(ChirpTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
                        ) {
                            StatusPill(status = AcousticStatus.IDLE)
                            Text(
                                text = stringResource(R.string.transmit_screen_cancelled),
                                style = ChirpTheme.typography.titleLarge,
                                color = ChirpTheme.colors.warning
                            )
                            Text(
                                text = "Transmission was halted before completion.",
                                style = ChirpTheme.typography.bodyMedium,
                                color = ChirpTheme.colors.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1.0f))

                    CdButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.transmit_btn_back))
                    }
                }

                is TransmitState.Failed -> {
                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(ChirpTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
                        ) {
                            StatusPill(status = AcousticStatus.ERROR)
                            Text(
                                text = stringResource(R.string.transmit_screen_failed),
                                style = ChirpTheme.typography.titleLarge,
                                color = ChirpTheme.colors.signalError
                            )
                            Text(
                                text = state.reason,
                                style = ChirpTheme.typography.bodyMedium,
                                color = ChirpTheme.colors.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1.0f))

                    CdButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.transmit_btn_back))
                    }
                }
            }
        }
    }
}
