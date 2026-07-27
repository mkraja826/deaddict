# Security Model

- No Supabase service-role key is shipped in Android.
- Every user-owned cloud table has RLS and a `user_id`.
- Cross-user read, update, and delete denial are automated tests.
- Privileged operations use authenticated Edge Functions.
- Tokens use platform-protected storage; app lock uses BiometricPrompt.
- Logs and analytics exclude journal content, explicit content, browsing data, messages, searches, private sharing content, and precise location.

Threat modeling and dependency scanning are required before closed testing.

The initial Supabase migration explicitly grants Data API access only to `authenticated`, enables RLS on every public user-data table, and defines separate own-row policies for select, insert, update, and delete. Update policies include both `USING` and `WITH CHECK`. Local pgTAP coverage verifies cross-user denial and ownership-transfer denial.

The same migration is deployed to the dedicated Deaddict hosted project. Hosted transactional probes confirmed cross-user select, insert, update, and delete denial; the Supabase security advisor reports no findings.

Local app access can be protected with AndroidX BiometricPrompt using biometric or device credential authentication. Screen and recent-task content can be protected with `FLAG_SECURE`. These controls protect casual access but are not represented as absolute device compromise protection.

Android cleartext traffic is disabled. `allowBackup=false` is reinforced with legacy backup rules and Android 12+ data-extraction rules that exclude databases, preferences, internal files, roots, and external app files from cloud backup and device transfer. Usage Access remains an explicit user-granted Android app-op and uses API-safe checks from API 26 onward.
