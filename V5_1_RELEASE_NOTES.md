# Nirmalam Chant V5.1 - Manual Chanting Refinements

## Changes
- Gentle milestone notification for manual taps reaching 27, 54, and 108; automatically disappears after 2.5 seconds without blocking chanting with a dialog.
- Less intrusive 12 ms optional tap haptics, and a restrained two-pulse acknowledgement at milestones. Existing Haptics toggle continues to control both.
- Journey dashboard summary with locally recorded completed chants, sessions, streak, and last-seven-day sessions.
- All data stored using the pre-existing Room database; active practice is recovered by MainViewModel using `getOrCreateActiveSession()` and the `observeCount()` flow on relaunch. No database schema change or migration is needed.
- Preserves the V5 visual design, focus mode, manual counter, undo, and local plans. No voice recognition or microphone permission.

## Limitations / testing
- No Android SDK/Gradle wrapper executable available here; not compiled or tested on-device. Build with Android Studio `:app:assembleDebug`.
- Exercise 26->27, 53->54, 107->108, Undo, reset, rotation, force-stop/relaunch, and Focus Mode.
- Milestone notice follows tap requests optimistically, rather than waiting for the database write confirmation. Very rapid taps at a milestone may produce repeated/early acknowledgement if the count has not recomposed yet; persistent counts remain governed by Room.
- Journey statistics summarize completed activities (not in-progress count). Current streak logic counts consecutive days ending today; it may show zero in the morning before today's practice.
- When upgrading an existing project folder from a legacy V4, run CLEANUP_V4_VOICE.ps1 to remove orphaned classifier files.
