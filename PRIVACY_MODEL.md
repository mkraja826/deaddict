# Privacy Model

Guest and local-only modes are first-class. Cloud backup, usage monitoring, analytics, and accountability sharing are separately controlled and default conservatively.

Users can export data and delete a program, local data, cloud data, or the account. Screenshot blocking, recent-app protection, biometric lock, and discreet presentation are user controls. Notification text is private by default.

Digital usage access is optional and managed in Android settings. DeAddict processes app-level foreground statistics locally. It does not access messages, passwords, photos, searches, screen contents, browsing content, or exact in-app feature activity.

## Implemented controls

- Biometric or device-credential app lock.
- Optional `FLAG_SECURE` protection for screenshots and recent-app previews.
- Usage monitoring can be disabled independently of Android’s system permission.
- Anonymous analytics are off by default; no analytics SDK is currently initialized.
- Local recovery data deletion requires explicit confirmation and clears Room records while retaining privacy preferences.
