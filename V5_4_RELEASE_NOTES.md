# Nirmalam Chant V5.4 - Personal chant profiles (source preview)

- Digital Mala > Choose your chant: create and edit up to five persistent chant profiles.
- Each profile stores a name, target and auto-advance interval (1-3600 seconds).
- Selecting a profile uses its independent in-progress Room session; switching pauses the running timer.
- Each profile has independent counts and stored history, and uses the same manual tally and Undo path as before.
- Profile settings are stored in Room (schema 4 to 5 migration); current profile selection is remembered in private app preferences.
- The timer is opt-in and does not run while backgrounded, on other tabs, or in Focus Mode.
- Editing a profile's target changes the target for *new* sessions, not an in-progress session.
- No microphone permissions or voice-recognition implementation.

## Critical testing
1. Back up existing app data; install as an update, **do not clear storage**, check old Journey history.
2. Open Digital Mala, create two profiles (e.g., Om / 3s / 108, Shivaya / 6s / 54).
3. Count 4 on Om, switch to Shivaya and count 2, switch back: Om should remain 4.
4. Start Om timer; switch profile: timer must pause. Start Shivaya timer and verify its own interval.
5. Restart app and check both profile counts and the selection.
6. Finish one target and confirm it appears in Journey under its chant title.
7. Test migration on a device with a real V5.3 database. Verify build in Android Studio.

## Known limits
- No Android SDK/Gradle compile or instrumented test has been executed in this environment.
- Existing pre-profile sessions remain in Journey as legacy practice, not retroactively assigned to profiles.
- The profile management view is shown within Digital Mala, while the existing circular counter remains available.
