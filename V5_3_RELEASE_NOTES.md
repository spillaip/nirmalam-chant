# Nirmalam Chant V5.3 — Timed Manual Counter

## New optional feature

Practice now includes a **Timed chant counter** card. Enter a whole-number interval from **1–3600 seconds** (default: 3), then tap **Start**. Every elapsed interval triggers the same `onAdd` tally callback as tapping the circular counter, including Room persistence, haptics/tone, milestone messages, Digital Mala progress, and Journey data.

Tap **Pause** to stop. The first increment occurs after a full interval, not immediately. The timer stops automatically on reaching the practice target, moving away from Practice, entering Focus Mode, requesting reset, or when the app goes to the background. Running status is deliberately *not* persisted or resumed automatically after rotation/restart; the selected interval is retained across configuration changes.

Manual counting remains available while the timer runs. Tapping manually **adds an additional chant**; it does not postpone the next timer tick. Users should pause timed mode if they do not want this behavior. This interval timer does **not** detect actual chanting, and accuracy depends on how closely the chosen interval matches the user's pace.

## Build and test

Run `:app:assembleDebug` in Android Studio. Test: 3-second interval, Pause/Resume, manual tap while active, Undo, milestones, Reset, target completion, switch tabs, background app, rotation, session restoration, Digital Mala and Journey. Confirm timer does not auto-start on relaunch.

Full Android compile and device validation were not performed in this environment.
