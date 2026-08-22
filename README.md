# Nirmalam Chant

An ad-free, privacy-first Android chanting counter for phone, tablet, foldables, and Android TV. The initial project implements local persistence, a device-only voice-processing pipeline, and a responsive Compose surface.

## Included

- Room/SQLite `chant_sessions` and microsecond-timestamped `chant_tallies` tables.
- User-initiated foreground microphone service for private, on-device voice tracking; it can be stopped at any time and never saves or uploads audio.
- A haptic pulse for each tally and a dual pulse at milestones of 108.
- Manual tally fallback, undo for the latest manual tally, and a confirmation-protected reset for the current chant.
- A local activity dashboard for performed chant sessions and scheduled practice plans.
- Mala progress, session intentions, local-only practice reminders, and an on-device streak/rhythm summary.
- Optional device-synthesized meditation tone after each tally; it is off by default and has no audio file or network dependency.
- Practice controls for target count, haptics, meditation tone, whisper/noise sensitivity, and local scheduled-practice actions.
- No ad, analytics, telemetry, account sign-in, cloud sync, or network-storage dependency.
- All tally and session data stays only in the local Room/SQLite database on the device.

## Run

Open this folder in Android Studio (Ladybug or newer), complete the Gradle sync, then run the `app` configuration on an Android 8.0+ device. Grant microphone access before starting voice tracking.

## Resetting a chant

When a practice has one or more counts, choose **Reset current chant** and confirm. It clears only the active session's tally and stops voice tracking; the intention, target, preferences, planned practices, and prior activities remain intact.

## Store assets

Actual emulator captures and a short Play-review demonstration of the user-initiated microphone foreground service are in [`play-store/`](play-store/):

- [`nirmalam-phone-practice.png`](play-store/nirmalam-phone-practice.png)
- [`nirmalam-tablet-practice.png`](play-store/nirmalam-tablet-practice.png)
- [`nirmalam-foreground-microphone-demo.mp4`](play-store/nirmalam-foreground-microphone-demo.mp4)

## Before release

1. Supply and validate a trained bundled TFLite chant classifier. Never upload raw audio.
2. Optionally add licensed temple-bell/singing-bowl assets for additional feedback choices.
3. Add encrypted local backup/export and Room migrations as schema versions evolve. Exports should only be created by an explicit user action and never uploaded by the app.

## Local chant classifier

The app now loads `app/src/main/assets/chant_classifier.tflite` when one is supplied. The expected model input is 1,600 mono PCM samples normalized to `float32`, and the second output score is treated as the chant confidence. Without this model, the app uses the adjustable local whisper/noise threshold; it never sends audio off-device.
