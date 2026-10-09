# V5 manual-only: fix for stale voice recognition files

## Root cause
The V5 source archive intentionally contains no LocalChantClassifier.kt, but extracting the V5 ZIP on top of an existing V4 working directory does not delete files that are absent from the ZIP. Android Studio still compiles those stale files. TensorFlow Lite was removed from the V5 app dependencies, so the old files fail compilation.

## Windows upgrade steps
1. Close the running app; keep a backup of your existing Android Studio project.
2. Extract this ZIP into the project directory, preserving its `app/`, `gradle/` structure.
3. From the project root run `powershell -ExecutionPolicy Bypass -File .\CLEANUP_V4_VOICE.ps1`, or delete the four obsolete files named in the script manually.
4. Sync Gradle, then run `gradlew.bat clean :app:assembleDebug` (or Android Studio > Build > Clean Project, then Rebuild Project).
5. If your project still contains legacy voice resources or classes not listed here, inspect/remove them only if they are no longer referenced.

## Safer fresh-folder option
Extract V5 into a new folder and open it as an Android Studio project. This avoids carrying stale source files forward. This ZIP may not include your original Gradle wrapper launchers, so copy `gradlew.bat`, `gradlew` and `gradle/wrapper/gradle-wrapper.jar` from your existing project if needed.

## Data safety
This cleanup deletes only obsolete Kotlin *source files*, not Room tables, app data or local databases. Do not clear the Android app's storage or uninstall the app unless you intend to remove local practice history.

## Validation
ZIP/static checks only; not yet Android-compiled or device-tested.
