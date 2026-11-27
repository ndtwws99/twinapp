package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

/**
 * Silent auto sign-in on startup using GetGoogleIdOption (single option per GetCredentialRequest).
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
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
            val result = credentialManager.getCredential(context, request)
            val cred = result.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(idToken, null)
                ).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

/**
 * Interactive Google Sign-In using GetSignInWithGoogleOption (single option per GetCredentialRequest).
 */
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
    } catch (_: Exception) {
        onAuthError("Thiếu cấu hình Google Sign-In (default_web_client_id).")
        return
    }
    val activity = context as? Activity
    if (activity == null) {
        onAuthError("Không thể mở hộp thoại đăng nhập Google từ ngữ cảnh hiện tại.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(activity, request)
            val cred = result.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val idToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(
                    GoogleAuthProvider.getCredential(idToken, null)
                ).await()
                onAuthSuccess()
            } else {
                onAuthError("Loại thông tin đăng nhập không hợp lệ.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Đăng nhập Google thất bại. Vui lòng thử lại.")
        }
    }
}

/**
 * Safe NonCancellable sign-out clearing Credential Manager state before Firebase signOut().
 */
fun signOutTwin(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onBeforeSignOut: suspend () -> Unit = {},
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                onBeforeSignOut()
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

fun signInWithEmailPassword(
    email: String,
    password: String,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
        onAuthError("Vui lòng nhập địa chỉ email hợp lệ.")
        return
    }
    if (password.length < 6) {
        onAuthError("Mật khẩu phải có ít nhất 6 ký tự.")
        return
    }
    scope.launch {
        try {
            Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
            onAuthSuccess()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Email/Password sign-in failed", e)
            onAuthError(mapEmailAuthError(e))
        }
    }
}

fun registerWithEmailPassword(
    displayName: String,
    email: String,
    password: String,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    val cleanName = displayName.trim()
    val cleanEmail = email.trim()
    if (cleanName.length < 2) {
        onAuthError("Tên hiển thị phải có ít nhất 2 ký tự.")
        return
    }
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
        onAuthError("Vui lòng nhập địa chỉ email hợp lệ.")
        return
    }
    if (password.length < 6) {
        onAuthError("Mật khẩu phải có ít nhất 6 ký tự.")
        return
    }
    scope.launch {
        try {
            val result = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(cleanName.take(80))
                .build()
            result.user?.updateProfile(profileUpdate)?.await()
            onAuthSuccess()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Email/Password registration failed", e)
            onAuthError(mapEmailAuthError(e))
        }
    }
}

fun sendPasswordReset(
    email: String,
    scope: CoroutineScope,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
        onError("Hãy nhập email tài khoản trước khi nhấn Quên mật khẩu.")
        return
    }
    scope.launch {
        try {
            Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
            onSuccess("Đã gửi liên kết đặt lại mật khẩu tới $cleanEmail.")
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Password reset failed", e)
            onError(mapEmailAuthError(e))
        }
    }
}

private fun mapEmailAuthError(e: Exception): String = when (e) {
    is FirebaseAuthWeakPasswordException -> "Mật khẩu quá yếu. Vui lòng chọn mật khẩu ít nhất 6 ký tự."
    is FirebaseAuthUserCollisionException -> "Email này đã được đăng ký tài khoản Twin. Hãy chuyển sang tab Đăng nhập."
    is FirebaseAuthInvalidUserException -> "Không tìm thấy tài khoản với email này."
    is FirebaseAuthInvalidCredentialsException -> "Email hoặc mật khẩu chưa chính xác. Vui lòng kiểm tra lại."
    is FirebaseAuthException -> when (e.errorCode) {
        "ERROR_OPERATION_NOT_ALLOWED" -> "Phương thức Email/Password chưa được bật trong Firebase Console."
        "ERROR_TOO_MANY_REQUESTS" -> "Bạn đã thử quá nhiều lần. Vui lòng đợi ít phút rồi thử lại."
        else -> e.localizedMessage ?: "Xác thực tài khoản thất bại."
    }
    else -> e.localizedMessage ?: "Xác thực tài khoản thất bại. Vui lòng thử lại."
}
