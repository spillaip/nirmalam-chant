# V4 MainActivity compile fix

## Root cause
An extra `}` after the Practice section prematurely ended the `LazyColumn`'s `LazyListScope` around line 264. Subsequent calls to `item {}` and `items(...)` were then outside the lazy-list builder, triggering `Unresolved reference item`, invalid @Composable invocation, and cascading syntax errors around lines 266-310.

## Applied correction
Kept the `RhythmCard` item inside the Practice section, closed the Practice `if` block once, and left the Journey and Settings `item/items` builders inside the original `LazyColumn` lambda. Updated privacy copy to explicitly acknowledge that calibration stores acoustic features locally (not raw audio uploads).

## Architecture recommendation
MainActivity currently mixes activity permissions/service commands, Compose screen state, dashboard sections, dialogs, settings, and navigation in one very large function. Follow-up: extract `PracticeScreen`, `JourneyScreen`, `SettingsScreen`, `VoiceTrainingDialog` into separate UI files; route activity permission events through a narrow coordinator and use a typed UI state and callback/event interface. Keep `MainViewModel` as orchestration and Room as source of truth. This is recommended for maintainability, not required to fix this particular compile failure.

## Verification
Only static structure and ZIP integrity were checked in this workspace; the Android SDK/Gradle wrapper JAR are not present. Run `:app:assembleDebug` on your Android Studio machine and share the first new compiler error, if any.
