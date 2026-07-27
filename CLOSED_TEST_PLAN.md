# DeAddict Closed-Test Plan

## Entry gates

- Create a signed Android App Bundle with Play App Signing.
- Configure Google OAuth, `deaddict_plus` base plans, secure purchase verification and acknowledgement, privacy-policy and deletion URLs, Data Safety answers, and India store listing.
- Obtain clinical review of Tier C safety language and professional review of Hindi and Telugu.
- Run unit, Room instrumentation, Compose instrumentation, lint, and release builds from a clean checkout.

## Test cohort

- Start with internal engineering and safety reviewers, then a small India closed-test cohort.
- Include Android API 26, 29, 33, and 36; low-memory hardware; small and large screens; English, Hindi, and Telugu; and at least one RTL system locale.
- Include TalkBack, 200% font scaling, display scaling, reduced-motion preference, dark mode, offline mode, interrupted purchases, reboot, timezone changes, and denied/revoked permissions.

## Critical journeys

- Private guest onboarding, program activation, every tracking type, seven-day insights, complete Rescue, Tier C escalation, notifications, local deletion, biometric relock, screenshot protection, language switching, purchase/restore/pending/cancel/refund flows, and cloud-consent boundaries.
- Confirm Rescue, safety, deletion, biometric protection, and essential privacy remain available without Plus and without network access.
- Confirm no sensitive program, journal, trigger, usage, or Rescue content appears in notifications, logs, analytics, recent-app previews, backups, or another account.

## Exit gates

- Zero open critical/high security, privacy, safety, billing, data-loss, accessibility, or crash defects.
- Resolve tester feedback; review Play pre-launch, crash, and ANR reports; document accepted lower-severity issues.
- Verify purchase lifecycle and entitlement revocation, account/data deletion, localized store/legal content, and signed production artifact provenance.
