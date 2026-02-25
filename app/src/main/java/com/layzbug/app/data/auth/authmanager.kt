package com.layzbug.app.data.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
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
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<Unit> {
        return try {
            // Google account is already signed in at this point
            // No need to await Firebase - just confirm we have the account
            Log.d("AuthManager", "✅ Sign in successful: ${account.email} (${account.id})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthManager", "❌ Sign in failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun signOut() {
        getGoogleSignInClient().signOut()
    }
}