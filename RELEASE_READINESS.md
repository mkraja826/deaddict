# DeAddict Release Readiness

Last updated: 2026-07-29

## Current status

DeAddict has a strong advanced-MVP engineering foundation but is not ready for public commercial release. Build success and foundation completion must not be interpreted as validated product, safety, billing, device, legal, or operational readiness.

Current planning estimate for public commercial readiness: **45%**.

## Implemented foundations

- Local-first recovery tracking with Room persistence.
- Durable Supabase upload, restore, retries, dead-letter handling, and delete tombstones for current aggregates.
- Account-deletion coordination and privacy controls.
- Rescue, goals, insights, focus sessions, usage estimates, export/import validation, localization infrastructure, and accessibility helpers.
- Google Play Billing client foundation that refuses unverified entitlements.
- Multi-addiction selected-program layer and Rook V1.
- CI for JVM tests, Android lint, debug build, and minified release build.

## Product architecture gates

- Implement permanent Recovery Tracks with stable IDs, owner scope, roles, lifecycle, and history.
- Migrate tracking and Rescue from `programId` ownership to `recoveryTrackId`.
- Implement unified multi-track daily check-in.
- Replace generic Home progress with goal-aware persisted progress.
- Add explainable cross-track insights.
- Add Rook feedback, repetition control, reviewed content, and safer defaults.
- Replace selected-list ordering with explicit track context.

## Credential and console gates

These require owner-controlled credentials or external consoles:

- Verify Google OAuth in Google Cloud, Supabase, and the release-signed Android app.
- Configure Play Console products, base plans, offers, license testers, and backend purchase verification.
- Configure Real-Time Developer Notifications and entitlement lifecycle.
- Configure FCM credentials and token lifecycle if push delivery is enabled.
- Create and protect production signing/upload credentials and configure Play App Signing.
- Provide production privacy-policy, terms, support, account-deletion, and Data Safety URLs.

## Device and destructive-test gates

- Complete account deletion with a disposable account and verify remote and local cleanup.
- Test account switching and verify owner isolation.
- Test two-device offline creation, reconnection, conflict resolution, retries, tombstones, and restore.
- Validate notifications across reboot, timezone changes, quiet hours, permission denial, and battery restrictions.
- Validate biometric lock, screenshot blocking, recent-preview protection, TalkBack, large text, reduced motion, contrast, and RTL.
- Validate usage estimates across representative Android devices and OEMs.
- Exercise purchase, pending, renewal, cancellation, expiration, grace period, account hold, restore, refund, and revocation.

## Safety, legal, and localization gates

- Professional review of high-risk guidance, withdrawal warnings, Rescue escalation, and regional resources.
- Legal review of privacy, consent, deletion, subscription, and market-specific language.
- Professional translation and cultural review of all enabled languages, including Rook.
- Store-listing review for medical-treatment, diagnosis, relapse-prevention, or guaranteed-outcome implications.

## Operations gates

- Configure crash and ANR monitoring.
- Add privacy-safe sync and billing health diagnostics.
- Assign support, incident-response, rollback, and release-monitoring owners.
- Document staged rollout and emergency rollback.
- Provide a controlled way to disable unsafe content without disabling Rescue, privacy controls, or account deletion.

## Closed-test exit criteria

- No open severity-1 or severity-2 defects.
- Crash-free and ANR metrics meet the team threshold.
- All critical journeys pass on the supported API and device matrix.
- Room migration, account switching, sync, restore, billing, and account deletion pass production-like tests.
- Safety, legal, privacy, security, and localization reviews are complete for the enabled market.
- Support and rollback procedures are operational.

## Source of truth

Detailed gates and product boundaries live in:

- `docs/current-architecture.md`
- `docs/data-model.md`
- `docs/sync-contract.md`
- `docs/safety-boundaries.md`
- `docs/release-gates.md`
- `docs/product-scope-v1.md`
