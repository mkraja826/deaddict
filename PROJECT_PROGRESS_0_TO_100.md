# DeAddict Delivery Progress

Last updated: 2026-07-29
Baseline commit: `7838fc470e2f876dc50a2b375cea8daa9edd011f`

## Readiness summary

A single percentage no longer represents the project accurately. The repository has a strong engineering foundation, while the permanent multi-addiction model, production verification, safety review, billing verification, and closed-beta evidence remain incomplete.

| Dimension | Current estimate | Meaning |
|---|---:|---|
| Engineering foundation | 88% | Core Android, Room, sync, Rescue, privacy, CI, and supporting utilities exist. |
| Core recovery functionality | 67% | Tracking and Rescue are substantial; unified check-in and real progress are missing. |
| Multi-addiction architecture | 38% | Selected-program V1 exists; permanent Recovery Tracks do not. |
| Product experience | 55% | Functional MVP screens exist; onboarding and Today are not production-ready. |
| Safety and clinical readiness | 40% | Safety tiers and high-risk boundaries exist; regional and professional review remain. |
| Privacy and security readiness | 65% | Strong controls exist; end-to-end verification and independent review remain. |
| Monetization readiness | 35% | Billing client foundation exists; backend verification and Play configuration remain. |
| International readiness | 35% | Localization infrastructure exists; full copy and professional review remain. |
| Public commercial release | 45% | Closed-beta, monitoring, safety, billing, device, and legal gates remain. |

These are planning estimates, not automatically measured metrics.

## Implemented engineering foundations

- Kotlin, Jetpack Compose, MVVM, Room, WorkManager, Supabase, repository pattern, and local-first guest operation.
- Durable authenticated sync for programs, tracking events, and Rescue sessions with retries, dead-letter handling, delete tombstones, transactional restore, and private-note exclusion.
- Deterministic conflict-policy foundations and account-isolation policy.
- Recovery-goal, milestone, focus-session, insights, export/import, localization, and accessibility helpers.
- Biometric/device-credential lock, screenshot protection, analytics-off default, local deletion, and JWT-protected account-deletion foundation.
- CI runs JVM tests, Android lint, debug builds, and minified release builds, and uploads reports, logs, and APK artifacts.
- Multi-addiction selected-program layer and Add Track UI.
- Rook Direct, Brutal Banter, and Quiet modes with medically high-risk tone override.

## Product-critical work remaining

1. Permanent Recovery Track IDs, ownership, roles, lifecycle, and history.
2. Room and Supabase migrations from `programId` ownership to `recoveryTrackId`.
3. Persisted primary and supporting tracks.
4. Unified daily multi-track check-in.
5. Goal-aware progress dashboard.
6. Cross-track and replacement-behavior insights.
7. Rook feedback, repetition control, reviewed content catalogue, and safer defaults.
8. Track-aware notifications and production FCM where used.
9. Backend billing verification and entitlement lifecycle.
10. Production observability, instrumented CI, accessibility/device matrix, and closed beta.

## Phase 0 baseline

The following source-of-truth documents now live under `docs/`:

- `current-architecture.md`
- `data-model.md`
- `sync-contract.md`
- `safety-boundaries.md`
- `release-gates.md`
- `product-scope-v1.md`

## Current implementation branch

Branch: `phase-0-project-baseline`

This branch updates the technical baseline only. It does not claim that Phase 1–6 product work is implemented.
