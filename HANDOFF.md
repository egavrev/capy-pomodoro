# Capy Pomodoro — Android build handoff

A cozy Pomodoro timer for Android. Pastel pink and white, capybara illustrations, pink bows as the signature decoration.

## Folder contents

| Path | What it is |
|---|---|
| `design/Main.dc.html` | **Focus timer** screen (reference markup + behaviour) |
| `design/Break.dc.html` | **Break timer** screen (alternative layout: hero photo + overlapping card) |
| `design/Settings.dc.html` | **Settings** screen |
| `design/canvas.json` | Canvas layout index (frame sizes only; ignore for the build) |
| `assets/images/capy_study.png` | Study capybara → used in Focus phase |
| `assets/images/capy_relax.png` | Relax capybara → used in Short/Long break phases |
| `assets/images/bow.svg` | Bow motif → convert to a VectorDrawable (`ic_bow.xml`) |
| `assets/audio/` | Empty: drop real audio loops here (see *Music*) |

The `.dc.html` files are design-tool source, not production code. Read them for exact sizes, colours, copy and layout. Inline `style` attributes hold the values, and the `<script>` at the bottom shows the intended timer and settings logic. Don't try to run them.

## Recommended stack

- Kotlin, Jetpack Compose, Material 3 (custom theme below), single-activity
- minSdk 26, targetSdk latest
- `ViewModel` + `StateFlow`; settings in **Preferences DataStore**
- Timer in a **foreground service** with an ongoing notification (time left + pause/skip actions) so it survives backgrounding
- Audio: **Media3 ExoPlayer**, looping per phase; a short chime via `SoundPool`
- Navigation Compose: `timer` ↔ `settings`

## Design tokens

| Token | Hex | Use |
|---|---|---|
| `blushBg` | `#FFF5F8` | App background |
| `dot` | `#F7C9D6` | Polka-dot pattern on background (1.6dp dots, 20dp grid) |
| `surface` | `#FFFFFF` | Cards, pills container, secondary buttons |
| `pinkSoft` | `#FCE1E9` | Ring track, progress track, icon tiles |
| `pinkTint` | `#FFF0F4` | Unselected chips, stepper buttons |
| `pinkLine` | `#FBE3EA` | Row dividers |
| `pinkProgress` | `#E77C99` | Ring / progress fill, round dots |
| `rose` (primary) | `#B8456A` | Selected tabs/chips, play button, toggle on, accent text (white text on it passes AA) |
| `bowFill` / `bowStroke` / `bowKnot` | `#F7B3C4` / `#C9506F` / `#F29BB3` | Bow motif |
| `ink` | `#4A2C36` | Primary text |
| `muted` | `#7E5A66` | Secondary text |
| `toggleOff` | `#E9D3DA` | Switch track off |

Shadows: soft, rose-tinted (`rgba(184,69,106,0.10–0.30)`), never grey.

**Type:** *Fredoka* (600) for display, meaning the app name, the timer digits (64sp on Focus, 60sp on Break), section titles and the Save button. *Nunito* (600/700/800) for everything else. Both are on Google Fonts; use downloadable fonts or bundle the TTFs.

**Shapes:** pills fully rounded; cards 22–30dp; image tiles 18dp; phone screen corners don't matter.

**Bow usage:** next to the app name in the top bar; on top of the timer ring (84×56dp, centred, overlapping the ring by ~half); on top of the Break card; in the Settings header; as a small badge (42×28dp) on the selected background tile.

## Screens

### 1. Focus timer (`Main.dc.html`)
Top to bottom, 24dp side padding:
1. **Top bar**: bow + "Capy Pomodoro" (Fredoka 22sp). Settings gear icon button on the right (44dp white circle) opens Settings.
2. **Phase tabs**: white pill container with 3 equal tabs: Focus · Short break · Long break. Selected = rose fill, white text. Tapping one switches phase and resets the time.
3. **Timer ring**: 300dp. Ring radius 138dp, stroke 14dp, round caps, starting at 12 o'clock, filling clockwise as time elapses. Inside: a 232dp circular crop of the phase image, zoomed so the capybara's head and upper body fill it. Bow on top.
4. **Time**: `MM:SS`, then a phase line in rose ("Small steps, big dreams" / "Rest those little paws" / "Stretch, sip, breathe"), then round dots (filled = completed) + "Round N of M".
5. **Now playing card**: music-note tile, track name, status ("Focus sound · playing" / "Muted"), mute toggle button.
6. **Controls** (pinned to bottom): Reset (56dp) · Play/Pause (88dp, rose, 5dp white border) · Skip to next phase (56dp).

### 2. Break timer (`Break.dc.html`)
Same functions, more relaxed layout. Use it for break phases, or offer it as a layout option:
1. **Hero image**: full-width, 390dp tall, bottom corners rounded 44dp, the relax capybara (`object-fit: cover`). Overlaid: app name chip (translucent white) and settings button.
2. **Timer card**: overlaps the hero by 56dp. White, 30dp radius, bow on its top edge. Compact phase tabs, time 60sp, linear progress bar (12dp), phase line + "Next: Focus · round 3".
3. Now playing card and controls as on Focus.

### 3. Settings (`Settings.dc.html`)
Scrolling screen. Back arrow returns to the timer. Bow in the header. Three white cards:
- **Timer**: steppers (− value +, 44dp buttons):
  - Focus 25 min (5–90, step 5)
  - Short break 5 min (1–30, step 1)
  - Long break 15 min (5–60, step 5)
  - Rounds before long break 4 (1–10)
  - Switches: *Auto-start next phase* (default on), *Chime when a phase ends* (default on; plays even when music is muted)
- **Background**: 2×2 grid of preview tiles (96dp tall): Blush dots (default), Pink gingham, Plain cream, Cozy capy room (relax image). Selected tile = 3dp rose border + bow badge. This sets the timer screens' background.
- **Music**: two radio groups, each 2×2 chips with a small round thumbnail of the matching capybara:
  - *During focus*: Lo-fi rain (default), Soft piano, Café murmur, No music
  - *During breaks*: Birdsong & piano (default), Ocean waves, Music box, No music
  - Volume slider 0–100 (default 60), rose accent
- **Save settings** button: full width, 56dp, rose. Label changes to "Saved" after tapping. In the real app, persist to DataStore on every change and make the button optional.

## Behaviour

- Cycle: Focus → Short break → Focus → … After *Rounds* focus sessions, a Long break, then reset the round count.
- Play/Pause toggles the countdown. Reset restarts the current phase. Skip jumps to the next phase in the cycle.
- At 00:00: play the chime (if on), switch phase, and auto-start if that setting is on.
- Each phase's music loops while running and pauses when the timer pauses. Mute is a quick override on the timer screen.
- Changing a duration in Settings applies to the next phase start, not the one already running.
- Foreground-service notification shows the phase + time left with Pause/Skip actions. Ask for the notification permission on Android 13+.
- Keep the screen awake during focus: optional, off by default.

## Music

The track names are placeholders. Put looping audio files in `assets/audio/` (e.g. `focus_lofi_rain.mp3`, `break_birdsong_piano.mp3`, `chime.ogg`) and map them in a single `Tracks.kt` list so they're easy to swap. Use only audio you have the rights to (your own, or royalty-free/CC0 with attribution where required).

## Accessibility

- All touch targets ≥ 44dp (48dp preferred on Android).
- Icon-only buttons need `contentDescription` (Settings, Reset, Play/Pause, Skip, Mute).
- Tabs are a `TabRow`/selectable group, the switches are Material `Switch`, and the music choices are radio groups.
- Timer digits: announce politely, not every second (e.g. on start/pause and each minute).

## Out of scope (for v1)
Statistics/history, task lists, widgets, and Wear OS. Keep the architecture open for a stats screen later.
