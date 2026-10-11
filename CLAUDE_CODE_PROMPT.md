# Prompt to paste into Claude Code

Open this folder in Claude Code, then paste:

---

Read `HANDOFF.md` and the three screen designs in `design/` (`Main.dc.html`, `Break.dc.html`, `Settings.dc.html`). They are design references, not code to run.

Build a native Android app called **Capy Pomodoro** in a new `app/` Gradle project, using Kotlin, Jetpack Compose and Material 3, matching the designs as closely as possible:

1. Set up the project, the theme (colours, Fredoka + Nunito fonts, shapes) and a polka-dot background modifier.
2. Convert `assets/images/bow.svg` to a VectorDrawable, copy both capybara PNGs into `res/drawable-nodpi/`, and build reusable composables: `Bow`, `TimerRing`, `PhaseTabs`, `NowPlayingCard`, `TimerControls`.
3. Implement the Focus and Break timer screens and the Settings screen, with navigation between them.
4. Implement the timer logic in a ViewModel plus a foreground service (notification with Pause/Skip), with settings persisted in DataStore.
5. Add ExoPlayer looping music per phase and a chime at phase end, with tracks defined in one `Tracks.kt` file. Use the files in `assets/audio/`; if it's empty, wire placeholders and tell me which files to add.

Work step by step. After each step, make sure it builds (`./gradlew assembleDebug`). Ask me before adding any dependency not mentioned in `HANDOFF.md`.

---
