package app.shoutsend.feature.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.shoutsend.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import app.shoutsend.core.data.vault.model.Direction
import app.shoutsend.core.data.vault.model.VaultItem
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.CdSnackbarHost
import app.shoutsend.core.designsystem.components.ShoutCard
import app.shoutsend.core.designsystem.components.StatusPill
import app.shoutsend.modem.payload.Payload
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying encrypted acoustic transmission history (Step 43).
 *
 * Conforms to UI Gate specifications:
 * - Direction filters: All, Starred, Sent, Received.
 * - Payload type filters: All, Text, URLs, Wi-Fi, Contacts.
 * - Swipe-to-delete with one-step Undo snackbar.
 * - Clear all records behind an explicit confirmation dialog.
 * - Passwords stay strictly masked in the list view (`••••••••`).
 * - TalkBack content descriptions and minimum 48dp touch targets on all interactive elements.
 * - Design tokens only (Rule 6, Rule 7).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToLinkTest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = ChirpTheme.colors
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            } else {
                viewModel.clearSnackbar()
            }
        }
    }

    // Clear All Confirmation Dialog
    if (uiState.showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearAllDialog() },
            title = {
                Text(
                    text = stringResource(R.string.vault_clear_dialog_title),
                    style = ChirpTheme.typography.titleMedium,
                    color = colors.error
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.vault_clear_dialog_message),
                    style = ChirpTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            },
            confirmButton = {
                CdButton(
                    onClick = { viewModel.confirmClearAll() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.signalError,
                        contentColor = colors.onError
                    )
                ) {
                    Text(stringResource(R.string.vault_clear_dialog_confirm))
                }
            },
            dismissButton = {
                CdOutlinedButton(onClick = { viewModel.dismissClearAllDialog() }) {
                    Text(stringResource(R.string.vault_clear_dialog_cancel))
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ChirpTheme.spacing.md, vertical = ChirpTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
        ) {
            // Header: Title & Clear All Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.vault_screen_title),
                        style = ChirpTheme.typography.titleLarge,
                        color = colors.primary
                    )
                    Text(
                        text = "${uiState.totalCount} stored · ${uiState.starredCount} starred",
                        style = ChirpTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = colors.onSurfaceVariant
                    )
                }

                if (uiState.totalCount > 0) {
                    CdOutlinedButton(
                        onClick = { viewModel.requestClearAll() },
                        modifier = Modifier.defaultMinSize(minHeight = ChirpTheme.sizes.touchTargetMin)
                    ) {
                        Text(stringResource(R.string.vault_clear_all))
                    }
                }
            }

            // Filter Strip 1: Direction (All, Starred, Sent, Received)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
            ) {
                item {
                    FilterChip(
                        selected = uiState.directionFilter == VaultFilterDirection.ALL,
                        onClick = { viewModel.setDirectionFilter(VaultFilterDirection.ALL) },
                        label = { Text(stringResource(R.string.vault_filter_all)) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.directionFilter == VaultFilterDirection.STARRED,
                        onClick = { viewModel.setDirectionFilter(VaultFilterDirection.STARRED) },
                        label = { Text(stringResource(R.string.vault_filter_starred)) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.directionFilter == VaultFilterDirection.SENT,
                        onClick = { viewModel.setDirectionFilter(VaultFilterDirection.SENT) },
                        label = { Text(stringResource(R.string.vault_filter_sent)) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.directionFilter == VaultFilterDirection.RECEIVED,
                        onClick = { viewModel.setDirectionFilter(VaultFilterDirection.RECEIVED) },
                        label = { Text(stringResource(R.string.vault_filter_received)) }
                    )
                }
            }

            // Filter Strip 2: Payload Types (All Types, Text, URLs, Wi-Fi, Contacts)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
            ) {
                item {
                    FilterChip(
                        selected = uiState.typeFilter == VaultFilterType.ALL,
                        onClick = { viewModel.setTypeFilter(VaultFilterType.ALL) },
                        label = { Text(stringResource(R.string.vault_filter_type_all)) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.typeFilter == VaultFilterType.TEXT,
                        onClick = { viewModel.setTypeFilter(VaultFilterType.TEXT) },
                        label = { Text(stringResource(R.string.vault_filter_type_text)) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.typeFilter == VaultFilterType.URL,
                        onClick = { viewModel.setTypeFilter(VaultFilterType.URL) },
                        label = { Text(stringResource(R.string.vault_filter_type_url)) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.typeFilter == VaultFilterType.WIFI,
                        onClick = { viewModel.setTypeFilter(VaultFilterType.WIFI) },
                        label = { Text(stringResource(R.string.vault_filter_type_wifi)) }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.typeFilter == VaultFilterType.CONTACT,
                        onClick = { viewModel.setTypeFilter(VaultFilterType.CONTACT) },
                        label = { Text(stringResource(R.string.vault_filter_type_contact)) }
                    )
                }
            }

            // Message List or Empty State
            if (uiState.filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = ChirpTheme.spacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    ShoutCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(ChirpTheme.spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                        ) {
                            Text(
                                text = if (uiState.totalCount == 0) {
                                    stringResource(R.string.vault_empty_title)
                                } else {
                                    stringResource(R.string.vault_empty_filtered)
                                },
                                style = ChirpTheme.typography.titleMedium,
                                color = colors.onSurface
                            )
                            Text(
                                text = stringResource(R.string.vault_empty_subtitle),
                                style = ChirpTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                ) {
                    items(
                        items = uiState.filteredItems,
                        key = { it.id }
                    ) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissVal ->
                                if (dismissVal == SwipeToDismissBoxValue.EndToStart || dismissVal == SwipeToDismissBoxValue.StartToEnd) {
                                    viewModel.deleteItem(item)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(ChirpTheme.shapes.card)
                                        .background(colors.error.copy(alpha = 0.25f))
                                        .padding(horizontal = ChirpTheme.spacing.md),
                                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                        Alignment.CenterStart
                                    } else {
                                        Alignment.CenterEnd
                                    }
                                ) {
                                    Text(
                                        text = stringResource(R.string.vault_detail_delete),
                                        style = ChirpTheme.typography.labelMedium,
                                        color = colors.error
                                    )
                                }
                            },
                            content = {
                                VaultItemRow(
                                    item = item,
                                    onClick = { onNavigateToDetail(item.id) },
                                    onToggleStar = { viewModel.toggleStar(item) }
                                )
                            }
                        )
                    }
                }
            }
        }

        // Bottom Snackbar Host
        CdSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(ChirpTheme.spacing.md)
        )
    }
}

/**
 * Individual row card in the Transmission Vault list.
 */
@Composable
private fun VaultItemRow(
    item: VaultItem,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ChirpTheme.colors
    val dateText = remember(item.timestamp) {
        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.US)
        sdf.format(Date(item.timestamp))
    }

    ShoutCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(ChirpTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xxs)
        ) {
            // Header Row: Direction Chip, Timestamp, Star Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSent = item.direction == Direction.SENT
                    Text(
                        text = if (isSent) "↑ SENT" else "↓ RCVD",
                        style = ChirpTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSent) colors.secondary else colors.tertiary
                    )

                    Text(
                        text = dateText,
                        style = ChirpTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )

                    if (item.isSecure) {
                        Text(
                            text = "🔒",
                            style = ChirpTheme.typography.labelSmall
                        )
                    }
                }

                val starDesc = stringResource(R.string.vault_action_star_desc)
                CdIconButton(
                    onClick = onToggleStar,
                    modifier = Modifier
                        .size(ChirpTheme.sizes.touchTargetMin)
                        .semantics { contentDescription = starDesc }
                ) {
                    Text(
                        text = if (item.isStarred) "★" else "☆",
                        style = ChirpTheme.typography.titleMedium,
                        color = if (item.isStarred) colors.warning else colors.onSurfaceVariant
                    )
                }
            }

            // Payload Content Preview
            val payload = item.payload
            when {
                item.isUnreadable -> {
                    Text(
                        text = "Encrypted message (unreadable)",
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.error
                    )
                }

                payload is Payload.Text -> {
                    Text(
                        text = payload.value,
                        style = ChirpTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface
                    )
                }

                payload is Payload.Url -> {
                    Text(
                        text = payload.value,
                        style = ChirpTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.primary
                    )
                }

                payload is Payload.WiFi -> {
                    Text(
                        text = "Wi-Fi: ${payload.ssid} · ••••••••",
                        style = ChirpTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface
                    )
                }

                payload is Payload.Contact -> {
                    val phoneSummary = payload.phones.firstOrNull()?.let { " · $it" }.orEmpty()
                    Text(
                        text = "Contact: ${payload.displayName}$phoneSummary",
                        style = ChirpTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface
                    )
                }

                else -> {
                    Text(
                        text = "No content",
                        style = ChirpTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            // Footer Diagnostics Snippet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val durationSec = String.format(Locale.US, "%.1fs", item.durationMs / 1000.0)
                val snrText = item.snrDb?.let { " · ${String.format(Locale.US, "%.1f", it)} dB" }.orEmpty()
                Text(
                    text = "${item.profileSummary} · $durationSec$snrText",
                    style = ChirpTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}
