package com.adpt.app.ui.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (expanded) {
        Popup(
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(100)) + scaleIn(tween(100), initialScale = 0.95f),
                exit = fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.95f),
            ) {
                Column(
                    modifier = modifier
                        .widthIn(min = 160.dp)
                        .shadow(elevation = 4.dp, shape = AdptShapes.small)
                        .clip(AdptShapes.small)
                        .background(color = AdptTheme.colors.surface, shape = AdptShapes.small),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun AdptDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (leadingIcon != null) {
            Box(modifier = Modifier.padding(end = 8.dp)) { leadingIcon() }
        }
        text()
    }
}
