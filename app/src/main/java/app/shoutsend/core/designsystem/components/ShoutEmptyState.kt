package app.shoutsend.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.graphics.shapes.RoundedPolygon
import app.shoutsend.core.designsystem.ShoutSendTheme
import app.shoutsend.core.designsystem.ShoutShapes

@Composable
fun ShoutEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    heroPolygon: RoundedPolygon? = null,
    action: (@Composable () -> Unit)? = null
) {
    val actualPolygon = heroPolygon ?: ShoutShapes.getHeroPolygon(ShoutSendTheme.heroShapeName)
    val heroShape = ShoutShapes.getHeroShape(ShoutSendTheme.heroShapeName)

    Column(
        modifier = modifier.padding(ShoutSendTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(ShoutSendTheme.sizes.buttonHeight * 2f)
                .clip(heroShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(ShoutSendTheme.sizes.iconXl),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(ShoutSendTheme.spacing.lg))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(ShoutSendTheme.spacing.xs))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (action != null) {
            Spacer(modifier = Modifier.height(ShoutSendTheme.spacing.lg))
            action()
        }
    }
}
