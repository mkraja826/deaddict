# Known Limitations

- The current milestone contains the application shell and program domain only.
- Hosted synchronization is not implemented; Rescue, monitoring, notifications, billing, and localization are partial.
- A dedicated DeAddict Supabase project and Google OAuth credentials are not configured; hosted authentication and synchronization cannot yet run.
- Hosted schema and RLS are verified; Google OAuth provider sign-in remains intentionally deferred.
- Editable recovery-plan goals remain; tracking entry types are implemented.
- Compose UI has compiled but has not yet received emulator/device visual and accessibility QA.
- Digital usage estimates are derived from Android foreground events and may vary by device; persisted trends, warnings, and focus sessions remain.
- Rescue regional emergency resources are not configured yet; the current escalation uses country-neutral professional and emergency guidance.
- Daily local check-ins are implemented. Risk-period, bedtime, weekly report, near-limit, focus-completion, and FCM delivery remain.
- Insights currently cover seven days; long-term reports, charts, comparison ranges, and export-ready report layouts remain.
- Data export, per-program deletion, discreet launcher identity, granular accountability sharing, and cloud/account deletion remain.
- Billing uses the `deaddict_plus` subscription product, but Play Console base plans are not configured and secure backend purchase verification/acknowledgement is intentionally unavailable while cloud services are disconnected. Unverified purchases never grant Plus.
- The current JVM suite passes completely; broader usage-estimation accuracy still requires physical-device sampling across OEMs.
- Hindi and Telugu currently cover primary navigation and critical safety/privacy/billing boundaries, not the complete interface. Translations still require professional review, and no market beyond India is enabled.
- The privacy-boundary Compose test and Room migration/repository tests pass on a Pixel 7 API 35 emulator. TalkBack, large-text, reduced-motion, contrast, RTL, and wider API/device checks remain manual release gates.
- The generated release APK is unsigned. Signing, Play App Signing, closed-track upload, tester feedback, crash/ANR monitoring, dependency review, and clinical safety review remain.
- UsageStatsManager can estimate app-level activity only; it cannot access messages, passwords, photos, searches, screen content, or exact short-form-video activity.
- DeAddict will not promise unbreakable blocking or provide medical treatment.
