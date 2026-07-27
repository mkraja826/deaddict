# Database Schema

Room schema version 1 contains:

- `active_programs`: selected program and archive lifecycle.
- `tracking_events`: activity, urge, craving, slip, quantity, time, and cost events.
- `rescue_sessions`: offline Rescue outcomes and explainable inputs.
- `sync_outbox`: idempotent mutations with claim, retry, completion, and dead-letter states.

All entities use client-generated stable IDs and timestamps. Local-only rows never enter the outbox. Private notes are stored locally but excluded from synchronized payloads. Exported Room schemas are version-controlled; migration tests are required before schema version 2.

Room schema version 2 adds optional trigger metadata to tracking events. `MIGRATION_1_2` preserves existing rows and notes; its instrumentation migration test is compiled and awaits a connected Android test device.

The cloud migration defines `user_programs`, `tracking_events`, and `rescue_sessions`. Every row contains a non-null `user_id` referencing `auth.users`, ownership columns are indexed, and all tables have RLS enabled.
