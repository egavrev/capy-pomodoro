package com.capypomodoro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capypomodoro.ui.theme.CapyColors

/**
 * 300dp progress ring from the Focus design: radius 138, 14dp stroke with round caps, starting at
 * 12 o'clock and filling clockwise as time elapses. The capybara sits in a 232dp circle inside and
 * the bow overlaps the top of the ring. Everything scales with [size].
 *
 * @param progress 0f..1f elapsed fraction of the phase.
 */
@Composable
fun TimerRing(
    progress: Float,
    art: CapyArt,
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
) {
    val k = size.value / 300f
    // Smooth the 4 Hz ticks; snap back instantly when a new phase starts.
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (progress < 0.001f) tween(0) else tween(durationMillis = 300, easing = LinearEasing),
        label = "ring",
    )

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx() * k
            val radius = 138.dp.toPx() * k
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            drawCircle(color = CapyColors.Surface, radius = radius, center = center)
            drawCircle(color = CapyColors.PinkSoft, radius = radius, center = center, style = Stroke(stroke))
            if (animated > 0f) {
                drawArc(
                    color = CapyColors.PinkProgress,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        CapyPortrait(art = art, diameter = 232.dp * k)
        Bow(
            width = 84.dp * k,
            height = 56.dp * k,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-26).dp * k),
        )
    }
}
