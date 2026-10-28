package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import app.shoutsend.core.designsystem.ShoutSendTheme

enum class ShoutCardVariant {
    FILLED,
    ELEVATED,
    OUTLINED
}

@Composable
fun ShoutCard(
    modifier: Modifier = Modifier,
    variant: ShoutCardVariant = ShoutCardVariant.FILLED,
    shape: Shape = ShoutSendTheme.shapes.card,
    colors: CardColors? = null,
    elevation: CardElevation? = null,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    when (variant) {
        ShoutCardVariant.FILLED -> {
            Card(
                modifier = modifier,
                shape = shape,
                colors = colors ?: CardDefaults.cardColors(),
                elevation = elevation ?: CardDefaults.cardElevation(),
                border = border,
                content = content
            )
        }
        ShoutCardVariant.ELEVATED -> {
            ElevatedCard(
                modifier = modifier,
                shape = shape,
                colors = colors ?: CardDefaults.elevatedCardColors(),
                elevation = elevation ?: CardDefaults.elevatedCardElevation(),
                content = content
            )
        }
        ShoutCardVariant.OUTLINED -> {
            OutlinedCard(
                modifier = modifier,
                shape = shape,
                colors = colors ?: CardDefaults.outlinedCardColors(),
                elevation = elevation ?: CardDefaults.outlinedCardElevation(),
                border = border ?: CardDefaults.outlinedCardBorder(),
                content = content
            )
        }
    }
}
