package app.shoutsend.core.designsystem

import androidx.compose.runtime.Composable

/**
 * Backward compatibility alias for ChirpTheme forwarding directly to [ShoutSendTheme].
 */
@Composable
fun ChirpTheme(
    tokens: DesignTokens = DesignTokens.default(),
    content: @Composable () -> Unit
) = ShoutSendTheme(tokens = tokens, content = content)

typealias ChirpTheme = ShoutSendTheme
