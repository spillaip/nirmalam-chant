# Nirmalam Chant V5.7 - Optional Practice Companions

Built on V5.6. Added `ui/screens/RitualExperience.kt` and integrated it into the Practice screen.

- **Breathing Rhythm Companion:** opt-in visual in/out pacing, configurable 3-8 seconds per half-cycle, no holds. Visual only; never changes counts or requires a microphone.
- **Guided Sankalpa Ritual:** optional opening dialog with intention presets; saves to the existing Room session intention through existing callbacks. Skippable.
- **Mindfulness Check-in:** optional before/after 1-5 self-report values saved only to app-private SharedPreferences with date/phase/chant profile; limited to 500 events. They are subjective reflections, not clinical measures.
- **Personal Chant Rituals:** save up to 10 named ordered sequences using the existing five chant profiles. A step can only advance after reaching its chant target. The user explicitly starts, advances, and finishes; no background automation or silent chant switching. Each selected chant keeps its existing independent Room session and interval. Switching profiles pauses the timer through the existing ChantHome callback.

## Architecture and limitations

This release does not change the Room schema or existing manual/timed count code, session history, Journey, or Focus Mode logic. Ritual definitions and mood check-ins use separate app-private SharedPreferences, so uninstalling/clearing app storage deletes them. There is not yet an analytics view of mood history or a cross-restart active-ritual resume function. Users can restart their saved rituals; chant progress remains separately persisted in Room. The guided opening is optional, not automatically forced at session start. Breathing guidance is not displayed inside full-screen Focus Mode.

## Test on physical device

1. Open Breathing Companion; toggle on/off; adjust interval; confirm counting is unchanged.
2. Open Sankalpa; save or skip; restart app and verify saved current-session intention.
3. Submit before/after ratings; verify UI response and app restarts without error.
4. Configure two chant profiles, save an ordered ritual, start it, complete first target, advance, verify target/count/timer correspond to the next profile. Pause a running timer before advancing.
5. Confirm Reset, Undo, milestones, Journey, Circle/Mala and auto-count in Focus Mode still work.

No Android Gradle compilation, emulator test, or physical-device test was performed in this environment. Package structure and static checks are not substitutes for those tests.
