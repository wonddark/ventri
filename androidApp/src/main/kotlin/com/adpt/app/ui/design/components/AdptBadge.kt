package com.adpt.app.ui.design.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AdptCircleBadge(
    borderColor: Color,
    size: Dp = 48.dp,
    strokeWidth: Dp = 2.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .border(strokeWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
