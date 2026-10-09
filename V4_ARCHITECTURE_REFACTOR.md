# Nirmalam Chant V4 - Compose architecture refactor

## Changes
- `MainActivity.kt` now handles Android activity lifecycle, runtime permission requests, and foreground service start/stop only.
- `ui/screens/ChantHome.kt` owns navigation state, dialogs, and shared visual components.
- `ui/screens/PracticeScreen.kt` owns the chant count, voice controls, intention, and Focus entry UI.
- `ui/screens/JourneyScreen.kt` owns plans and completed history UI.
- `ui/screens/SettingsScreen.kt` owns tone, goal, haptics, sensitivity, and privacy UI.
- Existing Room entities, repository, ViewModel, microphone service, and calibration algorithm are unchanged.

## Important limits
- Refactor is source-level only; this environment has no Android SDK/Gradle binary for assembling an APK.
- The five-example calibration still uses the V4 acoustic matcher; no pretrained embedding model has been added.
- Please run `:app:assembleDebug` and check Practice / Journey / Settings, focus mode, permissions, reminders, session persistence, and voice calibration on a physical device.
- No Room schema or app version changes were made.
