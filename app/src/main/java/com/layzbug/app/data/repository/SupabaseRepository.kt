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
    @SerialName("user_id")    val userId: String,
    @SerialName("walk_date")  val walkDate: String,
    @SerialName("is_walked")  val isWalked: Boolean,
    @SerialName("is_manual")  val isManual: Boolean = true,
    @SerialName("distance_km") val distanceKm: Double = 0.0,
    @SerialName("minutes")    val minutes: Long = 0L,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Singleton
class SupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val context: Context
) {
    private val manualTable = "manual_walks"

    private val currentUserId: String?
        get() = try {
            val email = GoogleSignIn.getLastSignedInAccount(context)?.email
            Log.d("SupabaseRepository", "currentUserId = $email")
            email
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "Failed to get userId: ${e.message}")
            null
        }

    val isLoggedIn: Boolean
        get() = !currentUserId.isNullOrEmpty()

    suspend fun syncManualWalk(
        date: LocalDate,
        isWalked: Boolean,
        distanceKm: Double = 0.0,
        minutes: Long = 0L
    ) {
        val userId = currentUserId
        if (userId == null) {
            Log.w("SupabaseRepository", "⚠️ Not logged in, skipping sync for $date")
            return
        }

        val dateStr = date.toString()

        try {
            val existing = supabase.from(manualTable)
                .select {
                    filter {
                        eq("user_id",   userId)
                        eq("walk_date", dateStr)
                    }
                }
                .decodeList<ManualWalk>()

            if (existing.isEmpty()) {
                supabase.from(manualTable).insert(
                    ManualWalk(
                        userId     = userId,
                        walkDate   = dateStr,
                        isWalked   = isWalked,
                        isManual   = true,
                        distanceKm = distanceKm,
                        minutes    = minutes
                    )
                )
                Log.d("SupabaseRepository", "✅ Inserted manual walk: $dateStr")
            } else {
                supabase.from(manualTable).update(
                    {
                        set("is_walked",   isWalked)
                        set("is_manual",   true)
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
                Log.d("SupabaseRepository", "✅ Updated manual walk: $dateStr")
            }
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "❌ Failed to sync manual walk $dateStr: ${e.message}", e)
        }
    }

    suspend fun fetchAllManualWalks(): List<ManualWalk> {
        val userId = currentUserId ?: return emptyList()

        return try {
            supabase.from(manualTable)
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id",   userId)
                        eq("is_manual", true)
                    }
                }
                .decodeList<ManualWalk>()
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "❌ Failed to fetch manual walks: ${e.message}", e)
            emptyList()
        }
    }

    fun observeManualWalks(): Flow<List<ManualWalk>> {
        val userId = currentUserId ?: return emptyFlow()

        return try {
            supabase.channel("manual_walks_channel")
                .postgresChangeFlow<PostgresAction>(schema = "public") {
                    table  = manualTable
                    filter = "user_id=eq.$userId"
                }
                .map { fetchAllManualWalks() }
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "❌ Failed to start real-time listener: ${e.message}", e)
            emptyFlow()
        }
    }

    suspend fun deleteManualWalk(date: LocalDate) {
        syncManualWalk(date, isWalked = false, distanceKm = 0.0)
    }
}