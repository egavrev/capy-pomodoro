package com.capypomodoro.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capypomodoro.R
import com.capypomodoro.audio.Track
import com.capypomodoro.audio.Tracks
import com.capypomodoro.data.AppSettings
import com.capypomodoro.data.BackgroundStyle
import com.capypomodoro.ui.components.Bow
import com.capypomodoro.ui.components.CapyArt
import com.capypomodoro.ui.components.CapyThumb
import com.capypomodoro.ui.components.CircleIconButton
import com.capypomodoro.ui.components.gingham
import com.capypomodoro.ui.components.polkaDots
import com.capypomodoro.ui.components.softShadow
import com.capypomodoro.ui.theme.CapyColors
import com.capypomodoro.ui.theme.CapyShapes
import com.capypomodoro.ui.theme.CapyType

@Composable
fun SettingsRoute(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsScreen(settings = settings, onChange = viewModel::update, onBack = onBack)
}

/** Settings.dc.html: Timer, Background and Music cards. Every change is saved immediately. */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    // "Save settings" is reassurance only: it flips to "Saved" until the next change.
    var saved by remember { mutableStateOf(false) }
    val change: ((AppSettings) -> AppSettings) -> Unit = { saved = false; onChange(it) }

    Column(
        Modifier
            .fillMaxSize()
            .background(CapyColors.BlushBg)
            .polkaDots()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(R.drawable.ic_back, stringResource(R.string.cd_back), onBack)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.settings_title), style = CapyType.ScreenTitle, modifier = Modifier.weight(1f))
            Bow(width = 48.dp, height = 32.dp)
        }

        SettingsCard(title = stringResource(R.string.section_timer), bottomPadding = 10.dp) {
            Stepper(
                label = stringResource(R.string.set_focus), hint = stringResource(R.string.set_focus_hint),
                value = settings.focusMinutes, display = stringResource(R.string.unit_min, settings.focusMinutes),
                range = AppSettings.FOCUS_RANGE, step = AppSettings.FOCUS_STEP,
                onValue = { v -> change { it.copy(focusMinutes = v) } },
            )
            Stepper(
                label = stringResource(R.string.set_short), hint = stringResource(R.string.set_short_hint),
                value = settings.shortBreakMinutes, display = stringResource(R.string.unit_min, settings.shortBreakMinutes),
                range = AppSettings.SHORT_RANGE, step = AppSettings.SHORT_STEP,
                onValue = { v -> change { it.copy(shortBreakMinutes = v) } },
            )
            Stepper(
                label = stringResource(R.string.set_long), hint = stringResource(R.string.set_long_hint),
                value = settings.longBreakMinutes, display = stringResource(R.string.unit_min, settings.longBreakMinutes),
                range = AppSettings.LONG_RANGE, step = AppSettings.LONG_STEP,
                onValue = { v -> change { it.copy(longBreakMinutes = v) } },
            )
            Stepper(
                label = stringResource(R.string.set_rounds), hint = stringResource(R.string.set_rounds_hint),
                value = settings.rounds, display = settings.rounds.toString(),
                range = AppSettings.ROUNDS_RANGE, step = AppSettings.ROUNDS_STEP,
                onValue = { v -> change { it.copy(rounds = v) } },
            )
            ToggleRow(
                label = stringResource(R.string.set_autostart), hint = stringResource(R.string.set_autostart_hint),
                checked = settings.autoStartNext, onChecked = { v -> change { it.copy(autoStartNext = v) } },
            )
            ToggleRow(
                label = stringResource(R.string.set_chime), hint = stringResource(R.string.set_chime_hint),
                checked = settings.chime, onChecked = { v -> change { it.copy(chime = v) } },
            )
            ToggleRow(
                label = stringResource(R.string.set_keep_awake), hint = stringResource(R.string.set_keep_awake_hint),
                checked = settings.keepScreenOn, onChecked = { v -> change { it.copy(keepScreenOn = v) } },
                last = true,
            )
        }

        SettingsCard(title = stringResource(R.string.section_background)) {
            BackgroundGrid(selected = settings.background, onSelect = { bg -> change { it.copy(background = bg) } })
        }

        SettingsCard(title = stringResource(R.string.section_music)) {
            MusicGroup(
                title = stringResource(R.string.music_during_focus), art = CapyArt.Study,
                options = Tracks.focus, selectedId = settings.focusTrackId,
                onSelect = { id -> change { it.copy(focusTrackId = id) } },
            )
            Spacer(Modifier.height(12.dp))
            MusicGroup(
                title = stringResource(R.string.music_during_breaks), art = CapyArt.Relax,
                options = Tracks.breaks, selectedId = settings.breakTrackId,
                onSelect = { id -> change { it.copy(breakTrackId = id) } },
            )
            Spacer(Modifier.height(18.dp))
            VolumeSlider(volume = settings.volume, onVolume = { v -> change { it.copy(volume = v) } })
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .softShadow(14.dp, CapyShapes.Pill, 0.9f)
                .clip(CapyShapes.Pill)
                .background(CapyColors.Rose)
                .clickable(role = Role.Button) { saved = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(if (saved) R.string.saved else R.string.save_settings),
                style = CapyType.Button, color = Color.White,
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    bottomPadding: androidx.compose.ui.unit.Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .softShadow(8.dp, CapyShapes.SettingsCard, 0.5f)
            .clip(CapyShapes.SettingsCard)
            .background(CapyColors.Surface)
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = bottomPadding),
    ) {
        Text(title, style = CapyType.SectionTitle, modifier = Modifier.padding(bottom = 12.dp))
        content()
    }
}

@Composable
private fun Stepper(
    label: String,
    hint: String,
    value: Int,
    display: String,
    range: IntRange,
    step: Int,
    onValue: (Int) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
            LabelHint(label, hint, Modifier.weight(1f))
            StepButton("−", stringResource(R.string.cd_decrease, label), enabled = value > range.first) {
                onValue((value - step).coerceIn(range))
            }
            Text(display, style = CapyType.StepperValue, textAlign = TextAlign.Center, modifier = Modifier.width(64.dp))
            StepButton("+", stringResource(R.string.cd_increase, label), enabled = value < range.last) {
                onValue((value + step).coerceIn(range))
            }
        }
        HorizontalDivider(thickness = 1.dp, color = CapyColors.PinkLine)
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(start = 10.dp)
            .size(44.dp)
            .clip(CapyShapes.Chip)
            .background(CapyColors.PinkTint)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            style = CapyType.Tab.copy(fontSize = 22.sp),
            color = if (enabled) CapyColors.Rose else CapyColors.Rose.copy(alpha = 0.35f),
        )
    }
}

@Composable
private fun ToggleRow(label: String, hint: String, checked: Boolean, onChecked: (Boolean) -> Unit, last: Boolean = false) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChecked),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabelHint(label, hint, Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = null, // the whole row toggles
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CapyColors.Rose,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = CapyColors.ToggleOff,
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }
        if (!last) HorizontalDivider(thickness = 1.dp, color = CapyColors.PinkLine)
    }
}

@Composable
private fun LabelHint(label: String, hint: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, style = CapyType.RowLabel)
        Text(hint, style = CapyType.RowHint)
    }
}

@Composable
private fun BackgroundGrid(selected: BackgroundStyle, onSelect: (BackgroundStyle) -> Unit) {
    val items = listOf(
        BackgroundStyle.DOTS to R.string.bg_dots,
        BackgroundStyle.GINGHAM to R.string.bg_gingham,
        BackgroundStyle.CREAM to R.string.bg_cream,
        BackgroundStyle.ROOM to R.string.bg_room,
    )
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (style, label) ->
                    BackgroundTile(
                        style = style, label = stringResource(label), selected = style == selected,
                        onClick = { onSelect(style) }, modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundTile(style: BackgroundStyle, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val preview = when (style) {
                BackgroundStyle.DOTS -> Modifier.background(CapyColors.BlushBg).polkaDots(Color(0xFFF2A9BD), 2.dp, 16.dp)
                BackgroundStyle.GINGHAM -> Modifier.background(Color.White).gingham(Color(0xFFF29BB3).copy(alpha = 0.35f), 22.dp)
                BackgroundStyle.CREAM -> Modifier.background(CapyColors.Cream)
                BackgroundStyle.ROOM -> Modifier.background(CapyColors.PinkSoft)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(CapyShapes.Tile)
                    .then(preview)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) CapyColors.Rose else CapyColors.TileBorder,
                        shape = CapyShapes.Tile,
                    ),
            ) {
                if (style == BackgroundStyle.ROOM) {
                    Image(
                        painter = painterResource(R.drawable.capy_relax),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = androidx.compose.ui.BiasAlignment(-0.2f, -0.1f),
                        modifier = Modifier.fillMaxSize().padding(if (selected) 3.dp else 1.dp).clip(CapyShapes.Tile),
                    )
                }
            }
            Text(label, style = CapyType.Chip.copy(color = CapyColors.Ink), modifier = Modifier.padding(start = 4.dp))
        }
        if (selected) {
            Bow(width = 42.dp, height = 28.dp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-10).dp))
        }
    }
}

@Composable
private fun MusicGroup(title: String, art: CapyArt, options: List<Track>, selectedId: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CapyThumb(art, 28.dp)
            Text(title, style = CapyType.GroupTitle)
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { track ->
                        MusicChip(
                            label = stringResource(track.title),
                            selected = track.id == selectedId,
                            onClick = { onSelect(track.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val fg = if (selected) Color.White else CapyColors.Ink
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(CapyShapes.Chip)
            .background(if (selected) CapyColors.Rose else CapyColors.PinkTint)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(R.drawable.ic_music), contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(label, style = CapyType.Chip, color = fg, maxLines = 2)
    }
}

@Composable
private fun VolumeSlider(volume: Int, onVolume: (Int) -> Unit) {
    // Local value while dragging; saved when the finger lifts.
    var dragging by remember { mutableFloatStateOf(volume.toFloat()) }
    LaunchedEffect(volume) { dragging = volume.toFloat() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.volume), style = CapyType.GroupTitle, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.percent, dragging.toInt()), style = CapyType.GroupTitle.copy(color = CapyColors.Rose))
        }
        Slider(
            value = dragging,
            onValueChange = { dragging = it },
            onValueChangeFinished = { onVolume(dragging.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = CapyColors.Rose,
                activeTrackColor = CapyColors.Rose,
                inactiveTrackColor = CapyColors.PinkSoft,
            ),
            modifier = Modifier.fillMaxWidth().height(44.dp),
        )
    }
}
