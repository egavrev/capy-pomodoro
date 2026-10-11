# Audio

Looping tracks and the chime used by the app. They're packaged as APK assets and mapped by file
name in `app/src/main/java/com/capypomodoro/audio/Tracks.kt`.

All files here are original: they were synthesised from scratch by `tools/make_audio.py`
(no samples, no third-party recordings), so there are no licence or attribution requirements.
Re-run it with `python3 tools/make_audio.py assets/audio` to regenerate.

To swap a sound, drop a new loop in this folder with the same name (or change the file name in
`Tracks.kt`) and rebuild. `.ogg` (Vorbis) loops most cleanly; `.mp3` works too.
