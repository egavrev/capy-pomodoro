package com.capypomodoro.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capypomodoro.R
import com.capypomodoro.timer.Phase
import com.capypomodoro.ui.theme.CapyColors

/** Soft, rose-tinted drop shadow (never grey). */
fun Modifier.softShadow(elevation: Dp, shape: Shape, strength: Float = 1f): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = CapyColors.Rose.copy(alpha = 0.10f * strength),
    spotColor = CapyColors.Rose.copy(alpha = 0.30f * strength),
)

/** The signature pink bow. Default size is the SVG's own 64×42. */
@Composable
fun Bow(modifier: Modifier = Modifier, width: Dp = 64.dp, height: Dp = 42.dp) {
    Image(
        painter = painterResource(R.drawable.ic_bow),
        contentDescription = null,
        modifier = modifier.size(width, height),
    )
}

/**
 * How each capybara illustration is framed, using the numbers from the designs.
 * [ringWidth]/[ringLeft]/[ringTop] place the image inside the 232dp circle on the Focus screen;
 * [hero] is the CSS object-position used for full-bleed crops.
 */
data class CapyArt(
    @DrawableRes val res: Int,
    @StringRes val description: Int,
    val aspect: Float,
    val ringWidth: Float,
    val ringLeft: Float,
    val ringTop: Float,
    val hero: BiasAlignment,
    val thumb: BiasAlignment,
) {
    companion object {
        /** object-position percentages → Compose bias (-1..1). */
        private fun pos(x: Float, y: Float) = BiasAlignment(x / 50f - 1f, y / 50f - 1f)

        val Study = CapyArt(
            res = R.drawable.capy_study, description = R.string.cd_focus_image,
            aspect = 1295f / 1214f, ringWidth = 400f, ringLeft = -66f, ringTop = -38f,
            hero = pos(45f, 30f), thumb = pos(45f, 28f),
        )
        val Relax = CapyArt(
            res = R.drawable.capy_relax, description = R.string.cd_break_image,
            aspect = 1089f / 1444f, ringWidth = 464f, ringLeft = -93f, ringTop = -31f,
            hero = pos(42f, 50f), thumb = pos(40f, 35f),
        )

        fun forPhase(phase: Phase) = if (phase == Phase.FOCUS) Study else Relax
    }
}

/** The illustration cropped into a circle of [diameter], framed like the 232dp design circle. */
@Composable
fun CapyPortrait(art: CapyArt, diameter: Dp, modifier: Modifier = Modifier, decorative: Boolean = false) {
    val k = diameter.value / 232f
    Box(
        modifier
            .size(diameter)
            .clip(CircleShape)
            .background(CapyColors.PinkSoft),
    ) {
        Image(
            painter = painterResource(art.res),
            contentDescription = if (decorative) null else stringResource(art.description),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset((art.ringLeft * k).dp, (art.ringTop * k).dp)
                .requiredSize((art.ringWidth * k).dp, (art.ringWidth * art.aspect * k).dp),
        )
    }
}

/** Small round thumbnail used in Settings (object-fit: cover). */
@Composable
fun CapyThumb(art: CapyArt, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(art.res),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = art.thumb,
        modifier = modifier.size(size).clip(CircleShape),
    )
}

/** Round icon-only button with a soft shadow, e.g. Settings, Back, Reset, Skip. */
@Composable
fun CircleIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    container: Color = CapyColors.Surface,
    tint: Color = CapyColors.Rose,
    shadow: Boolean = true,
) {
    val shape = CircleShape
    Box(
        modifier
            .size(size)
            .then(if (shadow) Modifier.softShadow(4.dp, shape, 0.6f) else Modifier)
            .clip(shape)
            .background(container)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                role = Role.Button,
                onClickLabel = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
