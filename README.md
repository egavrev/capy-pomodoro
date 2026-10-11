# Capy Pomodoro 🎀

A cozy Pomodoro timer for Android, with pastel pink, capybaras and bows. Kotlin, Jetpack Compose and Material 3.

- **Focus** screen with the progress ring, **Break** screen with the hero photo, and **Settings** (timer lengths, rounds, auto-start, chime, keep screen on, background, music, volume).
- The timer runs in a foreground service with a notification (Pause/Resume, Skip), so it keeps going with the screen off or the app closed.
- Each phase has its own looping music (Media3 ExoPlayer) and a soft chime at the end (SoundPool). All audio is original and synthesised by `tools/make_audio.py`. See `assets/audio/README.md`.
- Settings are saved instantly in DataStore.

## Getting the APK

Every push to `main` is built by GitHub Actions (`.github/workflows/android.yml`):

1. unit tests (`TimerLogicTest`),
2. `assembleDebug assembleRelease`,
3. a UI tour on an Android 13 emulator that taps through every screen. Its screenshots and logs go to the [`screenshots`](../../tree/screenshots) branch.

The latest APKs are on the [`builds`](../../tree/builds) branch:

- `capy-pomodoro.apk` is the one to install (release build, about 10 MB).
- `capy-pomodoro-debug.apk` is a debug build, for troubleshooting.

No Android Studio is needed. To build locally anyway: `cd app && ./gradlew assembleRelease` (JDK 17+ and the Android SDK).

### Release signing

Updates install over the old version only if every APK is signed with the same key. The key lives outside the repo, in `signing/` in your local folder (git-ignored; **back it up**). CI reads it from two repository secrets:

| Secret | Value |
|---|---|
| `CAPY_KEYSTORE_B64` | contents of `signing/CAPY_KEYSTORE_B64.txt` |
| `CAPY_KEYSTORE_PASSWORD` | contents of `signing/CAPY_KEYSTORE_PASSWORD.txt` |

Add them under GitHub → repo **Settings → Secrets and variables → Actions → New repository secret**.

## Installing on a Family Link phone

1. **Get the APK onto the phone.** Email `capy-pomodoro.apk` to the phone's Gmail, or copy it over USB into *Downloads*.
2. **Open it** from Gmail or *My Files*. Android asks to allow that app to "install unknown apps". On a Family Link-supervised account this switch is usually **blocked by the parent**. If it is, use step 3.
3. **USB install with adb** (no Android Studio needed):
   - In the **Family Link** app, open the child's device settings and **allow Developer options**.
   - On the phone: *Settings → About phone → Software information*, then tap **Build number** 7 times. Then *Settings → Developer options*, and turn on **USB debugging**.
   - On the Mac, get Google's small *platform-tools* package (`brew install android-platform-tools`, or download it from developer.android.com/tools/releases/platform-tools).
   - Connect the phone, accept the "Allow USB debugging?" prompt, and run `adb install -r capy-pomodoro.apk`.
   - When it's done, turn USB debugging off and block Developer options again in Family Link.
4. **Play Protect** may show a warning because the app isn't from Google Play. Choose *More details → Install anyway*.
5. **Give it unlimited time.** In Family Link go to *Controls → App limits → Capy Pomodoro* and choose **Unlimited time** ("Always allowed"). It then doesn't count toward the daily limit and stays usable after the limit is reached. (It is still blocked if you lock the phone manually.)
6. **First run:** tap ▶ and allow notifications, so the timer can show in the notification shade.
7. *Optional, Samsung:* *Settings → Battery → Background usage limits*. Make sure Capy Pomodoro isn't in *Sleeping/Deep sleeping apps*.

**Updating:** install the new `capy-pomodoro.apk` the same way. Settings are kept, as long as both APKs were signed with the key in `signing/`.

## Project layout

```
app/                     Gradle project (single module, the app itself)
  src/main/java/com/capypomodoro/
    timer/               TimerLogic (pure, unit-tested), PomodoroTimer, TimerService, notifications
    audio/               Tracks.kt (all sounds), MusicPlayer (ExoPlayer), ChimePlayer (SoundPool)
    data/                AppSettings + DataStore repository
    ui/                  theme, components (Bow, TimerRing, PhaseTabs, NowPlayingCard, TimerControls), screens
assets/audio/            looping tracks + chime (packaged as APK assets)
assets/images/, design/  source artwork and the reference designs
tools/                   make_audio.py (audio synth), ui_tour.sh (emulator screenshots)
```
