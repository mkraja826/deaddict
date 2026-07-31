# DeAddict Play Store screenshot capture

DeAddict store screenshots must be generated from the debug-only `StorePreviewActivity`. Never capture a developer's real account, a tester's recovery history, private notes, Supabase records, billing identity, email address, or notification content.

## Fictional preview set

The controlled preview contains two fictional Recovery Tracks:

- Social media — primary Recovery Track.
- Caffeine — supporting Recovery Track.

The values, streaks, outcomes, Rescue session, Rook messages, and cross-track patterns are authored examples. They are not copied from a real user and are not loaded from Room, DataStore, Supabase, Google Play Billing, Android usage access, or network services.

## Screens

The automated set captures five 1080 × 1920 portrait images:

1. `today.png` — one unified daily check-in with independent Social media and Caffeine outcomes.
2. `tracks.png` — primary/supporting Recovery Track selection and independent history.
3. `rescue.png` — offline Rescue flow owned by Social media.
4. `insights.png` — selected-track progress, cautious cross-track associations, and replacement actions.
5. `you.png` — Rook tone controls, privacy controls, deletion, and public support resources.

The final Play listing can use four or five of these images after visual review. The first screenshot should communicate the core promise: one private check-in across multiple independent addictions.

## Running the workflow

1. Open GitHub Actions.
2. Select **Capture Play Store screenshots**.
3. Choose light or dark theme.
4. Run the workflow from the approved release branch or commit.
5. Download the `deaddict-play-screenshots-<theme>` artifact.
6. Verify every image against the approved app build and store copy.

The workflow boots a Pixel 2 profile emulator, installs the debug APK, launches the non-exported preview activity with an explicit fictional screen key, captures each screen with `adb screencap`, and verifies the PNG signature, dimensions, and minimum file size.

## Privacy safeguards

- The preview activity exists only in `app/src/debug` and is absent from release manifests and the release AAB.
- It is not exported to other applications.
- It does not inject or modify the production database.
- It does not request authentication, usage access, notification permission, billing, camera, contacts, files, or location.
- It makes no network request.
- It does not display a private-note field.
- It uses generic fictional program names and no identifiable person.
- Screenshots are uploaded as short-retention workflow artifacts, not automatically committed to the repository or published to Play.

## Accuracy review

Before upload, compare every screenshot with the final signed candidate:

- Navigation labels and ordering must match.
- Rook tone descriptions must match the current safety rules.
- No screen may imply a cure, treatment, detox plan, taper plan, diagnosis, guaranteed result, or combined recovery score.
- Cross-track statements must remain associations rather than causation claims.
- Private notes must remain described as local-only.
- Account deletion and support resources must remain available outside the paid entitlement boundary.
- Subscription prices must not be shown unless they match the active Play Console products and region.

## Visual review

Review the artifact at 100% scale and on a phone-sized viewport. Confirm:

- No clipped text.
- No overlapping floating actions or system bars.
- Sufficient contrast in light and dark themes.
- Correct typography and spacing.
- No debug banners, emulator controls, cursor, keyboard, toast, notification, or permission dialog.
- No accidental status-bar details that identify a developer account.
- Rook copy remains readable and does not dominate the recovery action.

## Release evidence

Archive the approved screenshot artifact alongside the signed AAB checksum, commit SHA, mapping file, Data Safety export, content-rating certificate, store copy, policy URLs, and release decision. Record which screenshot theme and workflow run produced the uploaded Play assets.
