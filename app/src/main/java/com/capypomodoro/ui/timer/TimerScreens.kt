package com.capypomodoro.ui.timer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capypomodoro.R
import com.capypomodoro.timer.Phase
import com.capypomodoro.timer.TimerNotifications
import com.capypomodoro.timer.formatTime
import com.capypomodoro.ui.components.Bow
import com.capypomodoro.ui.components.CapyBackground
import com.capypomodoro.ui.components.CircleIconButton
import com.capypomodoro.ui.components.LinearPhaseProgress
import com.capypomodoro.ui.components.NowPlayingCard
import com.capypomodoro.ui.components.PhaseTabs
import com.capypomodoro.ui.components.RoundDots
import com.capypomodoro.ui.components.TimerControls
import com.capypomodoro.ui.components.TimerRing
import com.capypomodoro.ui.components.pullUp
import com.capypomodoro.ui.components.softShadow
import com.capypomodoro.ui.theme.CapyColors
import com.capypomodoro.ui.theme.CapyShapes
import com.capypomodoro.ui.theme.CapyType

/** Event handlers shared by both timer layouts. */
class TimerActions(
    val onToggle: () -> Unit,
    val onReset: () -> Unit,
    val onSkip: () -> Unit,
    val onSelectPhase: (Phase) -> Unit,
    val onToggleMute: () -> Unit,
    val onOpenSettings: () -> Unit,
)

/**
 * Focus uses the ring layout (Main.dc.html); breaks use the hero-photo layout (Break.dc.html).
 */
@Composable
fun TimerRoute(viewModel: TimerViewModel, onOpenSettings: () -> Unit) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Ask for the notification permission (Android 13+) the first time the timer is started.
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val onToggle = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedForNotifications &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedForNotifications = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.onToggle()
    }

    // Optional "keep screen on during focus".
    val view = LocalView.current
    val keepOn = ui.settings.keepScreenOn && ui.timer.phase == Phase.FOCUS && ui.timer.isRunning
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    val actions = remember(viewModel, onOpenSettings) {
        TimerActions(
            onToggle = onToggle,
            onReset = viewModel::onReset,
            onSkip = viewModel::onSkip,
            onSelectPhase = viewModel::onSelectPhase,
            onToggleMute = viewModel::onToggleMute,
            onOpenSettings = onOpenSettings,
        )
    }

    CapyBackground(style = ui.settings.background) {
        Crossfade(targetState = ui.timer.phase.isBreak, label = "layout") { isBreak ->
            if (isBreak) BreakScreen(ui, actions) else FocusScreen(ui, actions)
        }
    }
}

@Composable
fun FocusScreen(ui: TimerUiState, actions: TimerActions) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Bow(width = 36.dp, height = 24.dp)
            Spacer(Modifier.padding(start = 8.dp))
            Text(stringResource(R.string.app_name), style = CapyType.AppName, modifier = Modifier.weight(1f))
            CircleIconButton(R.drawable.ic_settings, stringResource(R.string.cd_settings), actions.onOpenSettings)
        }
        Spacer(Modifier.height(18.dp))
        PhaseTabs(selected = ui.timer.phase, onSelect = actions.onSelectPhase)

        // The ring takes whatever height is left, up to the design's 300dp.
        BoxWithConstraints(
            Modifier.weight(1f).fillMaxWidth().padding(top = 36.dp, bottom = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            val ring = minOf(300.dp, maxWidth, maxHeight)
            TimerRing(progress = ui.timer.progress, art = ui.art, size = ring)
        }

        TimeReadout(ui, style = CapyType.TimerFocus)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(TimerNotifications.lineFor(ui.timer.phase)), style = CapyType.PhaseLine)
        Spacer(Modifier.height(10.dp))
        RoundDots(completed = ui.timer.completedRounds, round = ui.timer.round, rounds = ui.timer.rounds)

        Spacer(Modifier.height(18.dp))
        NowPlaying(ui, actions)
        Spacer(Modifier.height(18.dp))
        TimerControls(
            running = ui.timer.isRunning,
            onReset = actions.onReset,
            onToggle = actions.onToggle,
            onSkip = actions.onSkip,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun BreakScreen(ui: TimerUiState, actions: TimerActions) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heroHeight = minOf(390.dp, maxHeight * 0.44f)
        Column(Modifier.fillMaxSize()) {
            // Hero photo with app-name chip and settings button.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clip(RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp))
                    .background(CapyColors.PinkSoft),
            ) {
                Image(
                    painter = painterResource(ui.art.res),
                    contentDescription = stringResource(ui.art.description),
                    contentScale = ContentScale.Crop,
                    alignment = ui.art.hero,
                    modifier = Modifier.fillMaxSize(),
                )
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.app_name),
                        style = CapyType.AppName.copy(fontSize = 17.sp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.92f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    CircleIconButton(
                        R.drawable.ic_settings, stringResource(R.string.cd_settings), actions.onOpenSettings,
                        container = Color.White.copy(alpha = 0.92f), shadow = false,
                    )
                }
            }

            // Timer card overlapping the hero by 56dp, then the now-playing card.
            Column(Modifier.padding(horizontal = 24.dp).pullUp(56.dp)) {
                BreakTimerCard(ui, actions)
                Spacer(Modifier.height(16.dp))
                NowPlaying(ui, actions)
            }

            Spacer(Modifier.weight(1f))
            TimerControls(
                running = ui.timer.isRunning,
                onReset = actions.onReset,
                onToggle = actions.onToggle,
                onSkip = actions.onSkip,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 16.dp, bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun BreakTimerCard(ui: TimerUiState, actions: TimerActions) {
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .softShadow(14.dp, CapyShapes.TimerCard, 0.6f)
                .background(Color.White, CapyShapes.TimerCard)
                .padding(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PhaseTabs(selected = ui.timer.phase, onSelect = actions.onSelectPhase, compact = true)
            TimeReadout(ui, style = CapyType.TimerBreak, modifier = Modifier.padding(top = 4.dp))
            LinearPhaseProgress(ui.timer.progress)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(TimerNotifications.lineFor(ui.timer.phase)),
                    style = CapyType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.padding(start = 8.dp))
                Text(nextLabel(ui), style = CapyType.Caption, maxLines = 1)
            }
        }
        Bow(width = 84.dp, height = 56.dp, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-30).dp))
    }
}

/** MM:SS, announced politely once a minute rather than every second. */
@Composable
private fun TimeReadout(ui: TimerUiState, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    val seconds = ui.timer.secondsLeft
    val spoken = if (seconds >= 60) stringResource(R.string.cd_time_left, (seconds + 59) / 60)
    else stringResource(R.string.cd_time_left_seconds)
    Text(
        formatTime(seconds),
        style = style,
        modifier = modifier.semantics {
            contentDescription = spoken
            liveRegion = LiveRegionMode.Polite
        },
    )
}

@Composable
private fun NowPlaying(ui: TimerUiState, actions: TimerActions) {
    val noMusic = ui.track.file == null
    val status = when {
        noMusic -> stringResource(R.string.music_off)
        ui.muted -> stringResource(R.string.music_muted)
        ui.timer.isRunning -> stringResource(if (ui.timer.phase.isBreak) R.string.music_break_playing else R.string.music_focus_playing)
        ui.timer.sessionActive -> stringResource(R.string.music_paused)
        else -> stringResource(R.string.music_ready)
    }
    NowPlayingCard(
        title = stringResource(ui.track.title),
        status = status,
        muted = ui.muted,
        onToggleMute = actions.onToggleMute,
    )
}

@Composable
private fun nextLabel(ui: TimerUiState): String {
    val (phase, round) = ui.timer.next
    return if (phase == Phase.FOCUS) stringResource(R.string.next_focus_round, round)
    else stringResource(R.string.next_phase, stringResource(TimerNotifications.labelFor(phase)))
}
