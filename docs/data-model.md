# DeAddict Data Model Baseline

Last updated: 2026-07-29

## Current Room database

Database version: `2`

Current entities:

```text
active_programs
tracking_events
rescue_sessions
sync_outbox
```

## active_programs

Fields:

- `id`
- `programId`
- `activatedAtEpochMillis`
- `archivedAtEpochMillis`
- `syncState`

`programId` has a unique index. The table currently represents both program activation and the user's recovery journey.

## tracking_events

Fields:

- `id`
- `programId`
- `kind`
- `quantity`
- `unit`
- `costMinorUnits`
- `urgeIntensity`
- `triggerKey`
- `occurredAtEpochMillis`
- `createdAtEpochMillis`
- `privateNote`
- `syncState`

Supported kinds:

```text
ACTIVITY
URGE
CRAVING
SLIP
QUANTITY
TIME
COST
```

## rescue_sessions

Fields:

- `id`
- `programId`
- `startedAtEpochMillis`
- `completedAtEpochMillis`
- `initialUrge`
- `finalUrge`
- `triggerKey`
- `actionKeys`
- `outcome`
- `syncState`

## sync_outbox

Fields:

- `id`
- `idempotencyKey`
- `aggregateType`
- `aggregateId`
- `operation`
- `payload`
- `createdAtEpochMillis`
- `attemptCount`
- `nextAttemptAtEpochMillis`
- `state`
- `lastErrorCode`

Aggregate types:

```text
ACTIVE_PROGRAM
TRACKING_EVENT
RESCUE_SESSION
```

## Current limitation

`programId` identifies the addiction definition but does not identify a particular user journey. The current model cannot cleanly represent multiple historical journeys for the same program, permanent primary/supporting roles, pause and maintenance states, or versioned goals.

## Locked domain distinction

```text
Program
= reusable addiction or behavior definition

Recovery Track
= one user's specific journey for that program
```

## Planned central identity

Future recovery records will reference:

```text
recoveryTrackId
```

The planned hierarchy is:

```text
Program
  -> Recovery Track
       -> Goal versions
       -> Daily check-ins
       -> Track check-in entries
       -> Tracking events
       -> Rescue sessions
       -> Milestones
       -> Notifications
       -> Insights
```

## Migration requirements

Any future Room migration must:

1. Preserve all active and archived programs.
2. Preserve tracking and Rescue IDs and timestamps.
3. Preserve private notes exactly.
4. Create a deterministic Recovery Track for every legacy active-program row.
5. Assign one primary open track where possible.
6. Preserve pending sync and delete tombstones.
7. Pass Room migration, foreign-key, and integrity tests.
8. Never use destructive migration for production user data.
