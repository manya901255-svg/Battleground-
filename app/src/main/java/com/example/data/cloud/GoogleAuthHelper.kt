package com.example.data.cloud

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object GoogleAuthHelper {
    private const val TAG = "WarXAuthHelper"
    private const val PREFS_NAME = "warx_auth_prefs"
    private const val KEY_PENDING_EMAIL = "pending_email_link_address"

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (runCatching { Firebase.auth.currentUser }.getOrNull() != null) {
            onAuthSuccess()
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = withTimeoutOrNull(1500L) {
                    credentialManager.getCredential(context, request)
                }
                val credential = result?.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Silent auto sign-in skipped: ${e.message}")
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val activity = context as? Activity
                    ?: throw IllegalStateException("Google Sign-In requires an Activity context.")
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type received.")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signInWithOAuthProvider(
        context: Context,
        providerId: String,
        languageCode: String,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit,
        scope: CoroutineScope
    ) {
        val activity = context as? Activity
        if (activity == null) {
            onAuthError("OAuth sign-in requires an active screen context.")
            return
        }

        scope.launch {
            try {
                val pending = Firebase.auth.pendingAuthResult
                if (pending != null) {
                    pending.await()
                    onAuthSuccess()
                    return@launch
                }

                val builder = OAuthProvider.newBuilder(providerId)
                when (providerId) {
                    "apple.com" -> {
                        builder.setScopes(listOf("email", "name"))
                        builder.addCustomParameter("locale", languageCode)
                    }
                    "facebook.com" -> {
                        builder.setScopes(listOf("email", "public_profile"))
                        builder.addCustomParameter("locale", languageCode)
                    }
                }

                Firebase.auth
                    .startActivityForSignInWithProvider(activity, builder.build())
                    .await()
                onAuthSuccess()
            } catch (e: FirebaseAuthException) {
                val code = e.errorCode
                val msg = e.localizedMessage.orEmpty()
                when {
                    code.contains("CANCEL", ignoreCase = true) ||
                        msg.contains("canceled", ignoreCase = true) ||
                        msg.contains("cancelled", ignoreCase = true) -> {
                        onAuthCancelled()
                    }
                    code == "ERROR_OPERATION_NOT_ALLOWED" ||
                        msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
                        msg.contains("not enabled", ignoreCase = true) -> {
                        val name = if (providerId == "apple.com") "Apple" else "Facebook"
                        onAuthError("$name sign-in is not yet enabled on the server. Enable $name in Firebase Authentication Console or sign in with Google.")
                    }
                    else -> {
                        onAuthError(msg.ifBlank { "Authentication failed ($code)." })
                    }
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage.orEmpty()
                if (msg.contains("canceled", ignoreCase = true) || msg.contains("cancelled", ignoreCase = true)) {
                    onAuthCancelled()
                } else {
                    Log.e(TAG, "OAuth sign-in failed for $providerId", e)
                    onAuthError(msg.ifBlank { "Sign in failed for $providerId." })
                }
            }
        }
    }

    fun sendEmailSignInLink(
        context: Context,
        email: String,
        onLinkSent: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope
    ) {
        val cleanEmail = email.trim()
        if (!isValidEmail(cleanEmail)) {
            onAuthError("Please enter a valid email address.")
            return
        }

        scope.launch {
            try {
                val projectId = runCatching { Firebase.app.options.projectId }.getOrNull()
                    ?.takeIf { it.isNotBlank() } ?: "warx-battleground"
                val continueUrl = "https://$projectId.firebaseapp.com/login"
                val settings = ActionCodeSettings.newBuilder()
                    .setUrl(continueUrl)
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName(context.packageName, true, null)
                    .build()

                Firebase.auth.sendSignInLinkToEmail(cleanEmail, settings).await()
                savePendingEmail(context, cleanEmail)
                onLinkSent()
            } catch (e: FirebaseAuthException) {
                val msg = e.localizedMessage.orEmpty()
                if (e.errorCode == "ERROR_OPERATION_NOT_ALLOWED" || msg.contains("OPERATION_NOT_ALLOWED", true)) {
                    onAuthError("Email Link sign-in needs to be enabled in Firebase Console (Email/Password -> Email link). You can also use Google Sign-In or Guest Mode.")
                } else {
                    onAuthError(msg.ifBlank { "Failed to send verification link." })
                }
            } catch (e: Exception) {
                onAuthError(e.localizedMessage ?: "Failed to send verification link.")
            }
        }
    }

    fun completeEmailLinkSignIn(
        context: Context,
        email: String,
        emailLink: String,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope
    ) {
        val cleanEmail = email.trim()
        val cleanLink = emailLink.trim()
        if (!isValidEmail(cleanEmail)) {
            onAuthError("Please enter a valid email address.")
            return
        }
        if (cleanLink.isBlank() || !Firebase.auth.isSignInWithEmailLink(cleanLink)) {
            onAuthError("Invalid Firebase email sign-in link. Paste the full link from your inbox.")
            return
        }

        scope.launch {
            try {
                Firebase.auth.signInWithEmailLink(cleanEmail, cleanLink).await()
                clearPendingEmail(context)
                onAuthSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Email link verification failed", e)
                onAuthError(e.localizedMessage ?: "Could not verify email sign-in link.")
            }
        }
    }

    fun sendPasswordReset(
        email: String,
        onResetSent: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope
    ) {
        val cleanEmail = email.trim()
        if (!isValidEmail(cleanEmail)) {
            onAuthError("Enter a valid email address to receive a password reset link.")
            return
        }
        scope.launch {
            try {
                Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
                onResetSent()
            } catch (e: Exception) {
                Log.e(TAG, "Password reset failed", e)
                onAuthError(e.localizedMessage ?: "Unable to send password reset email.")
            }
        }
    }

    fun getPendingEmail(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PENDING_EMAIL, "")
            .orEmpty()
    }

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return false
        return runCatching { Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() }
            .getOrElse { trimmed.contains("@") && trimmed.contains(".") }
    }

    private fun savePendingEmail(context: Context, email: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING_EMAIL, email)
            .apply()
    }

    private fun clearPendingEmail(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING_EMAIL)
            .apply()
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }
}
