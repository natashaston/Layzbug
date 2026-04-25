package com.layzbug.app.data.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val context: Context
) {
    init {
        val currentAccount = GoogleSignIn.getLastSignedInAccount(context)
        Log.d("AuthManager", "🔧 AuthManager initialized - currentUser: ${currentAccount?.email} (${currentAccount?.id})")
    }

    val isLoggedIn: Boolean
        get() {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            val loggedIn = account != null
            Log.d("AuthManager", "🔍 isLoggedIn called - returning: $loggedIn (user: ${account?.email})")
            return loggedIn
        }

    val currentUserId: String?
        get() = GoogleSignIn.getLastSignedInAccount(context)?.id

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestId()       // ensures Google account ID is always returned
            .requestProfile()  // keeps account cached across app restarts
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<Unit> {
        return try {
            Log.d("AuthManager", "✅ Sign in successful: ${account.email} (${account.id})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthManager", "❌ Sign in failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            Log.d("AuthManager", "🔄 Signing out...")
            getGoogleSignInClient().signOut().await()
            Log.d("AuthManager", "✅ Sign out complete")
        } catch (e: Exception) {
            Log.e("AuthManager", "❌ Sign out error: ${e.message}")
            getGoogleSignInClient().signOut()
        }
    }
}