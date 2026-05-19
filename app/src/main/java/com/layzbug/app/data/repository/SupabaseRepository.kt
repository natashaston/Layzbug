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

// ── Existing manual walks model (unchanged) ───────────────────────────────────
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

// ── New: auto-detected walk sessions (Google Fit backfill + step detector) ────
// Each row = one walking session (not one day). Multiple rows per day possible.
// Supabase table: auto_walks
// Schema:
//   id          uuid primary key default gen_random_uuid()
//   user_id     text not null
//   walk_date   date not null
//   start_time  timestamptz not null
//   end_time    timestamptz not null
//   duration_minutes bigint not null
//   distance_km real not null default 0
//   step_count  bigint not null default 0
//   steps_per_minute bigint not null default 0
//   is_qualified boolean not null default true
//   reject_reason text
//   source      text not null  -- 'google_fit' | 'step_detector' | 'health_connect'
//   created_at  timestamptz default now()
@Serializable
data class AutoWalkSession(
    val id: String? = null,
    @SerialName("user_id")          val userId: String,
    @SerialName("walk_date")        val walkDate: String,
    @SerialName("start_time")       val startTime: String,
    @SerialName("end_time")         val endTime: String,
    @SerialName("duration_minutes") val durationMinutes: Long,
    @SerialName("distance_km")      val distanceKm: Double = 0.0,
    @SerialName("step_count")       val stepCount: Long = 0L,
    @SerialName("steps_per_minute") val stepsPerMinute: Long = 0L,
    @SerialName("is_qualified")     val isQualified: Boolean = true,
    @SerialName("reject_reason")    val rejectReason: String? = null,
    @SerialName("source")           val source: String = "google_fit"
)

@Singleton
class SupabaseRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val context: Context
) {
    private val manualTable = "manual_walks"
    private val autoTable   = "auto_walks"

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

    // ── Manual walks (existing — unchanged) ──────────────────────────────────

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

    // ── Auto-detected sessions (Google Fit backfill + step detector) ─────────

    /**
     * Upsert a batch of auto-detected sessions for a given date.
     * Called after Google Fit backfill or step detector session completion.
     * Deletes existing auto sessions for the date first to avoid duplicates,
     * then inserts the new batch.
     */
    suspend fun upsertAutoWalkSessions(
        date: LocalDate,
        sessions: List<AutoWalkSession>
    ) {
        val userId = currentUserId
        if (userId == null) {
            Log.w("SupabaseRepository", "⚠️ Not logged in, skipping auto walk sync for $date")
            return
        }

        val dateStr = date.toString()

        try {
            // Delete existing auto sessions for this date + source combination
            // This allows re-running the backfill without creating duplicates
            val firstSource = sessions.firstOrNull()?.source ?: "google_fit"
            supabase.from(autoTable).delete {
                filter {
                    eq("user_id",   userId)
                    eq("walk_date", dateStr)
                    eq("source",    firstSource)
                }
            }

            // Insert new sessions
            if (sessions.isNotEmpty()) {
                val sessionsWithUser = sessions.map { it.copy(userId = userId, walkDate = dateStr) }
                supabase.from(autoTable).insert(sessionsWithUser)
                Log.d("SupabaseRepository", "✅ Upserted ${sessions.size} auto sessions for $dateStr")
            }
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "❌ Failed to upsert auto sessions for $dateStr: ${e.message}", e)
        }
    }

    /**
     * Fetch all auto-detected sessions from Supabase.
     * Used when restoring data on a new device.
     */
    suspend fun fetchAllAutoWalkSessions(): List<AutoWalkSession> {
        val userId = currentUserId ?: return emptyList()

        return try {
            supabase.from(autoTable)
                .select(columns = Columns.ALL) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<AutoWalkSession>()
        } catch (e: Exception) {
            Log.e("SupabaseRepository", "❌ Failed to fetch auto sessions: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Check whether a Google Fit backfill has already been done for this user.
     * Avoids re-running the expensive backfill on every sign-in.
     */
    suspend fun hasGoogleFitBackfillCompleted(): Boolean {
        val userId = currentUserId ?: return false

        return try {
            val count = supabase.from(autoTable)
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                        eq("source",  "google_fit")
                    }
                    limit(1)
                }
                .decodeList<AutoWalkSession>()
            count.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}