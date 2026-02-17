package com.layzbug.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class FirebaseWalkEntry(
    val date: String = "",
    val isWalked: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Singleton
class FirebaseRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val isLoggedIn: Boolean get() = auth.currentUser != null
    val currentUserId: String? get() = auth.currentUser?.uid

    // Get user's walks collection
    private fun userWalksCollection(userId: String) =
        firestore.collection("users").document(userId).collection("walks")

    // Sync a single walk to Firestore
    suspend fun syncWalkToFirebase(date: LocalDate, isWalked: Boolean) {
        val userId = currentUserId ?: return

        try {
            val entry = FirebaseWalkEntry(
                date = date.toString(),
                isWalked = isWalked,
                updatedAt = System.currentTimeMillis()
            )
            userWalksCollection(userId)
                .document(date.toString())
                .set(entry, SetOptions.merge())
                .await()

            Log.d("Firebase", "Synced walk for $date")
        } catch (e: Exception) {
            Log.e("Firebase", "Failed to sync walk: ${e.message}")
        }
    }

    // Listen to Firestore changes and emit them
    fun observeWalks(): Flow<List<FirebaseWalkEntry>> = callbackFlow {
        val userId = currentUserId ?: run {
            close()
            return@callbackFlow
        }

        val listener = userWalksCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firebase", "Listen failed: ${error.message}")
                    return@addSnapshotListener
                }

                val walks = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirebaseWalkEntry::class.java)
                } ?: emptyList()

                trySend(walks)
            }

        awaitClose { listener.remove() }
    }

    // Pull all walks from Firestore once
    suspend fun fetchAllWalks(): List<FirebaseWalkEntry> {
        val userId = currentUserId ?: return emptyList()

        return try {
            val snapshot = userWalksCollection(userId).get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirebaseWalkEntry::class.java)
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Failed to fetch walks: ${e.message}")
            emptyList()
        }
    }
}