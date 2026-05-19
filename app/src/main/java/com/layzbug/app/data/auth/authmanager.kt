package com.layzbug.app.data.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val WEB_CLIENT_ID =
            "445107189282-ovqp1pgopd3edcuf05mmlj2pf1klpra2.apps.googleusercontent.com"

        val FITNESS_SCOPE_ACTIVITY = Scope(Fitness.SCOPE_ACTIVITY_READ.scopeUri)
        val FITNESS_SCOPE_LOCATION = Scope(Fitness.SCOPE_LOCATION_READ.scopeUri)
    }

    init {
        val currentAccount = GoogleSignIn.getLastSignedInAccount(context)
        Log.d("AuthManager", "🔧 AuthManager initialized - currentUser: ${currentAccount?.email}")
    }

    val isLoggedIn: Boolean
        get() {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            val loggedIn = account != null
            Log.d("AuthManager", "🔍 isLoggedIn: $loggedIn (${account?.email})")
            return loggedIn
        }

    val currentUserId: String?
        get() = GoogleSignIn.getLastSignedInAccount(context)?.email

    fun hasFitnessPermission(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, FITNESS_SCOPE_ACTIVITY)
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestId()
            .requestProfile()
            .requestServerAuthCode(WEB_CLIENT_ID)
            .requestScopes(FITNESS_SCOPE_ACTIVITY, FITNESS_SCOPE_LOCATION)
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getBasicSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestId()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<Unit> {
        return try {
            Log.d("AuthManager", "✅ Sign in successful: ${account.email} (${account.id})")
            Log.d("AuthManager", "   Fitness scope granted: ${hasFitnessPermission()}")
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