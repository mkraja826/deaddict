# DeAddict Delivery Progress

Last updated: 2026-07-27

Overall progress: **84%**

| Phase | Scope | Status | Weight | Earned |
|---|---|---:|---:|---:|
| 0 | Audit, foundation, architecture, documentation | Complete | 7% | 7% |
| 1 | Program framework, taxonomy, safety tiers | Complete | 5% | 5% |
| 2 | Room, local repositories, offline operation, sync queue | Partial | 10% | 9% |
| 3 | Authentication, Supabase, RLS, guest migration | Partial | 9% | 8% |
| 4 | Onboarding, Home, Track, recovery plans | Partial | 11% | 10% |
| 5 | Digital usage monitoring | Partial | 7% | 5% |
| 6 | Rescue and safety escalation | Partial | 10% | 9% |
| 7 | Local and remote notifications | Partial | 7% | 5% |
| 8 | Insights and reports | Partial | 7% | 6% |
| 9 | Privacy and security controls | Partial | 9% | 7% |
| 10 | Play Billing and entitlements | Partial | 5% | 3% |
| 11 | Localization and worldwide configuration | Partial | 5% | 3% |
| 12 | QA, accessibility, security, closed test | Partial | 8% | 7% |

## Current gate

- Application ID: `com.deaddict.app`
- Minimum Android: API 26
- Compile/target SDK: API 36
- Local-first and guest-first are architectural invariants.
- Safety and essential privacy features cannot be paywalled.
- Room schema v2, atomic local repositories, and the durable sync outbox are implemented.
- Authenticated Supabase upload now processes programs, tracking events, and Rescue sessions through a network-constrained WorkManager pipeline with atomic claims, bounded retries, dead-lettering, interrupted-claim recovery, and private-note exclusion.
- Cloud restore now downloads the three user-owned datasets after upload processing and applies them transactionally without overwriting `LOCAL_ONLY` or `PENDING` rows; existing local private notes are preserved.
- GitHub Actions now gates JVM tests, Android lint, debug/release builds, reports, and APK artifacts. The connected upload-and-restore sync build is green.
- Phase 2 remains partial until delete/tombstone processing, broader timestamp conflict reconciliation, account-switch coverage, and real-device offline/reconnect verification are complete.
- Phase 3 Android Auth, explicit guest migration consent, local migrations, and RLS tests are implemented.
- The dedicated `Deaddict` Supabase project has the initial schema, least-privilege grants, own-row RLS, and tracking-trigger migration deployed with no security-advisor findings.
- Phase 3 remains partial until Google OAuth credentials and end-to-end provider sign-in are verified.
- Phase 4 now includes first-run program selection, the five-tab shell, local Home state, urge tracking, and safety-aware recovery guidance.
- Track now supports activity, urge, craving, slip, quantity, duration, cost, intensity, and trigger logging.
- Phase 4 remains partial until editable plan goals, UI tests, and device visual QA are complete.
- Phase 5 includes explicit Usage Access consent and daily app-level time, opening, session, rapid-reopening, morning, and late-night estimates.
- Phase 5 remains partial until persisted trends, warnings, focus sessions, and device-level accuracy QA are complete.
- Phase 6 includes the enforced 60-second pause, breathing guidance, motivation, urge recheck, triggers, three relevant replacement actions, supportive outcomes, and offline persistence.
- Phase 6 remains partial until regional safety-resource configuration, reduced-motion QA, and device accessibility validation are complete.
- Phase 7 includes notification permission-on-opt-in, private channels and copy, daily check-in scheduling, quiet hours, rate limiting, timezone changes, and reboot recovery.
- Phase 7 remains partial until risk/bedtime/weekly/focus schedulers, FCM credentials, token lifecycle, and device delivery QA are complete.
- Phase 8 includes explainable seven-day summaries, trigger and risk-period patterns, intensity trends, slip counts, and Rescue effectiveness.
- Phase 8 remains partial until long-term reports, export-ready charts, and device UI QA are complete.
- Phase 9 includes biometric/device-credential lock, screenshot and recent-preview protection, analytics off by default, usage-monitoring control, and confirmed local recovery-data deletion.
- Phase 9 remains partial until data export, per-program deletion, discreet icon/name, granular sharing, and cloud/account deletion are complete.
- Phase 10 includes Play Billing 9.1.0, Play-sourced subscription offers, purchase restoration, pending-purchase handling, and a centralized free/Plus entitlement policy.
- Rescue, safety resources, deletion, biometric protection, and essential privacy are explicitly tested as permanently free.
- Phase 10 remains partial until Play Console products/base plans, backend verification and acknowledgement, RTDN lifecycle sync, license testing, and end-to-end purchase QA are complete.
- Phase 11 includes Android per-app language declarations, English/Hindi/Telugu resources for primary navigation and critical billing/privacy/safety copy, RTL support, and fail-closed regional release configuration.
- India is the only enabled initial market; the United States remains reference-only and all unknown countries remain unreleased.
- Phase 11 remains partial until the complete UI catalog, professional translation review, localized legal documents/store listings, regional safety-resource validation, and RTL device QA are complete.
- Phase 12 includes passing JVM and connected-sync tests, a clean Android lint gate, passing Pixel 7 Compose privacy-boundary instrumentation, passing Room migration/repository instrumentation, named privacy switches for assistive technology, API 26-safe Usage Access checks, explicit backup/device-transfer exclusion, blocked cleartext traffic, and a minified release build.
- Phase 12 remains partial until the wider device/emulator matrix, manual TalkBack/large-text/reduced-motion QA, dependency and clinical review, signed AAB creation, Play closed testing, and crash/ANR review are complete.
