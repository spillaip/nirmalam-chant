# Nirmalam Chant V5 — Manual Mala

- Removed voice recognition, calibration, microphone service, RECORD_AUDIO and foreground microphone permissions, and TensorFlow Lite dependency.
- Redesigned the Practice screen around a large tappable circular count and progress ring.
- Retained manual count, Undo, reset confirmation, focus mode, intentions, Journey/history and reminder scheduling.
- Consolidated navigation into Practice / Journey / Settings.
- Removed noise sensitivity from Settings; retained haptics, tone and chant-target presets.
- Kept Room database schemas and migrations unchanged to protect existing history.

## Installation
Unzip into your existing Android Studio project, replacing corresponding project files. Build with `:app:assembleDebug`. No Gradle wrapper JAR is in the supplied sources, so run through Android Studio or your installed Gradle.

## Validation limitations
Static integrity checks only; Android SDK/Gradle builds and physical device tests are still required. Verify count, Undo, completion, Focus Mode, history, reminders, and upgrade from the existing app without clearing data.
