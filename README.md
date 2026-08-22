# Nirmalam Chant

An ad-free, privacy-first Android chanting counter for phone, tablet, foldables, and Android TV. The initial project implements local persistence, a device-only voice-processing pipeline, and a responsive Compose surface.

## Included

- Room/SQLite `chant_sessions` and microsecond-timestamped `chant_tallies` tables.
- Foreground microphone service with local amplitude-based voice activity detection and 250 ms debounce, accommodating up to 240 counts per minute.
- A haptic pulse for each tally and a dual pulse at milestones of 108.
- Manual tally fallback plus a large 10-foot-readable count display.
- A local activity dashboard for performed chant sessions and scheduled practice plans.
- Mala progress, session intentions, local-only practice reminders, and an on-device streak/rhythm summary.
- Optional device-synthesized meditation tone after each tally; it is off by default and has no audio file or network dependency.
- No ad, analytics, telemetry, account sign-in, cloud sync, or network-storage dependency.
- All tally and session data stays only in the local Room/SQLite database on the device.

## Run

Open this folder in Android Studio (Ladybug or newer), complete the Gradle sync, then run the `app` configuration on an Android 8.0+ device. Grant microphone access before starting voice tracking.

## Before release

1. Replace the baseline local VAD threshold with a tested, bundled TFLite chant classifier and calibration screen. Never upload raw audio.
2. Add an opt-out setting for haptics and audio feedback, plus licensed temple-bell/singing-bowl assets.
3. Add encrypted local backup/export and Room migrations as schema versions evolve. Exports should only be created by an explicit user action and never uploaded by the app.
