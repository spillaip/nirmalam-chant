# Nirmalam Chant V5.5 - Chant-first UX

Built from V5.4 source with the user's corrected JourneyScreen behavior preserved.

## Changes
- Practice header now identifies the active chant, seconds-per-chant, and target.
- Change opens a simple selection dialog (up to five profiles), with Edit/Add actions.
- Circle/Mala is an explicit two-option segmented control; progress is shared.
- Timer is a compact Start/Pause card that displays the profile interval and current state. Interval edits remain in the profile editor.
- Existing counting, profile switching, history, reset, Sankalpa, Focus Mode, and milestones remain on the original code paths.
- Journey imports cleaned; no changes to the existing capped-history calculation or Room schema.
- Settings heading visually aligned with the Practice design.

## Important validation
- This environment does not have a working Android SDK / Gradle wrapper build; `:app:assembleDebug` was not run.
- Test changing chants while a timer is running, switching Circle/Mala, fast taps, undo, reset, completion, and Journey.
- Existing Journey 7/30-day metrics are derived from the latest 30 completed sessions, not an unrestricted historical query.
- Unzip into a clean source directory or remove old obsolete Android sources before merging; preserve your existing signing config and any untracked local files separately.
