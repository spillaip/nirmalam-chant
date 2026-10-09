# Nirmalam Chant V5.1 - Reset dialog compilation fix

In `app/src/main/java/com/nirmalamgroup/nirmalamchant/ui/screens/ChantHome.kt`, a missing `showReset` Compose state declaration caused five unresolved-reference errors (lines 65, 68, 69, 94).

Added `var showReset by rememberSaveable { mutableStateOf(false) }` alongside other screen state. No other application logic was changed.

To integrate, replace the file or extract this archive into a clean project folder. Build with `:app:assembleDebug` in Android Studio. The archive has not been compiled in this environment.
