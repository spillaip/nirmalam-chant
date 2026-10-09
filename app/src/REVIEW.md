# Nirmalam Chant — source review and targeted patch

Scope: uploaded `main/` Android source directory only (no Gradle wrapper/build files, tests, or emulator APK).

## Changes applied
- Preserve Practice/Journey/Settings section, focus mode and intention across activity recreation using `rememberSaveable`.
- Route "Start" on a Journey plan to Practice and stop listening before swapping sessions.
- Stop voice tracking before beginning a new session; prevent old microphone service from counting into the previous session.
- Display correct Start/Pause listening action in focus view.
- Avoid reporting completed practices as incomplete by filtering Journey history to ended sessions.
- Refresh target/count/undo state on session activation and prevent Undo from being offered for already-finished practices.
- Use calendar-day rather than fixed 24-hour offsets to postpone reminders across daylight-saving transitions.
- Replace continuously animated full-screen background with a static background to reduce redraw/battery load.
- Guard `AudioRecord.stop()` for recording state and cancel the tracking service coroutine scope when destroyed.
- Change inaccurate "Today's chants" label to "Current practice".

## Further issues requiring full-project build/device validation
1. Detection is threshold-based when the TensorFlow Lite asset is absent and can classify non-chant noise as chanting. Validate with recordings in diverse environments.
2. Background microphone service needs real Android 12–15 lifecycle/permission and exceptional-stop tests. It currently may not gracefully communicate all capture failures.
3. Saved intention is not initialized from the persisted session on screen reload; add a session state flow.
4. `getOrCreateActiveSession` / `beginNextSession` are not transactional and may race across service/UI callers; add DAO @Transaction-based session control.
5. When a completed session is reopened after restart, a new active session is created automatically; consider a clear completion-to-next-practice UX.
6. Reminder notifications require Android notification permission; permission denial should yield explicit scheduling-only feedback.
7. Room schema migrations and timestamp conversion need migration tests with real legacy v1-v4 database files.
8. `SettingsCard` text input is updated on each keystroke, potentially creating unpredictable UX; prefer explicit Save button and error state.
9. `ChantHome` takes many positional callbacks/params; break into screen composables and a single screen-state model.
10. `LazyColumn` practice layout uses one very large item; split discrete cards into independently measured lazy items for better scroll responsiveness.

## Five innovative enhancements
1. **Adaptive chanting coach:** optional on-device model learns user cadence and recommends microphone sensitivity without uploading raw audio.
2. **Mala gesture mode:** full-screen swipe/tap tally with TalkBack semantics and optional wearable button support.
3. **Intelligent practice journeys:** mood/intention based, opt-in 7/21/40-day journeys with flexible targets and reflection prompts.
4. **Privacy-first insights:** weekly trend charts, rhythm stability, distraction-free streaks, and local encrypted export.
5. **Multilingual chant companion:** Telugu, Sanskrit, Hindi and English transliteration, pronunciation practice, and offline chants library.

## Verification limitations
Static source edits and text assertions only. Android/Gradle build, instrumentation tests and emulator tests **not run** because `main.zip` lacks `settings.gradle`, root/module `build.gradle`, Gradle wrapper and tests. Integrate the files in the full repository and run `./gradlew testDebugUnitTest lintDebug assembleDebug` plus device voice/background checks.
