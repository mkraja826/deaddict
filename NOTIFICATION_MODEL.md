# Notification Model

Local notifications cover check-ins, risk periods, bedtime, near-limit warnings, weekly reports, and focus completion. WorkManager restores schedules after reboot and handles timezone changes.

FCM is reserved for security, accountability, subscription, synchronization, and important regional safety content. Tokens refresh safely and are revoked at logout. Quiet hours, channels, rate limits, and private default copy are mandatory.

## Implemented locally

- Runtime permission is requested only when the user enables reminders.
- Separate channels exist for check-ins, risk reminders, reports, focus completion, and security.
- Daily check-in uses unique periodic WorkManager work with local-time initial delay.
- Default quiet hours are 22:00–07:00 and a daily notification has a 20-hour minimum gap.
- Reboot, timezone, and manual time changes reschedule enabled work.
- Visible copy is private and excludes program or habit names.

FCM is intentionally disabled until Firebase configuration is available.
