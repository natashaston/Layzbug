package com.layzbug.app.data.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val context: Context,
    private val auth: FirebaseAuth
) {
    init {
        val currentUser = auth.currentUser
        Log.d("AuthManager", "🔧 AuthManager initialized - currentUser: ${currentUser?.email} (${currentUser?.uid})")
    }

    val isLoggedIn: Boolean
        get() {
            val loggedIn = auth.currentUser != null
            Log.d("AuthManager", "🔍 isLoggedIn called - returning: $loggedIn (user: ${auth.currentUser?.email})")
            return loggedIn
        }

    val currentUserId: String? get() = auth.currentUser?.uid

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("445107189282-ovqp1pgopd3edcuf05mmlj2pf1klpra2.apps.googleusercontent.com") // Use Android client ID
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).await()
            Log.d("AuthManager", "✅ Sign in successful: ${auth.currentUser?.email}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthManager", "❌ Sign in failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        getGoogleSignInClient().signOut()
    }
}