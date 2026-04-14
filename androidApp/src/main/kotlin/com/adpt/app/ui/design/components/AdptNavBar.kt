package com.adpt.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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

    Column(modifier = modifier.fillMaxWidth()) {
        // Top divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.surface.copy(alpha = 0.7f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(bottom = navBottomPadding),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route ||
                    currentRoute?.startsWith("${item.route}?") == true
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
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onSelected,
            )
            .padding(vertical = 10.dp),
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
