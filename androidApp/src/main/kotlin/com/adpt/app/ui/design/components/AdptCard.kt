package com.ventri.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ventri.app.ui.design.VentriShapes
import com.ventri.app.ui.design.VentriTheme

@Composable
fun VentriCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = VentriShapes.card)
            .clip(VentriShapes.card)
            .background(color = VentriTheme.colors.surface, shape = VentriShapes.card),
        content = content,
    )
}

@Composable
fun VentriClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = VentriShapes.card)
            .clip(VentriShapes.card)
            .background(color = VentriTheme.colors.surface, shape = VentriShapes.card)
            .semantics { role = Role.Button }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
        content = content,
    )
}
