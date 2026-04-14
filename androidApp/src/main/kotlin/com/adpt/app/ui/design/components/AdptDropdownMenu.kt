package com.ventri.app.ui.design.components

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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ventri.app.ui.design.VentriShapes
import com.ventri.app.ui.design.VentriTheme

@Composable
fun VentriDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (expanded) {
        Popup(
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
            alignment = Alignment.TopStart
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(100)) + scaleIn(tween(100), initialScale = 0.95f),
                exit = fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.95f),
            ) {
                Column(
                    modifier = modifier
                        .widthIn(min = 160.dp).width(intrinsicSize = IntrinsicSize.Min)
                        .shadow(elevation = 4.dp, shape = VentriShapes.small)
                        .clip(VentriShapes.small)
                        .background(color = VentriTheme.colors.surface, shape = VentriShapes.small),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun VentriDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    selected: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Box(modifier = Modifier.padding(end = 8.dp)) { leadingIcon() }
        }
        Box(modifier = Modifier.weight(1f)) { text() }
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            VentriIcon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = VentriTheme.colors.accent,
            )
        }
    }
}
