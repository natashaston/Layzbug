package com.layzbug.app.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ManualWalk(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("walk_date")
    val walkDate: String,
    @SerialName("is_walked")
    val isWalked: Boolean,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Singleton
class SupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val context: Context
) {
    private val tableName = "manual_walks"

    // Get userId dynamically every time it's needed
    private val currentUserId: String?
        get() = try {
            GoogleSignIn.getLastSignedInAccount(context)?.id
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "Failed to get userId: ${e.message}")
            null
        }

    val isLoggedIn: Boolean
        get() {
            val userId = currentUserId
            Log.d("SupabaseRepository", "isLoggedIn check - userId: $userId")
            return !userId.isNullOrEmpty()
        }

    suspend fun syncManualWalk(date: LocalDate, isWalked: Boolean) {
        val userId = currentUserId

        if (userId == null) {
            Log.w("SupabaseRepository", "⚠️ Not logged in, cannot sync $date")
            return
        }

        try {
            Log.d("SupabaseRepository", "🚀 Syncing manual walk: $date = $isWalked (userId: $userId)")

            val walk = ManualWalk(
                userId = userId,
                walkDate = date.toString(),
                isWalked = isWalked
            )

            supabase.from(tableName).upsert(walk)

            Log.d("SupabaseRepository", "✅ Synced manual walk: $date = $isWalked")
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "❌ Failed to sync $date: ${e.message}", e)
        }
    }

    suspend fun fetchAllManualWalks(): List<ManualWalk> {
        val userId = currentUserId

        if (userId == null) {
            Log.w("SupabaseRepository", "⚠️ Not logged in, cannot fetch walks")
            return emptyList()
        }

        return try {
            val walks = supabase.from(tableName)
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<ManualWalk>()

            Log.d("SupabaseRepository", "📦 Fetched ${walks.size} manual walks for user: $userId")
            walks
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "❌ Failed to fetch walks: ${e.message}", e)
            emptyList()
        }
    }

    fun observeManualWalks(): Flow<List<ManualWalk>> {
        val userId = currentUserId

        if (userId == null) {
            Log.w("SupabaseRepository", "⚠️ Not logged in, cannot observe walks")
            return emptyFlow()
        }

        Log.d("SupabaseRepository", "👂 Starting real-time listener for user: $userId")

        return try {
            supabase.channel("manual_walks_channel")
                .postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = tableName
                    filter = "user_id=eq.$userId"
                }
                .map { action ->
                    Log.d("SupabaseRepository", "🔔 Received realtime update: ${action::class.simpleName}")
                    fetchAllManualWalks()
                }
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "❌ Failed to start realtime listener: ${e.message}", e)
            emptyFlow()
        }
    }

    suspend fun deleteManualWalk(date: LocalDate) {
        val userId = currentUserId

        if (userId == null) {
            Log.w("SupabaseRepository", "⚠️ Not logged in, cannot delete $date")
            return
        }

        try {
            supabase.from(tableName).delete {
                filter {
                    eq("user_id", userId)
                    eq("walk_date", date.toString())
                }
            }

            Log.d("SupabaseRepository", "🗑️ Deleted manual walk: $date")
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "❌ Failed to delete $date: ${e.message}", e)
        }
    }
}