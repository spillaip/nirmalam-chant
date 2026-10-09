# V5.5 Focus Mode auto-counter fix

Fixed `ChantHome.kt` so entering or exiting Focus Mode no longer stops or resets the existing auto-increment coroutine. Focus Mode is a view-only transition, and the interval continues uninterrupted. Completing the target sets auto-running to false.

Existing safety behavior retained: switching chant profiles, leaving Practice, pressing Pause, Reset, app ON_STOP, and starting a new session cancel auto-counting. The timer remains opt-in and does not auto-start after app restart.

## Verify on Android device

1. Select a chant with 3-second interval. Press Start and wait for 2 counts.
2. Enter Focus Mode between ticks. Verify the count continues approximately every 3 seconds without restarting its interval.
3. Exit Focus Mode. Confirm the same uninterrupted cadence and no doubled increments.
4. Press Pause; verify increments cease. Check target completion and app backgrounding also stop timer.
5. Change chant profile while auto mode runs; verify old timer stops.

Source-only checks run here; Android SDK/Gradle build and physical-device tests were not available in this environment.
