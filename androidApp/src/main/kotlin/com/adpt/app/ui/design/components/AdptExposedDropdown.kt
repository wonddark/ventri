package com.adpt.app.ui.design.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptExposedDropdown(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedText: String,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = AdptTheme.colors
    val typography = AdptTheme.typography
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AdptShapes.small)
                .border(1.dp, if (expanded) colors.accent else colors.outline, AdptShapes.small)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                ) { onExpandedChange(!expanded) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AdptText(
                    text = label,
                    style = typography.labelSmall,
                    color = if (expanded) colors.accent else colors.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(2.dp))
                AdptText(
                    text = selectedText,
                    style = typography.bodyMedium,
                    color = colors.onSurface,
                )
            }
            AdptIcon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = colors.onSurface.copy(alpha = 0.6f),
            )
        }
        AdptDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            content()
        }
    }
}
