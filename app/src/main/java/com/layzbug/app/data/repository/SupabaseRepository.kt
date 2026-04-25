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
    @SerialName("distance_km")
    val distanceKm: Double = 0.0,
    @SerialName("minutes")
    val minutes: Long = 0L,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Singleton
class SupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val context: Context
) {
    private val tableName = "manual_walks"

    private val currentUserId: String?
        get() = try {
            val id = GoogleSignIn.getLastSignedInAccount(context)?.id
            Log.d("SupabaseRepository", "currentUserId = $id")
            id
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "Failed to get userId: ${e.message}")
            null
        }

    val isLoggedIn: Boolean
        get() = !currentUserId.isNullOrEmpty()

    /**
     * Upsert a manual walk to Supabase.
     * Used for both walked=true and walked=false so unmarks propagate to other devices.
     * Conflict target is (user_id, walk_date) — requires a unique constraint on those
     * two columns in your Supabase table (add if missing).
     */
    suspend fun syncManualWalk(date: LocalDate, isWalked: Boolean, distanceKm: Double = 0.0, minutes: Long = 0L) {
        val userId = currentUserId
        if (userId == null) {
            Log.w("SupabaseRepository", "⚠️ Not logged in, skipping sync for $date")
            return
        }

        val dateStr = date.toString()

        try {
            // Check if row already exists for this user + date
            val existing = supabase.from(tableName)
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("walk_date", dateStr)
                    }
                }
                .decodeList<ManualWalk>()

            if (existing.isEmpty()) {
                // INSERT new row
                Log.d("SupabaseRepository", "➕ Inserting: $dateStr = $isWalked, ${distanceKm}km, ${minutes}min")
                val walk = ManualWalk(
                    userId     = userId,
                    walkDate   = dateStr,
                    isWalked   = isWalked,
                    distanceKm = distanceKm,
                    minutes    = minutes
                )
                supabase.from(tableName).insert(walk)
                Log.d("SupabaseRepository", "✅ Inserted: $dateStr")
            } else {
                // UPDATE existing row — target by user_id + walk_date, never by id
                Log.d("SupabaseRepository", "✏️ Updating: $dateStr = $isWalked, ${distanceKm}km, ${minutes}min")
                supabase.from(tableName).update(
                    {
                        set("is_walked",   isWalked)
                        set("distance_km", distanceKm)
                        set("minutes",     minutes)
                        set("updated_at",  java.time.Instant.now().toString())
                    }
                ) {
                    filter {
                        eq("user_id",   userId)
                        eq("walk_date", dateStr)
                    }
                }
                Log.d("SupabaseRepository", "✅ Updated: $dateStr = $isWalked")
            }
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "❌ Failed to sync $dateStr: ${e.message}", e)
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
                    filter { eq("user_id", userId) }
                }
                .decodeList<ManualWalk>()
            Log.d("SupabaseRepository", "📦 Fetched ${walks.size} walks for user: $userId")
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
                    table  = tableName
                    filter = "user_id=eq.$userId"
                }
                .map {
                    Log.d("SupabaseRepository", "🔔 Real-time update received")
                    fetchAllManualWalks()
                }
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "❌ Failed to start real-time listener: ${e.message}", e)
            emptyFlow()
        }
    }

    // Kept for any legacy call sites — internally now just upserts with is_walked=false
    suspend fun deleteManualWalk(date: LocalDate) {
        Log.d("SupabaseRepository", "deleteManualWalk called for $date — upserting is_walked=false instead")
        syncManualWalk(date, isWalked = false, distanceKm = 0.0)
    }
}