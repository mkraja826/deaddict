# Known Limitations

Last updated: 2026-07-29

- The current application is an advanced local-first MVP, not a production release.
- Multi-addiction V1 uses active-program records and selected-list ordering. It does not yet provide permanent Recovery Track identities, primary/supporting persistence, pause/maintenance lifecycle, or historical restarted journeys.
- Tracking events and Rescue sessions reference `programId`, not a permanent `recoveryTrackId`.
- The current onboarding is primarily a program picker; it does not create a complete goal, baseline, trigger plan, privacy choice, Rook boundary, or first unified check-in.
- Home progress is largely generic supportive copy rather than goal-aware persisted progress.
- The Track screen is a one-track event logger, not a unified daily multi-track check-in.
- Cross-track insights and replacement-behavior analysis are not implemented.
- Rook has Direct, Brutal Banter, and Quiet modes with a high-risk override, but lacks feedback, repetition control, blocked templates, off-limits topics, a reviewed large catalogue, and a production-safe Direct default.
- Supabase synchronization supports upserts and idempotent deletes for programs, tracking events, and Rescue sessions. Recovery Tracks, goals, and daily check-ins are not synchronized because those aggregates do not yet exist.
- Cloud restore protects pending local records and private tracking notes, but broader two-device and account-switch behavior still requires integration testing.
- Production Google OAuth and full email OTP behavior remain unverified on release-signed devices.
- Account deletion requires a destructive disposable-account test covering remote rows, Room data, preferences, reminders, work requests, and authentication state.
- Rescue regional emergency resources are not configured. The current flow uses country-neutral guidance.
- Daily local reminders exist, but track-aware, risk-period, weekly, near-limit, and production FCM flows are incomplete.
- Play Billing uses `deaddict_plus`, but the client currently has no connected backend purchase verifier. Unverified purchases never grant Plus.
- Hindi and Telugu cover only part of the interface. Many user-facing strings remain hardcoded in English, and professional review is pending.
- CI runs JVM tests, lint, and debug/release builds, but does not currently execute the full emulator/instrumented matrix on every pull request.
- TalkBack, large text, reduced motion, contrast, RTL, wider API/device coverage, and OEM usage-estimate validation remain release gates.
- Crash, ANR, sync-health, and billing-health production observability are not yet configured.
- UsageStatsManager can estimate app-level foreground activity only. It cannot access messages, passwords, photos, searches, browsing content, screen contents, or exact short-form-video activity.
- DeAddict will not promise unbreakable blocking, diagnosis, medical treatment, detox management, or guaranteed outcomes.
