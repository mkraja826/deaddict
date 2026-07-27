package com.deaddict.app.auth

@JvmInline
value class EmailAddress private constructor(val value: String) {
    companion object {
        private val pattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        fun of(value: String): EmailAddress {
            val normalized = value.trim().lowercase()
            require(pattern.matches(normalized)) { "Invalid email address" }
            return EmailAddress(normalized)
        }
    }
}

@JvmInline
value class EmailOtp private constructor(val value: String) {
    companion object {
        fun of(value: String): EmailOtp {
            require(value.matches(Regex("\\d{6,8}"))) { "Invalid email OTP" }
            return EmailOtp(value)
        }
    }
}

data class GoogleIdToken(
    val value: String,
    val rawNonce: String,
) {
    init {
        require(value.isNotBlank())
        require(rawNonce.length >= 32)
    }
}

data class AuthenticatedUser(
    val id: String,
    val email: String?,
)

sealed interface AuthAvailability {
    data object Configured : AuthAvailability
    data class Unavailable(val reason: String) : AuthAvailability
}

interface AuthGateway {
    val availability: AuthAvailability
    suspend fun requestEmailOtp(email: EmailAddress)
    suspend fun verifyEmailOtp(email: EmailAddress, otp: EmailOtp): AuthenticatedUser
    suspend fun signInWithGoogle(token: GoogleIdToken): AuthenticatedUser
    suspend fun currentUser(): AuthenticatedUser?
    suspend fun signOut()
}

