package app.shoutsend.core.devtools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.shoutsend.core.designsystem.DefaultTokens
import app.shoutsend.core.designsystem.ShoutIcons
import app.shoutsend.core.designsystem.ShoutSendTheme
import app.shoutsend.core.designsystem.ShoutShapes
import app.shoutsend.core.designsystem.ThemeMode
import app.shoutsend.core.designsystem.components.CdButton
import app.shoutsend.core.designsystem.components.CdOutlinedButton
import app.shoutsend.core.designsystem.components.CdTextButton
import app.shoutsend.core.designsystem.components.MorphingShape
import app.shoutsend.core.designsystem.components.ShoutButton
import app.shoutsend.core.designsystem.components.ShoutCard
import app.shoutsend.core.designsystem.components.ShoutDialog
import app.shoutsend.core.designsystem.components.ShoutFilterChip
import app.shoutsend.core.designsystem.components.ShoutIconButton
import app.shoutsend.core.designsystem.components.ShoutSwitch
import app.shoutsend.core.designsystem.components.ShoutTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperPanel(
    viewModel: DevPanelViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ShoutSendTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)
        ) {
            // Header with Title, Status, Escape Hatch Reset, and Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Design System DevTools",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (uiState.hasOverrides) "Custom overrides active" else "Factory defaults",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.hasOverrides) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = ShoutSendTheme.shapes.button
                            )
                            .clickable { showResetDialog = true }
                            .padding(horizontal = ShoutSendTheme.spacing.sm, vertical = ShoutSendTheme.spacing.xs)
                    ) {
                        Text(
                            text = "Reset All",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    ShoutIconButton(
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = ShoutIcons.Close,
                            contentDescription = "Close DevTools",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Tab Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)
            ) {
                ShoutFilterChip(
                    selected = uiState.activeTab == DevPanelTab.THEME_AND_COLORS,
                    onClick = { viewModel.selectTab(DevPanelTab.THEME_AND_COLORS) },
                    label = "Theme & Colors"
                )
                ShoutFilterChip(
                    selected = uiState.activeTab == DevPanelTab.SHAPES_AND_HERO,
                    onClick = { viewModel.selectTab(DevPanelTab.SHAPES_AND_HERO) },
                    label = "Shapes & Hero"
                )
                ShoutFilterChip(
                    selected = uiState.activeTab == DevPanelTab.DSP_LAB,
                    onClick = { viewModel.selectTab(DevPanelTab.DSP_LAB) },
                    label = "DSP Lab"
                )
            }

            HorizontalDivider()

            // Active Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (uiState.activeTab) {
                    DevPanelTab.THEME_AND_COLORS -> ThemeAndColorsTabContent(
                        uiState = uiState,
                        onDynamicColorChanged = viewModel::onDynamicColorChanged,
                        onThemeModeChanged = viewModel::onThemeModeChanged,
                        onSeedColorChanged = viewModel::onSeedColorChanged,
                        onContrastChanged = viewModel::onContrastChanged
                    )
                    DevPanelTab.SHAPES_AND_HERO -> ShapesAndHeroTabContent(
                        uiState = uiState,
                        onRadiusChanged = viewModel::onRadiusChanged,
                        onResetRadius = viewModel::resetRadius,
                        onHeroShapeChanged = viewModel::onHeroShapeChanged
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
                    .padding(vertical = ShoutSendTheme.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm),
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
        ShoutDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Design Tokens?") },
            text = { Text("This will clear all custom color, contrast, shape, and hero overrides and restore factory defaults.") },
            confirmButton = {
                ShoutButton(onClick = {
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
        ShoutDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Token Overrides") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)) {
                    Text(
                        text = "Paste a valid TokenOverrides JSON string:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    ShoutTextField(
                        value = importText,
                        onValueChange = {
                            importText = it
                            importError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                    if (importError != null) {
                        Text(
                            text = importError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                ShoutButton(onClick = {
                    coroutineScope.launch {
                        val result = viewModel.importJson(importText)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeAndColorsTabContent(
    uiState: DevPanelUiState,
    onDynamicColorChanged: (Boolean) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onSeedColorChanged: (Long) -> Unit,
    onContrastChanged: (Float) -> Unit
) {
    val currentColor = remember(uiState.seedColor) { Color(uiState.seedColor) }
    val hsv = remember(currentColor) { ColorUtils.rgbToHsv(currentColor) }
    var hue by remember(currentColor) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(currentColor) { mutableFloatStateOf(hsv[1]) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Dynamic Color Toggle
        item {
            ShoutCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ShoutSendTheme.spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dynamic Color",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Extract palette from Android wallpaper (API 31+)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ShoutSwitch(
                        checked = uiState.dynamicColor,
                        onCheckedChange = onDynamicColorChanged
                    )
                }
            }
        }

        // Theme Mode Selector
        item {
            ShoutCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ShoutSendTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)
                ) {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xs)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            ShoutFilterChip(
                                selected = uiState.themeMode == mode,
                                onClick = { onThemeModeChanged(mode) },
                                label = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Seed Color Palette (when dynamic color is disabled)
        item {
            ShoutCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ShoutSendTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seed Palette",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(ShoutSendTheme.sizes.iconLg)
                                .clip(CircleShape)
                                .background(currentColor)
                                .border(
                                    width = ShoutSendTheme.strokes.thin,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                        )
                    }

                    Text(
                        text = "Preset Seeds",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xs)
                    ) {
                        DefaultTokens.SeedPresets.forEach { (name, colorLong) ->
                            val isSelected = uiState.seedColor == colorLong
                            ShoutFilterChip(
                                selected = isSelected,
                                onClick = { onSeedColorChanged(colorLong) },
                                label = name
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(ShoutSendTheme.spacing.xs))

                    // Hue Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Hue", style = MaterialTheme.typography.bodySmall)
                            Text("${hue.toInt()}°", style = MaterialTheme.typography.labelSmall)
                        }
                        Slider(
                            value = hue,
                            onValueChange = {
                                hue = it
                                val newColor = ColorUtils.hsvToColor(hue, saturation, 0.7f)
                                onSeedColorChanged(newColor.value.toLong())
                            },
                            valueRange = 0f..360f,
                            colors = SliderDefaults.colors(
                                thumbColor = currentColor,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Chroma / Saturation Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Chroma / Saturation", style = MaterialTheme.typography.bodySmall)
                            Text("${(saturation * 100f).toInt()}%", style = MaterialTheme.typography.labelSmall)
                        }
                        Slider(
                            value = saturation,
                            onValueChange = {
                                saturation = it
                                val newColor = ColorUtils.hsvToColor(hue, saturation, 0.7f)
                                onSeedColorChanged(newColor.value.toLong())
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = currentColor,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        // Contrast Level
        item {
            ShoutCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ShoutSendTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Contrast Level",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                uiState.contrast <= -0.5f -> "Reduced (${uiState.contrast})"
                                uiState.contrast >= 0.5f -> "High (${uiState.contrast})"
                                else -> "Standard (0.0)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = uiState.contrast,
                        onValueChange = onContrastChanged,
                        valueRange = -1f..1f,
                        steps = 2
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShapesAndHeroTabContent(
    uiState: DevPanelUiState,
    onRadiusChanged: (String, Float) -> Unit,
    onResetRadius: (String) -> Unit,
    onHeroShapeChanged: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Hero Shape Selector & Live Preview
        item {
            ShoutCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ShoutSendTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hero Shape",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    // Hero Preview Box with Morphing Shape
                    Box(
                        modifier = Modifier
                            .size(ShoutSendTheme.sizes.buttonHeight * 2f)
                            .clip(ShoutShapes.getHeroShape(uiState.heroShape))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ShoutIcons.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(ShoutSendTheme.sizes.iconLg),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = uiState.heroShape,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xs)
                    ) {
                        ShoutShapes.AVAILABLE_HERO_SHAPES.keys.forEach { shapeName ->
                            ShoutFilterChip(
                                selected = uiState.heroShape == shapeName,
                                onClick = { onHeroShapeChanged(shapeName) },
                                label = shapeName
                            )
                        }
                    }
                }
            }
        }

        // Corner Radii Sliders
        item {
            Text(
                text = "Corner Radii (dp)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(uiState.radiusItems, key = { it.name }) { item ->
            var currentRadius by remember(item.currentRadiusDp) { mutableFloatStateOf(item.currentRadiusDp) }

            ShoutCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ShoutSendTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xs)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentRadius.toInt()} dp",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (currentRadius != item.defaultRadiusDp) {
                                ShoutIconButton(
                                    onClick = {
                                        currentRadius = item.defaultRadiusDp
                                        onResetRadius(item.name)
                                    }
                                ) {
                                    Icon(
                                        imageVector = ShoutIcons.Tune,
                                        contentDescription = "Reset ${item.name} radius",
                                        modifier = Modifier.size(ShoutSendTheme.sizes.iconSm)
                                    )
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
                        valueRange = 0f..48f
                    )
                }
            }
        }

        // Live Component Previews
        item {
            Text(
                text = "Live Component Previews",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            ShoutCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ShoutSendTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)
                ) {
                    ShoutButton(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ShoutButton (Full pill shape)")
                    }

                    ShoutTextField(
                        value = "Expressive TextField",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Label") }
                    )
                }
            }
        }
    }
}
