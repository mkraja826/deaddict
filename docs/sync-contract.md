# DeAddict Synchronization Contract

Last updated: 2026-07-29

## Purpose

This document records the current local-first synchronization behavior and the rules that future aggregates must preserve.

## Current synchronized aggregates

| Local aggregate | Remote table |
|---|---|
| Active program | `user_programs` |
| Tracking event | `tracking_events` |
| Rescue session | `rescue_sessions` |

## Local-first flow

```text
User action
  -> Room transaction
  -> sync_outbox mutation
  -> immediate local UI update
  -> WorkManager upload
  -> cloud restore after outbox drains
```

Cloud failure must not prevent local tracking, Rescue, or viewing locally available history.

## Current outbox guarantees

- Stable aggregate IDs
- Unique idempotency keys
- UPSERT and DELETE operations
- Retry scheduling
- In-flight state
- Completed state
- Dead-letter state
- Error code storage
- Delete tombstone support

## Worker behavior

The cloud worker processes bounded batches. Restore occurs only after the pending outbox becomes idle. This prevents a cloud snapshot from overwriting local mutations that have not uploaded yet.

## Privacy boundary

Private tracking notes are not included in normal remote tracking payloads.

Future free-text motivation, custom trigger notes, check-in notes, and track notes must remain local unless a separately reviewed encrypted synchronization feature is explicitly enabled by the user.

## Authentication boundary

Remote operations require an authenticated Supabase user. Local/private mode remains available when the backend is unavailable or the user is signed out.

## Deletion rule

A synchronized delete must include both the aggregate ID and authenticated user scope. Delete tombstones must win over stale updates and remain durable long enough to protect reinstall and multi-device restore.

## Conflict policy baseline

Current conflict handling is deterministic and must remain deterministic. Future records should use revisions and client-update timestamps rather than relying only on device arrival order.

Recommended precedence:

1. Delete tombstone
2. Highest valid revision
3. Latest client update timestamp
4. Stable mutation ID tie-breaker

## Parent-before-child order

Future synchronization should use this ordering:

```text
Recovery Tracks
  -> Goal versions
  -> Daily check-ins
  -> Track check-in entries
  -> Tracking events
  -> Rescue sessions
  -> Milestones
```

Restore must follow the same parent-before-child order.

## Owner isolation

Every user-owned record must be scoped to an owner. Account switching must clear or isolate local user data before another account can access the application.

Planned owner forms:

```text
guest:<profile-uuid>
user:<supabase-user-uuid>
```

## Required future verification

- Two devices creating offline records
- Delete versus stale update
- Account switching
- Guest-to-account conversion
- Reinstall and restore
- Duplicate WorkManager execution
- Network interruption during upload
- Large-history restore
- Invalid or foreign-owned child record rejection
- Preservation of local private notes
