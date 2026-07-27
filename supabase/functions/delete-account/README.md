# delete-account

Authenticated Edge Function used by the Android account-deletion coordinator.

Security requirements:

- Deploy with JWT verification enabled.
- The caller must provide a valid Supabase user access token.
- `SUPABASE_SERVICE_ROLE_KEY` remains available only in the Edge Function runtime and must never be shipped in Android.
- The function resolves the caller with `auth.getUser()` and deletes only that Auth user.
- Current recovery tables reference `auth.users(id)` with `ON DELETE CASCADE`, so their rows are removed with the account.

The Android client clears its local session and local recovery state only after this function returns a successful response.
