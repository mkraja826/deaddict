# Google Play Data Safety mapping

This document maps the current DeAddict Android implementation to the Play Console Data Safety questionnaire. It is a release-control document, not a substitute for reviewing the final AAB, SDK versions, Supabase configuration, and Google Play definitions immediately before submission.

## Collection overview

### Personal information

**Possible data:** email address, authentication provider identifier, Supabase user identifier, and authentication/security metadata.

**When:** only when the user chooses to create or use an account.

**Purpose:** account management, authentication, synchronization, restoration, security, and deletion requests.

**Required or optional:** optional for local-first core use; required for account-based cloud features.

### User-generated content and health-related recovery records

**Possible data:** Recovery Tracks, program identifiers, goals, goal versions, daily outcomes, mood, stress, energy, sleep quality, urge ratings, measurements, triggers, Rescue sessions, replacement actions, and eligible preferences.

**When:** entered by the user and synchronized only when an authenticated cloud-eligible flow is used.

**Purpose:** app functionality, cross-device synchronization, restoration, selected-track Insights, and account deletion.

**Important exclusion:** private check-in notes are device-local and must not appear in Supabase tables, outbox payloads, remote diagnostics, store screenshots, support tickets, or analytics.

### Purchase information

**Possible data:** Google Play product identifier, purchase token, entitlement state, verification result, and transaction metadata made available by Google Play Billing.

**Purpose:** subscription verification, entitlement restoration, fraud prevention, and support.

**Payment-card data:** not received by DeAddict; payment credentials are processed by Google Play.

## Local-only processing that is not transmitted in the current release

- Private check-in notes.
- Android app-usage information obtained through optional Usage Access.
- Biometric, face, fingerprint, or device-credential templates.
- Aggregate local app-health counters and timestamp-only previous-crash marker.
- Raw exception messages and stack traces, which are intentionally not stored by the local health monitor.

Recheck these statements if any remote diagnostics, analytics, advertising, attribution, customer-support, or experimentation SDK is added.

## Service providers

The current implementation may send eligible data to Supabase for authentication, database synchronization, server functions, verification, and deletion; to Google for sign-in; and to Google Play Billing for purchase operations. In the Play form, evaluate these transfers using the current definitions of collection, sharing, and service-provider processing. Do not mark data as sold. The current application contains no advertising network.

## Security declarations to verify

- Data transmitted off-device uses encrypted HTTPS connections.
- Android cleartext traffic is disabled.
- Android backup for app data is disabled.
- Supabase tables use authenticated ownership controls and row-level security.
- Account deletion is available in the app.
- A public external account-deletion URL is supplied in Play Console.
- A public privacy-policy URL is supplied in Play Console.

## Suggested Play Console review matrix

| Data category | Collected | Shared | Optional | Primary purpose |
| --- | --- | --- | --- | --- |
| Email address / user ID | For signed-in users | Review service-provider definition | Yes | Account management and sync |
| User-generated recovery records | For cloud-eligible signed-in use | Review service-provider definition | Yes | App functionality and sync |
| Purchase history / token | For subscription users | Google Play/service verification | Yes | Purchase verification |
| Private notes | No remote collection | No | Yes | Device-local journaling |
| App usage from Usage Access | No remote collection in current release | No | Yes | Device-local limit support |
| Diagnostics | Local aggregate counters only | No | N/A | Reliability guardrails |
| Biometric information | Not received by DeAddict | No | Yes | Android-managed app unlock |

## Final submission checks

1. Inspect the dependency graph and Google Play SDK Index guidance for every production SDK.
2. Verify network traffic from a release build with account, sync, billing, deletion, notification, and usage-monitoring flows.
3. Confirm private notes never leave the device.
4. Confirm the published privacy policy matches the final form.
5. Complete the Data deletion questions and add the public deletion URL.
6. Save screenshots or an exported copy of the approved form with the release record.
