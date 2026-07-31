# DeAddict 1.0.0 release notes

## Google Play release notes

DeAddict 1.0 introduces independent Recovery Tracks for managing more than one addiction or habit without combining them into a single score.

- Create primary and supporting Recovery Tracks.
- Complete one private daily check-in across active tracks.
- Record separate outcomes, measurements, and peak urges.
- Use guided Rescue tools during difficult moments.
- Review goal-aware progress over 7, 30, or 90 days.
- See cautious cross-track and replacement-action patterns.
- Keep private notes on the device.
- Use optional sign-in for eligible cross-device synchronization.
- Protect the app with biometric/device-credential locking and screenshot protection.
- Delete local recovery data or request account deletion from the app.

This release also includes accessibility improvements, deterministic sync conflict handling, release guardrails, and medically high-risk safety boundaries. DeAddict is a self-management support tool and does not provide detoxification or tapering instructions.

## Internal release summary

Version name: `1.0.0`

Version code: `100`

Recommended initial rollout: `5%`

Required evidence before production:

- Signed AAB and checksum.
- Passing JVM tests, lint, migration tests, and release/publish gates.
- Production Supabase migration and RLS verification.
- Google Sign-In and Billing verification.
- Privacy policy and deletion URLs live over HTTPS.
- Approved Data Safety and content-rating forms.
- Physical-device TalkBack and Switch Access walkthroughs.
- Internal and closed testing sign-off.
- Rollback owner and monitored health thresholds.
