package com.layzbug.app.data.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.layzbug.app.FitSyncManager
import com.layzbug.app.data.auth.AuthManager
import com.layzbug.app.data.repository.WalkRepository
import com.layzbug.app.domain.StatsValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import com.layzbug.app.data.local.WalkEntity

data class CalendarDayModel(
    val date: LocalDate,
    val walked: Boolean,
    val distanceKm: Double = 0.0,
    val minutes: Long = 0L
)

@HiltViewModel
class MonthViewModel @Inject constructor(
    private val fitSyncManager: FitSyncManager,
    private val walkRepository: WalkRepository,
    private val authManager: AuthManager,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    // Read year/month from nav args immediately — no flicker
    private val initialMonth: YearMonth = run {
        val year = savedStateHandle.get<String>("year")?.toIntOrNull() ?: YearMonth.now().year
        val month = savedStateHandle.get<String>("month")?.toIntOrNull() ?: YearMonth.now().monthValue
        YearMonth.of(year, month)
    }

    private val _currentMonth = MutableStateFlow(initialMonth)
    private val _refreshTrigger = MutableStateFlow(0)

    private val _showSignInPrompt = MutableStateFlow(false)
    val showSignInPrompt: StateFlow<Boolean> = _showSignInPrompt.asStateFlow()

    // Track if user signed in this session to avoid repeated prompts
    private var hasPromptedSignInThisSession = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val walkDays: StateFlow<List<CalendarDayModel>> = combine(
        _currentMonth,
        _refreshTrigger
    ) { month, _ -> month }
        .flatMapLatest { month ->
            walkRepository.getWalksForMonth(month.year, month.monthValue).map { entities ->
                buildCalendarDays(month, entities)
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = buildInitialCalendarDays(initialMonth)
        )

    // Now includes distance from walk entities
    @OptIn(ExperimentalCoroutinesApi::class)
    val monthStats: StateFlow<StatsValue> = combine(
        _currentMonth,
        _refreshTrigger
    ) { month, _ -> month }
        .flatMapLatest { month ->
            walkRepository.getWalksForMonth(month.year, month.monthValue).map { entities ->
                val count = entities.count { it.isWalked }
                val distanceKm = Math.round(entities.sumOf { it.distanceKm } * 10.0) / 10.0
                val totalMinutes = entities.sumOf { it.minutes }
                val monthName = month.month.name.lowercase().replaceFirstChar { it.uppercase() }
                StatsValue(
                    value = count,
                    label = "Walks in $monthName",
                    distanceKm = distanceKm,
                    totalMinutes = totalMinutes
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = buildInitialStats(initialMonth)
        )

    private fun buildCalendarDays(month: YearMonth, entities: List<WalkEntity>): List<CalendarDayModel> {
        return (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            val entity = entities.find { it.date == date }
            CalendarDayModel(
                date = date,
                walked = entity?.isWalked ?: false,
                distanceKm = entity?.distanceKm ?: 0.0,
                minutes = entity?.minutes ?: 0L
            )
        }
    }

    private fun buildInitialCalendarDays(month: YearMonth): List<CalendarDayModel> {
        val cached = walkRepository.getCachedMonthData(month.year, month.monthValue)
        return if (cached != null) {
            buildCalendarDays(month, cached)
        } else {
            emptyList()
        }
    }

    private fun buildInitialStats(month: YearMonth): StatsValue {
        val cached = walkRepository.getCachedMonthData(month.year, month.monthValue)
        val count = cached?.count { it.isWalked } ?: 0
        val distanceKm = Math.round((cached?.sumOf { it.distanceKm } ?: 0.0) * 10.0) / 10.0
        val totalMinutes = cached?.sumOf { it.minutes } ?: 0L
        val monthName = month.month.name.lowercase().replaceFirstChar { it.uppercase() }
        return StatsValue(value = count, label = "Walks in $monthName", distanceKm = distanceKm, totalMinutes = totalMinutes)
    }

    fun loadMonthData(month: YearMonth) {
        if (_currentMonth.value != month) {
            _currentMonth.value = month
            _refreshTrigger.value++
        }
    }

    fun setWalkStatus(date: LocalDate, status: Boolean) {
        viewModelScope.launch {
            Log.d("MonthViewModel", "📝 Setting walk status for $date to $status")
            walkRepository.updateManualWalk(date, status)

            _refreshTrigger.value++

            val isLoggedIn = authManager.isLoggedIn
            val userId = authManager.currentUserId
            Log.d("MonthViewModel", "🔐 Auth check - isLoggedIn: $isLoggedIn, userId: $userId")

            // Only show prompt if not logged in AND haven't prompted this session
            if (!isLoggedIn && !hasPromptedSignInThisSession) {
                Log.d("MonthViewModel", "⚠️ Not logged in, showing prompt after 2s")
                kotlinx.coroutines.delay(2000)
                _showSignInPrompt.value = true
                hasPromptedSignInThisSession = true  // Mark as prompted
                Log.d("MonthViewModel", "✅ showSignInPrompt set to: ${_showSignInPrompt.value}")
            } else {
                Log.d("MonthViewModel", "✅ Already logged in as $userId OR already prompted, skipping")
            }
        }
    }

    fun syncAfterSignIn() {
        viewModelScope.launch {
            Log.d("MonthViewModel", "🔄 Starting post-login sync...")

            // Pull data from Supabase
            Log.d("MonthViewModel", "📥 Pulling Supabase data...")
            walkRepository.syncFromSupabase()

            // Start listening for real-time changes
            Log.d("MonthViewModel", "👂 Starting real-time listener...")
            walkRepository.startSupabaseSync()

            Log.d("MonthViewModel", "✅ All sync complete!")
        }
    }

    fun launchSignIn(launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>) {
        val signInIntent = authManager.getGoogleSignInClient().signInIntent
        launcher.launch(signInIntent)
    }

    suspend fun signInWithGoogle(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount): Boolean {
        val result = authManager.signInWithGoogle(account)
        return result.isSuccess
    }

    fun dismissSignInPrompt() {
        _showSignInPrompt.value = false
    }
}