package com.ventri.app.ui.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ventri.app.ui.design.VentriTheme

@Composable
fun VentriProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = VentriTheme.colors.accent,
    strokeWidth: Dp = 3.dp,
    size: Dp = 40.dp,
) {
    val transition = rememberInfiniteTransition(label = "progress")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
        ),
        label = "rotation",
    )

    val sweepAngle by transition.animateFloat(
        initialValue = 30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                30f at 0 using LinearEasing
                270f at 600 using LinearEasing
                30f at 1200 using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    Canvas(modifier = modifier.size(size)) {
        rotate(rotation) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}
