# Nirmalam Chant V5.6 — Optional Practice Enhancements

Based on V5.5 Focus Timer Fix; retains Circle/Mala, profile selection, Room counts and Focus timer behavior.

## Features
- **Smart Mala Journey**: totals of completed chants divided into 108-bead malas, plus partial progress. Counts use all completed sessions, including legacy sessions; in-progress sessions are excluded.
- **Smart Chant Pace**: opt-in Gentle (6s), Steady (3s), Deep focus (2s) presets for the selected profile; disabled during auto count. Existing custom intervals remain editable in chant profile.
- **Spiritual Consistency Calendar**: rolling 30-day grid, with daily shade proportional to chant counts; locally stored completed sessions only.
- **Accuracy**: separate Room Flow for all completed sessions feeds Journey stats, streaks, insights and new calendar instead of the limited recent-history list. The short history list remains bounded for display.

## Device test checklist
1. Build :app:assembleDebug and upgrade existing app without clearing data.
2. Confirm all 5 chant profiles, independent timers and Focus Mode work as before.
3. Choose a preset, verify interval changes; check timer never starts automatically.
4. Complete 108 chants; verify 1 mala in Journey and calendar changes for today.
5. Verify recent history and 7-/30-day summary, plus Undo/Reset and app restart.
6. Test different Android display sizes and TalkBack descriptions for calendar squares.

## Limits
- Counts are grouped by session **start date**, not tally dates; overnight practices appear on their start date.
- The overall mala total combines chants across all profiles. No per-profile filtering yet.
- No cloud or voice recognition.
- Android build and physical-device validation required.
