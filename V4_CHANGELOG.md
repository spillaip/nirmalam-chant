# Nirmalam Chant V4 — Practice UX and adaptive acoustic baseline

## Implemented
- Redesigned Practice home: calmer hierarchy, single large counter, progress indicator, prominent voice and manual actions, Undo and Focus controls.
- Added a five-example chant-training guide before invoking microphone calibration.
- Updated the detector prompts and template readiness to five examples.
- Removed V3's brittle first-example strict similarity rejection during enrollment.
- Changed runtime template comparison to an adaptive threshold based on enrollment variability, with a bounded threshold and a median-of-nearest-three score.
- Existing Practice/Journey/Settings navigation and data store retained.

## Important limitation
This is **not** a pretrained embedding model, speech transcription, or verified mantra recognition. It is an experimental acoustic matcher. It can miss continuous chants and can mistake similar sounding speech for a chant. Do not treat its count as validated. Evaluate under quiet/noisy and negative-speech conditions on a physical Android device. No APK compilation was performed in this environment.

## Validation checklist
1. Assemble debug in Android Studio.
2. Install over existing app; check that Room data, intentions and history persist.
3. Tap Teach / retrain; record five repetitions with short pauses.
4. Test 10 known chants, 10 unrelated phrases, varied pace, noise, and silence.
5. Verify Stop, background/foreground, manual +1, Undo, reset and completion.
6. If accuracy remains low, integrate and evaluate a real pretrained audio encoder before release.
