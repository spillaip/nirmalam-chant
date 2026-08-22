# Privacy commitment

Nirmalam Chant is local-only by design.

- It does not connect to cloud storage, user accounts, external databases, or sync services.
- It does not include advertising, analytics, telemetry, trackers, or third-party marketing SDKs.
- Microphone buffers are processed in memory on the device and are not stored or transmitted.
- Chant sessions and tallies are stored solely in the app's Room/SQLite database on the device.
- Any future backup or export must be initiated by the user and written to a destination the user explicitly chooses; the app must not upload it.
