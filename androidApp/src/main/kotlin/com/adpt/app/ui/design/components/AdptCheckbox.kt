package com.adpt.app.ui.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adpt.app.ui.design.AdptTheme

@Composable
fun AdptCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = AdptTheme.colors
    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "checkProgress",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier = if (onCheckedChange != null) {
        Modifier
            .semantics { role = Role.Checkbox }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 18.dp),
                onClick = { onCheckedChange(!checked) },
            )
    } else Modifier

    Canvas(
        modifier = modifier
            .size(20.dp)
            .then(clickModifier),
    ) {
        val strokeWidth = 1.5.dp.toPx()
        val cornerRadius = CornerRadius(4.dp.toPx())
        val boxColor = if (checked) colors.accent else Color.Transparent
        val borderColor = if (checked) colors.accent else colors.outline

        drawRoundRect(color = boxColor, cornerRadius = cornerRadius)
        drawRoundRect(
            color = borderColor,
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth),
        )

        if (checkProgress > 0f) {
            val checkPath = Path().apply {
                moveTo(size.width * 0.20f, size.height * 0.50f)
                lineTo(size.width * 0.42f, size.height * 0.72f)
                lineTo(size.width * 0.80f, size.height * 0.28f)
            }
            val pathMeasure = PathMeasure().apply { setPath(checkPath, forceClosed = false) }
            val animatedPath = Path()
            pathMeasure.getSegment(
                startDistance = 0f,
                stopDistance = pathMeasure.length * checkProgress,
                destination = animatedPath,
                startWithMoveTo = true,
            )
            drawPath(
                path = animatedPath,
                color = colors.onAccent,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}
