# DeAddict Current Architecture

Last updated: 2026-07-29
Baseline commit: `7838fc470e2f876dc50a2b375cea8daa9edd011f`

## Purpose

This document records the implemented architecture on `main` before the permanent Recovery Track work begins. It distinguishes code foundations from production-verified behavior.

## Modules

- `:app` — Android application, Compose UI, ViewModels, auth, sync, Rescue, insights, Rook, notifications, privacy, billing, export/import, and usage monitoring.
- `:core:programs` — program catalogue, categories, IDs, and safety tiers.
- `:core:database` — Room database, entities, DAOs, repositories, outbox, and migrations.

## Stack

- Kotlin and Java 17
- Jetpack Compose and Material 3
- MVVM with `StateFlow`
- Hilt dependency injection
- Room local persistence
- WorkManager background processing
- Supabase Auth, PostgREST, and Edge Functions
- Android Credential Manager and Google ID
- Google Play Billing
- DataStore preferences

## Runtime architecture

```text
Compose UI
  -> AppViewModel / feature state
  -> local repositories
  -> Room source of truth
  -> durable sync outbox
  -> WorkManager
  -> Supabase
```

Local writes are committed before cloud synchronization. Cloud failure must not block tracking or Rescue.

## Current global state responsibilities

`AppViewModel` currently coordinates navigation, active programs, selected program, tracking, Rescue, insights, notifications, privacy, usage monitoring, Rook, billing, and account deletion. This is an MVP concentration point and must be reduced gradually rather than rewritten at once.

## Current navigation

- Home
- Track
- Rescue
- Insights
- Profile

Target production navigation is documented separately as Today / Tracks / Tools / Insights / You.

## Current multi-addiction behavior

The application stores multiple active program records. The selected program is moved to the first position in UI state so existing Track, Rescue, and Insights screens operate on it. This is a compatibility layer, not the permanent Recovery Track domain model.

## Architecture invariants

1. Room is the local source of truth.
2. Cloud synchronization is optional and must be retryable.
3. User-generated records use stable IDs.
4. Private notes are excluded from normal cloud payloads.
5. Safety-critical behavior must be deterministic.
6. Rook templates must remain reviewed and bounded.
7. No Android client may contain a Supabase service-role key.
8. No screen should eventually rely on list ordering to identify a track.

## Current pressure points

- Program definitions and user journeys are represented by the same active-program concept.
- `programId` is the main foreign reference for tracking and Rescue.
- One global ViewModel owns unrelated feature state.
- The root Compose callback surface is large.
- Home progress is mostly generic copy.
- Notifications are primarily one daily local reminder.
- Billing has a disconnected verifier.
- Instrumented tests are not part of the main CI workflow.
- Full localization is blocked by hardcoded strings.

## Refactor direction

Introduce a permanent Recovery Track model first, then extract feature state incrementally:

```text
App shell
  -> Today
  -> Tracks
  -> Check-in
  -> Tracking
  -> Rescue
  -> Insights
  -> Settings
```

No broad rewrite should combine the Room migration, navigation replacement, billing changes, and all screen refactors in one pull request.
