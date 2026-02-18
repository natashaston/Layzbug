package com.layzbug.app.data.viewmodel

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
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

data class CalendarDayModel(val date: LocalDate, val walked: Boolean)

@HiltViewModel
class MonthViewModel @Inject constructor(
    private val fitSyncManager: FitSyncManager,
    private val walkRepository: WalkRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _refreshTrigger = MutableStateFlow(0) // Add refresh trigger

    private val _showSignInPrompt = MutableStateFlow(false)
    val showSignInPrompt: StateFlow<Boolean> = _showSignInPrompt.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val walkDays: StateFlow<List<CalendarDayModel>> = combine(
        _currentMonth,
        _refreshTrigger // Combine with refresh trigger
    ) { month, _ -> month }
        .flatMapLatest { month ->
            // Always fetch from database, don't use cache for display
            walkRepository.getWalksForMonth(month.year, month.monthValue).map { entities ->
                buildCalendarDays(month, entities)
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = buildInitialCalendarDays(YearMonth.now())
        )

    val monthStats: StateFlow<StatsValue> = walkDays.map { days ->
        val count = days.count { it.walked }
        val monthName = _currentMonth.value.month.name.lowercase().replaceFirstChar { it.uppercase() }
        StatsValue(
            value = count,
            label = "Walks in $monthName"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = buildInitialStats(YearMonth.now())
    )

    private fun buildCalendarDays(month: YearMonth, entities: List<WalkEntity>): List<CalendarDayModel> {
        return (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            CalendarDayModel(
                date = date,
                walked = entities.find { it.date == date }?.isWalked ?: false
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
        val monthName = month.month.name.lowercase().replaceFirstChar { it.uppercase() }
        return StatsValue(value = count, label = "Walks in $monthName")
    }

    fun loadMonthData(month: YearMonth) {
        if (_currentMonth.value != month) {
            _currentMonth.value = month
        }
    }

    fun setWalkStatus(date: LocalDate, status: Boolean) {
        viewModelScope.launch {
            Log.d("MonthViewModel", "Setting walk status for $date to $status")
            walkRepository.updateWalk(date, status)

            // Trigger refresh after update
            _refreshTrigger.value++

            Log.d("MonthViewModel", "Is logged in: ${authManager.isLoggedIn}")
            if (!authManager.isLoggedIn) {
                Log.d("MonthViewModel", "Not logged in, showing prompt after 2s")
                kotlinx.coroutines.delay(2000)
                _showSignInPrompt.value = true
                Log.d("MonthViewModel", "showSignInPrompt set to: ${_showSignInPrompt.value}")
            } else {
                Log.d("MonthViewModel", "Already logged in, skipping prompt")
            }
        }
    }

    fun launchSignIn(launcher: ActivityResultLauncher<android.content.Intent>) {
        val signInIntent = authManager.getGoogleSignInClient().signInIntent
        launcher.launch(signInIntent)
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount) {
        val result = authManager.signInWithGoogle(account)
        if (result.isSuccess) {
            Log.d("MonthViewModel", "Sign in successful, syncing data...")
            walkRepository.syncFromFirebase()
            walkRepository.startFirebaseSync()
        }
    }

    fun dismissSignInPrompt() {
        _showSignInPrompt.value = false
    }
}