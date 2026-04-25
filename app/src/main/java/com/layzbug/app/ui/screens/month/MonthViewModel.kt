package com.layzbug.app.data.viewmodel

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.layzbug.app.FitSyncManager
import com.layzbug.app.data.auth.AuthManager
import com.layzbug.app.data.repository.WalkRepository
import com.layzbug.app.domain.StatsValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import com.layzbug.app.data.local.WalkEntity

// DataStore extension on Context
private val Context.dataStore by preferencesDataStore(name = "layzbug_prefs")
private val SUPPRESS_SYNC_PROMPT = booleanPreferencesKey("suppress_sync_prompt")

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
    @ApplicationContext private val context: Context,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val initialMonth: YearMonth = run {
        val year  = savedStateHandle.get<String>("year")?.toIntOrNull()  ?: YearMonth.now().year
        val month = savedStateHandle.get<String>("month")?.toIntOrNull() ?: YearMonth.now().monthValue
        YearMonth.of(year, month)
    }

    private val _currentMonth    = MutableStateFlow(initialMonth)
    private val _refreshTrigger  = MutableStateFlow(0)

    private val _showSignInPrompt = MutableStateFlow(false)
    val showSignInPrompt: StateFlow<Boolean> = _showSignInPrompt.asStateFlow()

    // Reads persisted "don't show again" from DataStore
    val suppressSyncPrompt: StateFlow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[SUPPRESS_SYNC_PROMPT] ?: false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val walkDays: StateFlow<List<CalendarDayModel>> = combine(
        _currentMonth, _refreshTrigger
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthStats: StateFlow<StatsValue> = combine(
        _currentMonth, _refreshTrigger
    ) { month, _ -> month }
        .flatMapLatest { month ->
            walkRepository.getWalksForMonth(month.year, month.monthValue).map { entities ->
                val count        = entities.count { it.isWalked }
                val distanceKm   = Math.round(entities.sumOf { it.distanceKm } * 10.0) / 10.0
                val totalMinutes = entities.sumOf { it.minutes }
                val monthName    = month.month.name.lowercase().replaceFirstChar { it.uppercase() }
                StatsValue(value = count, label = "Walks in $monthName", distanceKm = distanceKm, totalMinutes = totalMinutes)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = buildInitialStats(initialMonth)
        )

    private fun buildCalendarDays(month: YearMonth, entities: List<WalkEntity>): List<CalendarDayModel> {
        return (1..month.lengthOfMonth()).map { day ->
            val date   = month.atDay(day)
            val entity = entities.find { it.date == date }
            CalendarDayModel(
                date       = date,
                walked     = entity?.isWalked ?: false,
                distanceKm = entity?.distanceKm ?: 0.0,
                minutes    = entity?.minutes ?: 0L
            )
        }
    }

    private fun buildInitialCalendarDays(month: YearMonth): List<CalendarDayModel> {
        val cached = walkRepository.getCachedMonthData(month.year, month.monthValue)
        return if (cached != null) buildCalendarDays(month, cached) else emptyList()
    }

    private fun buildInitialStats(month: YearMonth): StatsValue {
        val cached       = walkRepository.getCachedMonthData(month.year, month.monthValue)
        val count        = cached?.count { it.isWalked } ?: 0
        val distanceKm   = Math.round((cached?.sumOf { it.distanceKm } ?: 0.0) * 10.0) / 10.0
        val totalMinutes = cached?.sumOf { it.minutes } ?: 0L
        val monthName    = initialMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
        return StatsValue(value = count, label = "Walks in $monthName", distanceKm = distanceKm, totalMinutes = totalMinutes)
    }

    fun loadMonthData(month: YearMonth) {
        if (_currentMonth.value != month) {
            _currentMonth.value = month
            _refreshTrigger.value++
        }
    }

    fun setWalkStatus(date: LocalDate, status: Boolean, previousStatus: Boolean) {
        viewModelScope.launch {
            walkRepository.updateManualWalk(date, status)
            _refreshTrigger.value++

            val isLoggedIn = authManager.isLoggedIn
            val suppressed = suppressSyncPrompt.value
            // Only prompt if the user actually changed something manually —
            // auto-walked days arrive with status == previousStatus so this stays false
            val wasManuallyChanged = status != previousStatus

            Log.d("MonthViewModel", "setWalkStatus: status=$status prev=$previousStatus changed=$wasManuallyChanged isLoggedIn=$isLoggedIn suppressed=$suppressed")

            if (wasManuallyChanged && !isLoggedIn && !suppressed) {
                kotlinx.coroutines.delay(400)
                _showSignInPrompt.value = false
                _showSignInPrompt.value = true
            }
        }
    }

    /** Persists the "don't show again" preference to DataStore. */
    fun suppressSyncPromptPermanently() {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[SUPPRESS_SYNC_PROMPT] = true
            }
            Log.d("MonthViewModel", "Sync prompt permanently suppressed")
        }
    }

    fun syncAfterSignIn() {
        viewModelScope.launch {
            // Push any locally marked walks that were made before sign-in
            walkRepository.syncPendingManualWalks()
            // Then pull from Supabase to get any walks from other devices
            walkRepository.syncFromSupabase()
            // Start real-time listener
            walkRepository.startSupabaseSync()
        }
    }

    fun launchSignIn(launcher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>) {
        val signInIntent = authManager.getGoogleSignInClient().signInIntent
        launcher.launch(signInIntent)
    }

    suspend fun signInWithGoogle(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount): Boolean {
        return authManager.signInWithGoogle(account).isSuccess
    }

    fun dismissSignInPrompt() {
        _showSignInPrompt.value = false
    }
}