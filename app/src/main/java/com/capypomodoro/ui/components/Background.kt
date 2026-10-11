package com.capypomodoro.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capypomodoro.R
import com.capypomodoro.data.BackgroundStyle
import com.capypomodoro.ui.theme.CapyColors
import kotlin.math.roundToInt

/**
 * Polka-dot pattern: one dot centred in every [spacing] cell (CSS `radial-gradient` + `background-size`).
 * Drawn as a single repeating shader, so it costs one draw call however large the area.
 */
fun Modifier.polkaDots(
    color: Color = CapyColors.Dot,
    radius: Dp = 1.6.dp,
    spacing: Dp = 20.dp,
): Modifier = drawWithCache {
    val cell = spacing.toPx().roundToInt().coerceAtLeast(2)
    val tile = ImageBitmap(cell, cell)
    Canvas(tile).drawCircle(
        center = Offset(cell / 2f, cell / 2f),
        radius = radius.toPx(),
        paint = Paint().apply { this.color = color; isAntiAlias = true },
    )
    val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind { drawRect(brush) }
}

/** Gingham: translucent vertical and horizontal half-stripes, darker where they cross. */
fun Modifier.gingham(
    color: Color = CapyColors.BowFill.copy(alpha = 0.28f),
    cellSize: Dp = 28.dp,
): Modifier = drawWithCache {
    val cell = cellSize.toPx().roundToInt().coerceAtLeast(2)
    val half = cell / 2f
    val tile = ImageBitmap(cell, cell)
    val paint = Paint().apply { this.color = color }
    Canvas(tile).apply {
        drawRect(0f, 0f, half, cell.toFloat(), paint)
        drawRect(0f, 0f, cell.toFloat(), half, paint)
    }
    val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind { drawRect(brush) }
}

/** The background chosen in Settings, painted behind the timer screens. */
fun Modifier.capyBackground(style: BackgroundStyle): Modifier = when (style) {
    BackgroundStyle.DOTS -> background(CapyColors.BlushBg).polkaDots()
    BackgroundStyle.GINGHAM -> background(Color.White).gingham()
    BackgroundStyle.CREAM -> background(CapyColors.Cream)
    BackgroundStyle.ROOM -> background(CapyColors.PinkSoft)
}

/** Full-screen background. "Cozy capy room" shows the relax illustration under a soft white veil. */
@Composable
fun CapyBackground(
    style: BackgroundStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().capyBackground(style)) {
        if (style == BackgroundStyle.ROOM) {
            Image(
                painter = painterResource(R.drawable.capy_relax),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(-0.2f, -0.1f),
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.62f)))
        }
        content()
    }
}
