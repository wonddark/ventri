package com.adpt.app.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.adpt.app.ui.design.AdptShapes
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptSurface(
    modifier: Modifier = Modifier,
    color: Color = AdptTheme.colors.surface,
    shape: Shape = AdptShapes.card,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color = color, shape = shape),
        content = content,
    )
}
