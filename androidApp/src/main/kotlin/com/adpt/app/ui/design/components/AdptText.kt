package com.ventri.app.ui.design.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.ventri.app.ui.design.VentriTheme

@Composable
fun VentriText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = VentriTheme.typography.bodyMedium,
    color: Color = VentriTheme.colors.onSurface,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = overflow,
    )
}
