# DeAddict Release Readiness

Last updated: 2026-07-28

## Engineering status

The application foundation is merge-ready once the latest Android CI run succeeds. Implemented areas include local-first recovery tracking, Room persistence, authenticated Supabase sync, durable outbox processing, cloud restore, deletion tombstones, account deletion coordination, privacy controls, billing foundations, notifications, insights, recovery goals, focus sessions, export/import validation, accessibility helpers, localization infrastructure, and CI verification.

## Merge gates

- Android CI passes JVM tests, lint, debug build, and minified release build.
- No unresolved pull-request review threads.
- Pull request remains mergeable against `main`.
- Documentation reflects implemented behavior and known boundaries.

## Credential and console gates

These require owner-controlled credentials or external consoles:

- Configure and verify Google OAuth in Google Cloud, Supabase, and the Android app.
- Configure Play Console subscription products, base plans, offers, license testers, and backend purchase verification.
- Configure Firebase Cloud Messaging credentials and token lifecycle.
- Create the production signing key, upload key, signed Android App Bundle, and Play App Signing configuration.
- Provide production privacy-policy, terms, support, and data-safety URLs.

## Device and destructive-test gates

These must be performed on release-signed builds and disposable accounts:

- Complete account deletion and verify remote rows, local Room data, preferences, reminders, work requests, and authentication state are removed.
- Test account switching between two users and confirm no local data crosses account boundaries.
- Test offline creation, reconnection, conflict resolution, retries, tombstones, and restore behavior across two devices.
- Validate notifications across reboot, timezone changes, quiet hours, permission denial, and battery restrictions.
- Validate biometric lock, screenshot blocking, recent-app preview protection, TalkBack, large text, reduced motion, and RTL layouts.
- Validate usage-access estimates against representative Android devices and OEMs.
- Exercise Play Billing purchase, pending, cancellation, renewal, grace-period, restore, and entitlement-revocation flows.

## Clinical, legal, and localization gates

- Clinical review of recovery guidance, withdrawal warnings, safety escalation, and regional resources.
- Legal review of privacy, consent, deletion, subscription, and worldwide-release language.
- Professional review of translations and regional safety-resource accuracy.
- Store-listing review for claims that could be interpreted as medical treatment or guaranteed outcomes.

## Closed-test exit criteria

- No open severity-1 or severity-2 defects.
- Crash-free and ANR metrics meet the team’s release threshold.
- All critical user journeys pass on the supported API and device matrix.
- Data deletion and privacy controls are verified end to end.
- Billing and entitlement verification are confirmed against production-like services.
- Support, incident response, rollback, and release-monitoring owners are assigned.

## Current boundary

Code-only implementation cannot complete credential provisioning, console configuration, signed-device validation, destructive production-like testing, clinical/legal approval, or professional translation review. These remain explicit release gates rather than assumed completion.
