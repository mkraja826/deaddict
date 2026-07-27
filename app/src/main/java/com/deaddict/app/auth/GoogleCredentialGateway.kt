package com.deaddict.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class GoogleCredentialGateway(
    context: Context,
    private val serverClientId: String,
    private val credentialManager: CredentialManager = CredentialManager.create(context),
) {
    suspend fun getIdToken(context: Context): GoogleIdToken {
        check(serverClientId.isNotBlank()) { "Google server client ID is not configured" }
        val rawNonce = ByteArray(32)
            .also(SecureRandom()::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        val hashedNonce = MessageDigest.getInstance("SHA-256")
            .digest(rawNonce.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val googleOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()
        val response = credentialManager.getCredential(
            context = context,
            request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build(),
        )
        val credential = response.credential
        check(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
        ) { "Unexpected credential type" }
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        return GoogleIdToken(googleCredential.idToken, rawNonce)
    }
}

