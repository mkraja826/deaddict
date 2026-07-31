# DeAddict 1.0.0 publish checklist

A production release is approved only when every blocking item below is complete and `:app:verifyPublishReadiness` passes using the same credentials and metadata used to build the signed AAB.

## Repository and build

- [ ] `main` contains the complete Recovery Track, daily check-in, sync, Insights, Rook, accessibility, and production-readiness work.
- [ ] Version is `1.0.0` with version code `100` or greater.
- [ ] JVM tests, Android lint, database migration tests, release minification, and release-readiness checks pass.
- [ ] Signed release AAB is built from a clean checkout of the approved commit.
- [ ] `jarsigner` verifies the AAB.
- [ ] Play App Signing and upload-key ownership are confirmed.
- [ ] Obsolete experimental PRs are closed as superseded.

## Production configuration

- [ ] Production Supabase project is selected.
- [ ] All SQL migrations, constraints, indexes, grants, and row-level-security policies are deployed.
- [ ] Account deletion server function is deployed and tested.
- [ ] Google Sign-In production OAuth client and SHA fingerprints are configured.
- [ ] Google Play Billing products, base plans, offers, and backend verification are configured.
- [ ] Production notification behavior is tested on Android 13+ and after reboot/timezone change.
- [ ] Backup, restore, cross-device conflict, guest reconciliation, and deletion are tested with production-like accounts.

## Public support site

- [ ] Repository variable `DEADDICT_DEVELOPER_NAME` contains the verified public developer identity.
- [ ] Repository variable `DEADDICT_SUPPORT_EMAIL` contains the monitored public support email.
- [ ] GitHub Pages is configured to deploy through GitHub Actions.
- [ ] **Validate and deploy public support site** completes successfully from `main` using `workflow_dispatch`.
- [ ] Home, privacy, terms, support, and account-deletion pages load over HTTPS without authentication.
- [ ] In-app **Support & legal** links open the same production pages.
- [ ] Developer name, support email, internal links, dark mode, keyboard focus, and mobile layout are reviewed on the deployed site.

## Public metadata required by the publish gate

- [ ] `DEADDICT_DEVELOPER_NAME`
- [ ] `DEADDICT_SUPPORT_EMAIL`
- [ ] `DEADDICT_SUPPORT_URL`
- [ ] `DEADDICT_PRIVACY_POLICY_URL`
- [ ] `DEADDICT_TERMS_URL`
- [ ] `DEADDICT_ACCOUNT_DELETION_URL`
- [ ] Production Supabase URL and publishable key
- [ ] Production Google server client ID
- [ ] Release keystore path, alias, and passwords

All URLs must be distinct public HTTPS pages that load without authentication. Localhost, example domains, and placeholder endpoints are rejected. The deletion page must provide a clear request path without requiring the app to be reinstalled or the user to sign in.

## Play Console account and app setup

- [ ] Developer identity, email, phone number, address, and website are verified.
- [ ] Confirm whether DeAddict's health/recovery classification requires an organization developer account for this launch.
- [ ] Package name `com.deaddict.app` is registered and Play App Signing is enabled.
- [ ] App category, tags, contact details, countries, pricing, and target audience are configured.
- [ ] Ads declaration is set to no ads for the current release.
- [ ] In-app purchases/subscriptions declaration is complete.
- [ ] Content rating questionnaire is complete and the certificate is saved.
- [ ] Data Safety form matches the final AAB and published privacy policy.
- [ ] Health-app, permissions, and sensitive-data declarations are complete where shown by Play Console.
- [ ] Account deletion questions include both the in-app path and external deletion URL.

## Store listing

- [ ] App title and English short/full description are entered exactly from the approved listing document.
- [ ] 512 × 512 app icon uploaded.
- [ ] 1024 × 500 feature graphic uploaded.
- [ ] **Capture Play Store screenshots** runs from the approved commit for the chosen light/dark theme.
- [ ] The downloaded artifact contains five validated 1080 × 1920 PNG files.
- [ ] At least four accurate phone screenshots are selected and uploaded.
- [ ] Screenshots come only from the debug-only fictional preview and contain no private notes, accounts, billing identity, notifications, or real recovery records.
- [ ] Screenshot navigation, Rook wording, privacy wording, safety boundaries, and feature availability match the signed candidate.
- [ ] Hindi and Telugu listing text and screenshots are reviewed before enabling localized promotion.
- [ ] Support, privacy, terms, and deletion links open successfully from a logged-out browser.

## Device and accessibility validation

- [ ] TalkBack walkthrough on a physical phone.
- [ ] Switch Access walkthrough on a physical phone.
- [ ] Large-font testing at 130%, 150%, and 200%.
- [ ] Light and dark theme review.
- [ ] Compact phone, standard phone, tablet, landscape, and foldable-window checks.
- [ ] Biometric/device-credential lock and screenshot protection checks.
- [ ] Offline, slow-network, expired-session, billing-offline, and sync-conflict checks.
- [ ] Medically high-risk programs preserve professional-guidance language in every supported locale.

## Testing tracks and rollout

- [ ] Internal test release completed.
- [ ] Closed test completed with documented testers and feedback.
- [ ] Pre-launch report reviewed with no blocking crash, ANR, security, accessibility, or compatibility issue.
- [ ] Local app-health assessment is `PROCEED` for the release candidate.
- [ ] Initial production rollout is 5%.
- [ ] Crash, ANR, save failure, sync failure, billing verification, deletion, and support channels are reviewed before increasing rollout.
- [ ] Rollout above 25% uses the explicit wide-rollout override only after a recorded health review.
- [ ] Rollback owner and procedure are named before publication.

## Release record

Archive the commit SHA, signed AAB checksum, mapping file, native symbols if any, Data Safety export, content-rating certificate, approved screenshot workflow artifact and run ID, store copy, policy URLs, deployed public-site version, Supabase migration state, test evidence, rollout decision, and release notes together.
