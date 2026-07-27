# Architecture

DeAddict uses feature-oriented Clean Architecture with MVVM at UI boundaries.

## Module direction

`app` depends on pure domain modules such as `core:programs`. Future data modules implement domain-owned repository contracts; domain code does not depend on Android, storage, or network SDKs.

## Invariants

- A user action is committed locally before synchronization.
- Guest mode has no network dependency.
- Sensitive notes are local unless the user explicitly opts in.
- Safety decisions are data-driven by `SafetyTier`, not duplicated in screens.
- Rescue and tracking remain operational without network access.
- Synced mutations have stable client IDs and idempotency keys.

## UI state

ViewModels expose immutable state through `StateFlow`. One-shot effects are narrowly scoped and never carry durable business state.

