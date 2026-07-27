# Sync Model

Local storage is authoritative for interactive operations. Mutations update Room in one transaction with an outbox entry. WorkManager uploads later using stable mutation IDs; retries are bounded and idempotent.

Guest data stays local. Linking or creating an account does not upload it until a separate, explicit consent step. Conflict policy is defined per entity before sync implementation.

The v1 outbox uses deterministic idempotency keys, atomic claim transitions, exponential backoff capped at six hours, and dead-lettering after ten attempts. Private notes are never serialized into its payload.
