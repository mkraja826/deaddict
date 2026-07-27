# Known Limitations

- The current milestone is an advanced local-first MVP with authenticated cloud upload and restore; it is not yet a production release.
- Supabase synchronization currently supports upserts for programs, tracking events, and Rescue sessions. Delete/tombstone propagation, account switching, granular conflict resolution, and physical-device offline/reconnect validation remain.
- Cloud restore protects unsynced `LOCAL_ONLY` and `PENDING` records and preserves private tracking notes, but broader multi-device conflict behavior still requires integration testing.
- The dedicated DeAddict Supabase project, least-privilege table grants, own-row RLS policies, and tracking-trigger schema are deployed. Google OAuth credentials and end-to-end provider sign-in remain unverified.
- Editable recovery-plan goals remain; tracking entry types are implemented.
- Compose UI has compiled but has not yet received the full emulator/device visual and accessibility matrix.
- Digital usage estimates are derived from Android foreground events and may vary by device; persisted trends, warnings, and focus sessions remain.
- Rescue regional emergency resources are not configured yet; the current escalation uses country-neutral professional and emergency guidance.
- Daily local check-ins are implemented. Risk-period, bedtime, weekly report, near-limit, focus-completion, and FCM delivery remain.
- Insights currently cover seven days; long-term reports, charts, comparison ranges, and export-ready report layouts remain.
- Data export, per-program deletion, discreet launcher identity, granular accountability sharing, and cloud/account deletion remain.
- Billing uses the `deaddict_plus` subscription product, but Play Console base plans and secure backend purchase verification/acknowledgement are not configured. Unverified purchases never grant Plus.
- GitHub Actions verifies JVM tests, Android lint, debug and minified release builds, reports, and APK artifacts. Broader usage-estimation accuracy still requires physical-device sampling across OEMs.
- Hindi and Telugu currently cover primary navigation and critical safety/privacy/billing boundaries, not the complete interface. Translations still require professional review, and no market beyond India is enabled.
- The privacy-boundary Compose test and Room migration/repository tests pass on a Pixel 7 API 35 emulator. TalkBack, large-text, reduced-motion, contrast, RTL, and wider API/device checks remain manual release gates.
- The generated release APK is unsigned. Signing, Play App Signing, closed-track upload, tester feedback, crash/ANR monitoring, dependency review, and clinical safety review remain.
- UsageStatsManager can estimate app-level activity only; it cannot access messages, passwords, photos, searches, screen content, or exact short-form-video activity.
- DeAddict will not promise unbreakable blocking or provide medical treatment.
