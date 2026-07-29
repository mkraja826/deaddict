# DeAddict Production Release Gates

Last updated: 2026-07-29

## Purpose

A successful build is not the same as production readiness. This checklist separates code completion from credential, device, safety, legal, commercial, and operational verification.

## Engineering gates

- Permanent Recovery Track data model implemented.
- Room migrations preserve existing data.
- Unified multi-track daily check-in is transactional and offline-first.
- Home displays persisted goal-aware progress.
- Rescue is track-aware and safe offline.
- Cross-track insights are explainable and non-diagnostic.
- Rook feedback and safety overrides are implemented.
- Instrumented tests run in CI.
- No open severity-1 or severity-2 defects.

## Data and synchronization gates

- Account switching never exposes another owner’s records.
- Guest-to-account migration is transactional.
- Two-device offline/reconnect tests pass.
- Restore does not overwrite unsynced local changes.
- Delete tombstones prevent stale recreation.
- Private notes remain within the documented boundary.
- Large-history restore and migration tests pass.

## Authentication gates

- Production Google OAuth is verified on release-signed devices.
- Email OTP deliverability and verification are tested.
- Session expiry, sign-out, and revocation are tested.
- Disposable-account deletion is tested end to end.

## Billing gates

- Play Console base plans and offers are configured.
- Purchases are verified server-side.
- Purchases are acknowledged correctly.
- Real-Time Developer Notifications are processed.
- Pending, renewal, cancellation, expiration, grace period, account hold, refund, and revocation are tested.
- Unverified purchases never grant Plus.
- Cancellation never deletes local recovery data.

## Notification gates

- Notification permission denial is handled.
- Reboot and timezone rescheduling are tested.
- Quiet hours and rate limits work.
- Paused tracks receive no reminders.
- Lock-screen wording is neutral by default.
- Production FCM token and delivery lifecycle is verified if push is enabled.

## Privacy and security gates

- Privacy policy matches implemented behavior.
- Google Play Data Safety answers match all SDKs and data flows.
- Health-app declarations are complete.
- Supabase RLS and least-privilege grants receive independent review.
- Sensitive recovery content is absent from logs, analytics, and crash breadcrumbs.
- Export and deletion journeys are verified.
- Security and dependency reviews are complete.

## Safety gates

- High-risk recovery guidance receives professional review.
- Regional safety resources are verified.
- Country-neutral offline fallback exists.
- Immediate-danger flow overrides Rook and commercial UI.
- No detox, taper, dosage, diagnosis, or guaranteed-outcome claims appear.

## Accessibility and device gates

- TalkBack critical journeys pass.
- Maximum supported text scaling remains usable.
- Reduced motion and color-independent states are supported.
- RTL layouts are verified where enabled.
- Representative API levels, OEMs, tablets, and foldables are tested.
- Usage estimates are sampled against real devices.

## Operations gates

- Crash and ANR monitoring are active.
- Sync, billing, and account-deletion failures are measurable.
- Incident-response and support owners are assigned.
- Rollback and staged rollout procedures are documented.
- Unsafe content can be remotely disabled without disabling Rescue or account deletion.

## Closed-beta exit

- Critical journeys pass on the supported matrix.
- Crash-free and ANR metrics meet the release threshold.
- No unresolved severity-1 or severity-2 defects.
- Account deletion, billing, sync, and restore pass production-like tests.
- Safety review is complete.
- Support and rollback procedures are operational.
