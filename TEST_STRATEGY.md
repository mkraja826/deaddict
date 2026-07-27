# Test Strategy

- Pure domain rules: fast JVM unit tests.
- Room: DAO, transaction, and migration tests.
- Sync: idempotency, retry, conflict, guest-consent, and offline tests.
- Supabase: RLS allow-own and deny-cross-user integration tests.
- UI: focused Compose tests for critical navigation, tracking, Rescue, privacy, and accessibility.
- Release gate: unit tests, instrumentation suite, lint, release build, dependency/security checks, and manual accessibility verification.

During development, test changed modules only. Run the full suite at phase gates.

Latest device gate (Pixel 7 AVD, API 35):

- JVM domain/application suites: 19 passed.
- Room migration and local repository instrumentation: 3 passed.
- Compose locked privacy boundary: 1 passed.
- Remaining device matrix: API 26/29/33/36, TalkBack, large text, RTL, reduced motion, and physical-device OEM coverage.
