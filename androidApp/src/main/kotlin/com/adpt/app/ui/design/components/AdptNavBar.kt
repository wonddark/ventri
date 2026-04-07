package com.adpt.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

data class AdptNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun AdptNavBar(
    items: List<AdptNavItem>,
    currentRoute: String?,
    onItemSelected: (AdptNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AdptTheme.colors
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = navBottomPadding)
            .shadow(elevation = 8.dp, shape = AdptShapes.pill)
    ) {
        // Frosted glass background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = 0.99f }
                .blur(radius = 15.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    color = colors.surface.copy(alpha = 0.75f),
                    shape = AdptShapes.pill,
                )
        )
        // Nav items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AdptShapes.pill)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                AdptNavBarItem(
                    item = item,
                    selected = selected,
                    onSelected = { onItemSelected(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AdptNavBarItem(
    item: AdptNavItem,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AdptTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .background(
                color = if (selected) colors.accentMuted else Color.Transparent,
                shape = AdptShapes.pill,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onSelected,
            )
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AdptIcon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (selected) colors.accent else colors.onSurface.copy(alpha = 0.5f),
            )
            AdptText(
                text = item.label,
                style = AdptTheme.typography.labelSmall,
                color = if (selected) colors.accent else colors.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}
