# DeAddict production guardrails

## Scope

This release-health layer is deliberately privacy preserving. It stores aggregate counters and a timestamp-only crash marker on the device. It does not store or transmit owner identifiers, Recovery Track identifiers, addiction types, check-in outcomes, private notes, trigger text, stack traces, exception messages, or device advertising identifiers.

No health record is uploaded in this phase. A future remote diagnostics backend must remain opt-in, aggregate-only, documented in the privacy policy, and independently reviewed before it is enabled.

## Staged rollout

1. Start production updates at 5%.
2. Move to 10% after a stable observation window.
3. Move to 25% only when the release-health decision is `PROCEED`.
4. Rollouts above 25% require an explicit `DEADDICT_ALLOW_WIDE_ROLLOUT=true` override after review.
5. Pause expansion immediately when a `HOLD` condition is observed.

The CI variable `DEADDICT_ROLLOUT_PERCENT` defaults to 5. Values must be between 1 and 100. CI refuses values above 25 unless the explicit wide-rollout override is present.

## Local health thresholds

The app evaluates daily check-in saves and Insights calculations as reliability operations.

- Daily check-in save is slow at 1,500 ms or more.
- Insights calculation is slow at 2,000 ms or more.
- Rate thresholds are evaluated only after at least 20 operations.
- Watch when operation failures reach 2%.
- Hold when operation failures reach 5%.
- Watch when slow operations reach 10%.
- Hold when slow operations reach 20%.
- Watch after one detected previous crash.
- Hold after two detected previous crashes.

A crash marker contains only the crash timestamp. It is consumed on the next app start and does not contain a stack trace or throwable details.

## Release checklist

- JVM tests pass.
- Android lint passes.
- `:app:verifyReleaseReadiness` passes.
- Release minification remains enabled.
- Backups remain disabled.
- Cleartext traffic remains disabled.
- RTL support remains enabled.
- Debug, release, database-test, APK, and AAB artifacts build successfully.
- Signed non-PR release builds pass `jarsigner` verification.
- Accessibility checks cover headings, pane titles, state announcements, descriptive controls, validation messages, and minimum Material touch targets.
- No new diagnostic payload contains recovery content or identifiers.

## Rollback conditions

Stop rollout expansion and prepare a rollback when any hold threshold is reached, check-in persistence fails repeatedly, Insights becomes unavailable for a meaningful share of users, account isolation is violated, private notes appear in any remote payload, or a medically high-risk safety boundary regresses.
