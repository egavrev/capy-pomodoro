package com.capypomodoro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.sp
import com.capypomodoro.R
import com.capypomodoro.timer.Phase
import com.capypomodoro.timer.TimerNotifications
import com.capypomodoro.ui.theme.CapyColors
import com.capypomodoro.ui.theme.CapyShapes
import com.capypomodoro.ui.theme.CapyType

/**
 * Focus · Short break · Long break selector.
 *
 * @param compact the Break-card variant: pink-tint container, tabs sized to their labels.
 */
@Composable
fun PhaseTabs(
    selected: Phase,
    onSelect: (Phase) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val container = if (compact) CapyColors.PinkTint else CapyColors.Surface
    val shape = RoundedCornerShape(if (compact) 22.dp else 26.dp)
    Row(
        modifier
            .then(if (compact) Modifier else Modifier.fillMaxWidth().softShadow(6.dp, shape, 0.5f))
            .clip(shape)
            .background(container)
            .padding(if (compact) 4.dp else 5.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp),
    ) {
        Phase.entries.forEach { phase ->
            val isSelected = phase == selected
            val bg by animateColorAsState(if (isSelected) CapyColors.Rose else Color.Transparent, label = "tabBg")
            val fg by animateColorAsState(if (isSelected) Color.White else CapyColors.Muted, label = "tabFg")
            Box(
                Modifier
                    .then(if (compact) Modifier else Modifier.weight(1f))
                    .height(44.dp)
                    .clip(RoundedCornerShape(if (compact) 20.dp else 22.dp))
                    .background(bg)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(phase) })
                    .padding(horizontal = if (compact) 14.dp else 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(TimerNotifications.labelFor(phase)),
                    style = if (compact) CapyType.Tab.copy(fontSize = 13.sp) else CapyType.Tab,
                    color = fg,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Track name, status line and mute toggle. */
@Composable
fun NowPlayingCard(
    title: String,
    status: String,
    muted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .softShadow(6.dp, CapyShapes.Card, 0.5f)
            .clip(CapyShapes.Card)
            .background(CapyColors.Surface)
            .padding(start = 14.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(CapyShapes.IconTile).background(CapyColors.PinkSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_music), contentDescription = null, tint = CapyColors.Rose, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = CapyType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(status, style = CapyType.CardSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        CircleIconButton(
            icon = if (muted) R.drawable.ic_volume_off else R.drawable.ic_volume_on,
            contentDescription = stringResource(if (muted) R.string.cd_unmute else R.string.cd_mute),
            onClick = onToggleMute,
            container = CapyColors.PinkTint,
            iconSize = 20.dp,
            shadow = false,
        )
    }
}

/** Reset (56dp) · Play/Pause (88dp, rose, 5dp white border) · Skip (56dp). */
@Composable
fun TimerControls(
    running: Boolean,
    onReset: () -> Unit,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = R.drawable.ic_reset,
            contentDescription = stringResource(R.string.cd_reset),
            onClick = onReset,
            size = 56.dp,
            iconSize = 24.dp,
        )
        val playLabel = stringResource(if (running) R.string.cd_pause else R.string.cd_start)
        Box(
            Modifier
                .size(88.dp)
                .softShadow(14.dp, CircleShape)
                .clip(CircleShape)
                .background(CapyColors.Rose)
                .border(5.dp, Color.White, CircleShape)
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { contentDescription = playLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (running) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (running) 32.dp else 34.dp),
            )
        }
        CircleIconButton(
            icon = R.drawable.ic_skip,
            contentDescription = stringResource(R.string.cd_skip),
            onClick = onSkip,
            size = 56.dp,
            iconSize = 24.dp,
        )
    }
}

/** Filled dots for finished focus rounds, then "Round N of M". */
@Composable
fun RoundDots(completed: Int, round: Int, rounds: Int, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(rounds) { i ->
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (i < completed) CapyColors.PinkProgress else Color.White)
                    .border(2.dp, CapyColors.PinkProgress, CircleShape),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.round_of, round, rounds), style = CapyType.Caption)
    }
}

/** 12dp linear progress bar from the Break card. */
@Composable
fun LinearPhaseProgress(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(CapyColors.PinkSoft),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(CapyColors.PinkProgress),
        )
    }
}


/**
 * Pulls this element up by [by], overlapping whatever is above it, and gives the space back to the
 * layout (unlike Modifier.offset, which leaves a gap below).
 */
fun Modifier.pullUp(by: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val px = by.roundToPx()
    layout(placeable.width, (placeable.height - px).coerceAtLeast(0)) { placeable.place(0, -px) }
}
