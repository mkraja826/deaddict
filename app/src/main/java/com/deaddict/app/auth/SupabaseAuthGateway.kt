package com.deaddict.app.auth

import com.deaddict.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseAuthGateway(
    private val client: SupabaseClient?,
    private val onAuthenticated: suspend (AuthenticatedUser) -> Unit = {},
    private val onSignedOut: suspend () -> Unit = {},
) : AuthGateway {
    override val availability: AuthAvailability =
        if (client == null) {
            AuthAvailability.Unavailable("Backend is not configured; private mode remains available.")
        } else {
            AuthAvailability.Configured
        }

    override suspend fun requestEmailOtp(email: EmailAddress) {
        requireClient().auth.signInWith(OTP) {
            this.email = email.value
            createUser = true
        }
    }

    override suspend fun verifyEmailOtp(
        email: EmailAddress,
        otp: EmailOtp,
    ): AuthenticatedUser {
        val auth = requireClient().auth
        auth.verifyEmailOtp(
            type = OtpType.Email.EMAIL,
            email = email.value,
            token = otp.value,
        )
        val user = auth.currentUserOrNull().requireUser()
        onAuthenticated(user)
        return user
    }

    override suspend fun signInWithGoogle(token: GoogleIdToken): AuthenticatedUser {
        val auth = requireClient().auth
        auth.signInWith(IDToken) {
            idToken = token.value
            provider = Google
            nonce = token.rawNonce
        }
        val user = auth.currentUserOrNull().requireUser()
        onAuthenticated(user)
        return user
    }

    override suspend fun currentUser(): AuthenticatedUser? =
        requireClient().auth.currentUserOrNull()?.toDomain()

    override suspend fun signOut() {
        requireClient().auth.signOut()
        onSignedOut()
    }

    private fun requireClient(): SupabaseClient =
        checkNotNull(client) { "Supabase is not configured" }
}

fun createDeAddictSupabaseClient(): SupabaseClient? {
    val url = BuildConfig.SUPABASE_URL
    val publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    if (url.isBlank() || publishableKey.isBlank()) return null
    require(url.startsWith("https://") && url.endsWith(".supabase.co")) {
        "Supabase URL must be a hosted HTTPS project URL"
    }
    require(!publishableKey.contains("service_role", ignoreCase = true)) {
        "A service-role key must never be embedded in Android"
    }
    return createSupabaseClient(url, publishableKey) {
        install(Auth)
        install(Postgrest)
        install(Functions)
    }
}

private fun UserInfo?.requireUser(): AuthenticatedUser =
    checkNotNull(this) { "Authentication completed without a user" }.toDomain()

private fun UserInfo.toDomain(): AuthenticatedUser =
    AuthenticatedUser(id = id, email = email)
