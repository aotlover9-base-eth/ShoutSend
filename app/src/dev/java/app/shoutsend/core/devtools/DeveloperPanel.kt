package app.shoutsend.core.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.shoutsend.core.designsystem.ChirpTheme
import app.shoutsend.core.designsystem.DefaultTokens
import app.shoutsend.core.designsystem.components.CdButton
import androidx.compose.material3.Card
import app.shoutsend.core.designsystem.components.CdIconButton
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.CdOutlinedTextField
import app.shoutsend.core.designsystem.components.CdTextButton
import app.shoutsend.core.designsystem.components.CdTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperPanel(
    viewModel: DevPanelViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ChirpTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
        ) {
            // Header with Title, Tabs, and Escape Hatch Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Design System DevTools",
                        style = ChirpTheme.typography.titleMedium,
                        color = ChirpTheme.colors.onSurface
                    )
                    Text(
                        text = if (uiState.hasOverrides) "Custom overrides active" else "Factory defaults",
                        style = ChirpTheme.typography.bodySmall,
                        color = if (uiState.hasOverrides) ChirpTheme.colors.tertiary else ChirpTheme.colors.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)) {
                    // Escape hatch reset button (fixed default styling to guarantee readability)
                    Box(
                        modifier = Modifier
                            .background(
                                color = DefaultTokens.CyberRadioDark.errorContainer,
                                shape = ChirpTheme.shapes.button
                            )
                            .clickable { showResetDialog = true }
                            .padding(horizontal = ChirpTheme.spacing.sm, vertical = ChirpTheme.spacing.xs)
                    ) {
                        Text(
                            text = "Reset All",
                            style = ChirpTheme.typography.labelMedium,
                            color = DefaultTokens.CyberRadioDark.onErrorContainer
                        )
                    }

                    CdIconButton(onClick = onDismiss) {
                        Text("✕", style = ChirpTheme.typography.titleMedium)
                    }
                }
            }

            // Tab Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                FilterChip(
                    selected = uiState.activeTab == DevPanelTab.COLORS,
                    onClick = { viewModel.selectTab(DevPanelTab.COLORS) },
                    label = { Text("Colors") }
                )
                FilterChip(
                    selected = uiState.activeTab == DevPanelTab.SHAPES,
                    onClick = { viewModel.selectTab(DevPanelTab.SHAPES) },
                    label = { Text("Shapes & Radii") }
                )
                FilterChip(
                    selected = uiState.activeTab == DevPanelTab.DSP_LAB,
                    onClick = { viewModel.selectTab(DevPanelTab.DSP_LAB) },
                    label = { Text("DSP Lab") }
                )
            }

            HorizontalDivider()

            // Active Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.activeTab) {
                    DevPanelTab.COLORS -> ColorsTabContent(
                        colorGroups = uiState.colorGroups,
                        onColorChanged = viewModel::onColorChanged,
                        onHexChanged = viewModel::onHexChanged,
                        onResetColor = viewModel::resetColor
                    )
                    DevPanelTab.SHAPES -> ShapesTabContent(
                        radiusItems = uiState.radiusItems,
                        onRadiusChanged = viewModel::onRadiusChanged,
                        onResetRadius = viewModel::resetRadius
                    )
                    DevPanelTab.DSP_LAB -> DspLabContent(
                        viewModel = viewModel.dspLabViewModel
                    )
                }
            }

            HorizontalDivider()

            // Footer Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ChirpTheme.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CdOutlinedButton(
                    onClick = {
                        val json = viewModel.exportJson()
                        clipboardManager.setText(AnnotatedString(json))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export JSON")
                }

                CdButton(
                    onClick = {
                        val clip = clipboardManager.getText()?.text.orEmpty()
                        importText = clip
                        importError = null
                        showImportDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Import JSON")
                }
            }
        }
    }

    // Reset All Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Design Tokens?") },
            text = { Text("This will clear all custom color and radius overrides and restore factory Cyber Radio tokens.") },
            confirmButton = {
                CdButton(onClick = {
                    viewModel.resetAll()
                    showResetDialog = false
                }) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                CdTextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Token Overrides") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)) {
                    Text("Paste a valid TokenOverrides JSON string:")
                    CdOutlinedTextField(
                        value = importText,
                        onValueChange = {
                            importText = it
                            importError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                    if (importError != null) {
                        Text(
                            text = importError!!,
                            color = ChirpTheme.colors.error,
                            style = ChirpTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                CdButton(onClick = {
                    viewModel.importJson(importText) { result ->
                        if (result.isSuccess) {
                            showImportDialog = false
                        } else {
                            importError = result.exceptionOrNull()?.message ?: "Invalid JSON"
                        }
                    }
                }) {
                    Text("Apply Import")
                }
            },
            dismissButton = {
                CdTextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ColorsTabContent(
    colorGroups: Map<String, List<ColorRoleItem>>,
    onColorChanged: (String, Color) -> Unit,
    onHexChanged: (String, String) -> Boolean,
    onResetColor: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        colorGroups.forEach { (groupName, roles) ->
            item {
                Text(
                    text = groupName,
                    style = ChirpTheme.typography.titleSmall,
                    color = ChirpTheme.colors.primary,
                    modifier = Modifier.padding(top = ChirpTheme.spacing.xs)
                )
            }

            items(roles, key = { it.role }) { item ->
                ColorRoleRow(
                    item = item,
                    onColorChanged = { onColorChanged(item.role, it) },
                    onHexChanged = { hex -> onHexChanged(item.role, hex) },
                    onReset = { onResetColor(item.role) }
                )
            }
        }
    }
}

@Composable
private fun ColorRoleRow(
    item: ColorRoleItem,
    onColorChanged: (Color) -> Unit,
    onHexChanged: (String) -> Boolean,
    onReset: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val hsv = remember(item.currentColor) { ColorUtils.rgbToHsv(item.currentColor) }
    var hue by remember(item.currentColor) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(item.currentColor) { mutableFloatStateOf(hsv[1]) }
    var brightness by remember(item.currentColor) { mutableFloatStateOf(hsv[2]) }
    var hexText by remember(item.currentColor) { mutableStateOf(ColorUtils.colorToHex(item.currentColor)) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ChirpTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(ChirpTheme.sizes.iconLg)
                            .background(item.currentColor, CircleShape)
                            .border(ChirpTheme.strokes.thin, ChirpTheme.colors.outline, CircleShape)
                    )

                    Column {
                        Text(
                            text = item.role,
                            style = ChirpTheme.typography.titleSmall,
                            color = ChirpTheme.colors.onSurface
                        )
                        Text(
                            text = hexText,
                            style = ChirpTheme.typography.radioReadoutSmall,
                            color = ChirpTheme.colors.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.isModified) {
                        Text(
                            text = "Modified",
                            style = ChirpTheme.typography.labelSmall,
                            color = ChirpTheme.colors.tertiary
                        )
                        CdIconButton(onClick = onReset) {
                            Text("↺", style = ChirpTheme.typography.titleSmall)
                        }
                    }
                    Text(
                        text = if (expanded) "▲" else "▼",
                        style = ChirpTheme.typography.bodySmall,
                        color = ChirpTheme.colors.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = ChirpTheme.spacing.xs))

                // Hex input field
                CdOutlinedTextField(
                    value = hexText,
                    onValueChange = {
                        hexText = it
                        onHexChanged(it)
                    },
                    label = { Text("Hex Color") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Hue slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Hue", style = ChirpTheme.typography.bodySmall)
                        Text("${hue.toInt()}°", style = ChirpTheme.typography.radioReadoutSmall)
                    }
                    Slider(
                        value = hue,
                        onValueChange = {
                            hue = it
                            val newColor = ColorUtils.hsvToColor(hue, saturation, brightness)
                            hexText = ColorUtils.colorToHex(newColor)
                            onColorChanged(newColor)
                        },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            thumbColor = item.currentColor,
                            activeTrackColor = ChirpTheme.colors.primary
                        )
                    )
                }

                // Saturation slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Saturation", style = ChirpTheme.typography.bodySmall)
                        Text("${(saturation * 100f).toInt()}%", style = ChirpTheme.typography.radioReadoutSmall)
                    }
                    Slider(
                        value = saturation,
                        onValueChange = {
                            saturation = it
                            val newColor = ColorUtils.hsvToColor(hue, saturation, brightness)
                            hexText = ColorUtils.colorToHex(newColor)
                            onColorChanged(newColor)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = item.currentColor,
                            activeTrackColor = ChirpTheme.colors.primary
                        )
                    )
                }

                // Brightness slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Brightness", style = ChirpTheme.typography.bodySmall)
                        Text("${(brightness * 100f).toInt()}%", style = ChirpTheme.typography.radioReadoutSmall)
                    }
                    Slider(
                        value = brightness,
                        onValueChange = {
                            brightness = it
                            val newColor = ColorUtils.hsvToColor(hue, saturation, brightness)
                            hexText = ColorUtils.colorToHex(newColor)
                            onColorChanged(newColor)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = item.currentColor,
                            activeTrackColor = ChirpTheme.colors.primary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ShapesTabContent(
    radiusItems: List<RadiusTokenItem>,
    onRadiusChanged: (String, Float) -> Unit,
    onResetRadius: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ChirpTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.md)
    ) {
        radiusItems.forEach { item ->
            var currentRadius by remember(item.currentRadiusDp) { mutableFloatStateOf(item.currentRadiusDp) }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ChirpTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = ChirpTheme.typography.titleSmall,
                            color = ChirpTheme.colors.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentRadius.toInt()} dp",
                                style = ChirpTheme.typography.radioReadoutMedium,
                                color = ChirpTheme.colors.primary
                            )
                            if (currentRadius != item.defaultRadiusDp) {
                                CdIconButton(onClick = {
                                    currentRadius = item.defaultRadiusDp
                                    onResetRadius(item.name)
                                }) {
                                    Text("↺", style = ChirpTheme.typography.titleSmall)
                                }
                            }
                        }
                    }

                    Slider(
                        value = currentRadius,
                        onValueChange = {
                            currentRadius = it
                            onRadiusChanged(item.name, it)
                        },
                        valueRange = 0f..36f
                    )
                }
            }
        }

        HorizontalDivider()

        // Live preview of components with the active shape tokens
        Text(
            text = "Live Component Previews",
            style = ChirpTheme.typography.titleSmall,
            color = ChirpTheme.colors.primary
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ChirpTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ChirpTheme.spacing.sm)
            ) {
                CdButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CdButton (ShapeSmall)")
                }

                CdTextField(
                    value = "TextField preview",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
