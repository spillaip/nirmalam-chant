# Nirmalam Chant V5.2 — Optional practice enhancements

- Digital Mala: an optional 108-bead ring switched on from Practice. The existing circular counter remains the default. Both use the same manual tally callback, so Undo and persistence are unchanged.
- Daily Sankalpa: the existing Room-backed session intention is presented as a collapsible, optional card with save, clear, and preset actions. No Room migration required.
- Practice Insights: seven-day bar visualization and seven-/thirty-day chant totals using the already available completed-session stream, with an empty state.

Important: completed activity data is currently capped by the DAO query to 30 latest completed sessions. Insights are therefore described as being based on the latest 30 completed sessions, not full lifetime totals. For comprehensive monthly trends in a future version, add a dedicated SQL aggregate query across all sessions.

The existing V5.1 stable manual counter, haptics, milestones, focus mode, history and navigation are preserved. No voice/audio permissions introduced.

Build verification: Android SDK/Gradle tooling unavailable here. The ZIP was integrity-tested and basic source checks completed. Compile and test on Android Studio and physical device before release.

Suggested tests: change Digital Mala on/off mid-session; tap to 27 and Undo; complete at 108; save/clear Sankalpa and restart; verify 7/30-day counts against recent completed sessions; verify Focus Mode retains original counter.
