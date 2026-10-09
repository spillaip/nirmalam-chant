# Nirmalam Chant V3 — personalized offline chant recognition

## What changed
- The previous speech/energy-triggered tally has been replaced. Voice counting is now disabled until calibration succeeds.
- On Practice, choose **Record / retrain my chant (3 repetitions)**. Chant each full repetition and pause briefly (~0.32 sec). The status label shows accepted examples. Use the same mantra three times at a similar pace.
- Tap **Start voice tracking** and repeat the chant, pausing between repetitions. The detector segments complete phrases and counts only if the segmented utterance matches at least 2 of the 3 saved acoustic templates.
- Templates are short feature sequences stored in private SharedPreferences. No raw audio is written to disk or transmitted. Retraining replaces the saved examples. Manual tapping still works.

## Implementation
- 16 kHz mono 16-bit AudioRecord; 20-ms analysis windows.
- RMS-based voice activity detection with adaptive ambient-noise floor and a ~320-ms silence boundary.
- Per-window normalized log energy, zero-crossing rate, waveform-change features; band-constrained DTW distance and length-ratio checks.
- Foreground microphone service and existing runtime permission flow are retained.
- No change to Room schema and no new network permission.

## Important limitations
- This is an **experimental personalized acoustic matching baseline**, not phoneme-aware speech recognition or a trained keyword-spotting model. Similar sounding words, music, background voices and reverberation may produce false positives or false negatives.
- Short chants without pauses and fast, continuous chanting may be undercounted. Environmental changes, microphone positioning and changes in pace can affect recognition. Users should test and calibrate in a quiet room and use manual count if matching fails.
- Thresholds are initial engineering estimates and have **not** been calibrated against real recordings. Collect consented, diverse positive and negative test examples before shipping broadly.
- Uses application-private preferences without Android backup; clearing app data removes templates.
- The Gradle wrapper launcher/JAR and Android SDK are not available in this package/runtime; no successful Android compilation or physical-device testing is claimed.

## Physical-device test cases
1. Open the app with no template and press Start: no voice tallies should occur; the UI directs calibration.
2. Train exactly three complete repetitions with quiet gaps and verify status counts 1/3, 2/3, 3/3.
3. Speak unrelated words, cough, clap, run music: voice count should remain stable.
4. Chant full repetitions at a normal pace with brief pauses: each valid isolated utterance should result in at most one count.
5. Interrupt/restart calibration, revoke microphone permission, background/foreground the app, lock the screen, and complete a target; verify microphone indicator and tracking state.
6. Confirm existing manual count, Undo, history and intention persistence still work.
