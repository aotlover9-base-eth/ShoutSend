package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Connected button group for type pickers and segmented toggles.
 * Built on Material 3 Expressive [ButtonGroup].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShoutButtonGroup(
    modifier: Modifier = Modifier,
    expandedRatio: Float = ButtonGroupDefaults.ExpandedRatio,
    horizontalArrangement: Arrangement.Horizontal = ButtonGroupDefaults.HorizontalArrangement,
    content: @Composable ButtonGroupScope.() -> Unit
) {
    ButtonGroup(
        modifier = modifier,
        expandedRatio = expandedRatio,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}
