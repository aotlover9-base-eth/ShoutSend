package app.shoutsend.core.designsystem.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import app.shoutsend.core.designsystem.DesignTokens
import app.shoutsend.core.designsystem.ShoutIcons
import app.shoutsend.core.designsystem.ShoutSendTheme
import app.shoutsend.core.designsystem.ShoutShapes
import app.shoutsend.core.designsystem.components.AcousticStatus
import app.shoutsend.core.designsystem.components.ByteMeter
import app.shoutsend.core.designsystem.components.LevelMeter
import app.shoutsend.core.designsystem.components.ShoutAssistChip
import app.shoutsend.core.designsystem.components.ShoutButton
import app.shoutsend.core.designsystem.components.ShoutButtonGroup
import app.shoutsend.core.designsystem.components.ShoutButtonSize
import app.shoutsend.core.designsystem.components.ShoutButtonVariant
import app.shoutsend.core.designsystem.components.ShoutCard
import app.shoutsend.core.designsystem.components.ShoutCardVariant
import app.shoutsend.core.designsystem.components.ShoutFilterChip
import app.shoutsend.core.designsystem.components.ShoutIconButton
import app.shoutsend.core.designsystem.components.ShoutIconButtonVariant
import app.shoutsend.core.designsystem.components.ShoutLinearProgress
import app.shoutsend.core.designsystem.components.ShoutLoadingIndicator
import app.shoutsend.core.designsystem.components.ShoutSwitch
import app.shoutsend.core.designsystem.components.ShoutTextField
import app.shoutsend.core.designsystem.components.SignalMeter
import app.shoutsend.core.designsystem.components.StatusPill

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DesignSystemGalleryContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(ShoutSendTheme.spacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.lg)
    ) {
        Text(
            text = "Material 3 Expressive Gallery",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 1. Buttons & Button Groups
        ShoutCard(
            modifier = Modifier.fillMaxWidth(),
            variant = ShoutCardVariant.ELEVATED
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ShoutSendTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)
            ) {
                Text("Buttons", style = MaterialTheme.typography.titleMedium)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShoutButton(onClick = {}, variant = ShoutButtonVariant.FILLED) { Text("Filled") }
                    ShoutButton(onClick = {}, variant = ShoutButtonVariant.TONAL) { Text("Tonal") }
                    ShoutButton(onClick = {}, variant = ShoutButtonVariant.OUTLINED) { Text("Outlined") }
                    ShoutButton(onClick = {}, variant = ShoutButtonVariant.TEXT) { Text("Text") }
                }

                Text("Sizes (S, M, L, XL)", style = MaterialTheme.typography.labelSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShoutButton(onClick = {}, size = ShoutButtonSize.S) { Text("S") }
                    ShoutButton(onClick = {}, size = ShoutButtonSize.M) { Text("M") }
                    ShoutButton(onClick = {}, size = ShoutButtonSize.L) { Text("L") }
                    ShoutButton(onClick = {}, size = ShoutButtonSize.XL) { Text("XL") }
                }

                Text("ButtonGroup", style = MaterialTheme.typography.labelSmall)
                ShoutButtonGroup(modifier = Modifier.fillMaxWidth()) {
                    ShoutButton(onClick = {}) { Text("First") }
                    ShoutButton(onClick = {}, variant = ShoutButtonVariant.TONAL) { Text("Second") }
                    ShoutButton(onClick = {}, variant = ShoutButtonVariant.OUTLINED) { Text("Third") }
                }
            }
        }

        // 2. Icon Buttons
        ShoutCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ShoutSendTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)
            ) {
                Text("Icon Buttons (Min 48dp touch target)", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)) {
                    ShoutIconButton(onClick = {}, variant = ShoutIconButtonVariant.STANDARD) {
                        Icon(ShoutIcons.Tune, contentDescription = null)
                    }
                    ShoutIconButton(onClick = {}, variant = ShoutIconButtonVariant.FILLED) {
                        Icon(ShoutIcons.Send, contentDescription = null)
                    }
                    ShoutIconButton(onClick = {}, variant = ShoutIconButtonVariant.TONAL) {
                        Icon(ShoutIcons.Mic, contentDescription = null)
                    }
                    ShoutIconButton(onClick = {}, variant = ShoutIconButtonVariant.OUTLINED) {
                        Icon(ShoutIcons.Lock, contentDescription = null)
                    }
                }
            }
        }

        // 3. Text Fields & Switches & Chips
        ShoutCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ShoutSendTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)
            ) {
                Text("Inputs & Controls", style = MaterialTheme.typography.titleMedium)

                ShoutTextField(
                    value = "Hello ShoutSend v2",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Input message") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Expressive Switch")
                    ShoutSwitch(checked = true, onCheckedChange = {})
                }

                Row(horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xs)) {
                    ShoutFilterChip(selected = true, onClick = {}, label = "Standard")
                    ShoutFilterChip(selected = false, onClick = {}, label = "Stealth")
                    ShoutAssistChip(onClick = {}, label = "Copy")
                }
            }
        }

        // 4. Hero Shapes
        ShoutCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ShoutSendTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)
            ) {
                Text("Material Expressive Hero Shapes", style = MaterialTheme.typography.titleMedium)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)
                ) {
                    ShoutShapes.AVAILABLE_HERO_SHAPES.keys.forEach { shapeName ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.xxs)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(ShoutSendTheme.sizes.buttonHeight)
                                    .clip(ShoutShapes.getHeroShape(shapeName))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shapeName.take(2),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(shapeName, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 5. Indicators & Acoustic Visuals
        ShoutCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ShoutSendTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(ShoutSendTheme.spacing.sm)
            ) {
                Text("Indicators & Acoustic Visuals", style = MaterialTheme.typography.titleMedium)

                ShoutLinearProgress(
                    progress = { 0.65f },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(status = AcousticStatus.LISTENING)
                    SignalMeter(snrDb = 18.5)
                    LevelMeter(levelDbfs = -12f)
                    ByteMeter(bytesUsed = 142, maxBytes = 320)
                }

                ShoutLoadingIndicator()
            }
        }
    }
}

@Preview(name = "Expressive Gallery - Dark", showBackground = true)
@Composable
fun PreviewDesignSystemGalleryDark() {
    ShoutSendTheme(tokens = DesignTokens.defaultDark()) {
        DesignSystemGalleryContent()
    }
}

@Preview(name = "Expressive Gallery - Light", showBackground = true)
@Composable
fun PreviewDesignSystemGalleryLight() {
    ShoutSendTheme(tokens = DesignTokens.defaultLight()) {
        DesignSystemGalleryContent()
    }
}
