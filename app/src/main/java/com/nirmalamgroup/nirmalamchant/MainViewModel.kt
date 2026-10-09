package com.nirmalamgroup.nirmalamchant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.nirmalamgroup.nirmalamchant.data.ChantRepository
import com.nirmalamgroup.nirmalamchant.data.TallySource
import com.nirmalamgroup.nirmalamchant.tracking.ChantFeedback
import com.nirmalamgroup.nirmalamchant.tracking.FeedbackPreferences
import com.nirmalamgroup.nirmalamchant.reminders.LocalReminderScheduler
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.LocalDate
import java.time.ZoneId
import com.nirmalamgroup.nirmalamchant.data.PracticePlan
import com.nirmalamgroup.nirmalamchant.data.ChantProfile

data class DashboardState(
    val performed: List<com.nirmalamgroup.nirmalamchant.data.CompletedActivity> = emptyList(),
    val planned: List<com.nirmalamgroup.nirmalamchant.data.PracticePlan> = emptyList(),
    val streakDays: Int = 0,
    val sessionsThisWeek: Int = 0,
    val completedForInsights: List<com.nirmalamgroup.nirmalamchant.data.CompletedActivity> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ChantRepository = (application as NirmalamApplication).repository
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count
    private val _targetReached = MutableStateFlow(false)
    val targetReached: StateFlow<Boolean> = _targetReached
    private val _currentTarget = MutableStateFlow(108)
    val currentTarget: StateFlow<Int> = _currentTarget
    private val _dashboard = MutableStateFlow(DashboardState())
    val dashboard: StateFlow<DashboardState> = _dashboard
    private val _meditationToneEnabled = MutableStateFlow(FeedbackPreferences.isSoundEnabled(application))
    val meditationToneEnabled: StateFlow<Boolean> = _meditationToneEnabled
    private val _hapticsEnabled = MutableStateFlow(FeedbackPreferences.isHapticsEnabled(application))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled
    private val _defaultTarget = MutableStateFlow(FeedbackPreferences.defaultTarget(application))
    val defaultTarget: StateFlow<Int> = _defaultTarget
    private val _intention = MutableStateFlow("")
    val intention: StateFlow<String> = _intention
    private val _profiles = MutableStateFlow<List<ChantProfile>>(emptyList())
    val profiles: StateFlow<List<ChantProfile>> = _profiles
    private val _selectedProfileId = MutableStateFlow<String?>(null)
    val selectedProfileId: StateFlow<String?> = _selectedProfileId
    private val selectedPrefs = application.getSharedPreferences("chant_profile_selection", 0)
    private var selectedProfile: ChantProfile? = null

    private var currentSession: com.nirmalamgroup.nirmalamchant.data.ChantSession? = null
    private var countJob: Job? = null
    private val _canUndoManualTally = MutableStateFlow(false)
    val canUndoManualTally: StateFlow<Boolean> = _canUndoManualTally

    init {
        viewModelScope.launch {
            repository.profiles().collect { list ->
                _profiles.value = list
                if (selectedProfile == null && list.isNotEmpty()) {
                    val saved = selectedPrefs.getString("id", null)
                    chooseProfileInternal(list.firstOrNull { it.id == saved } ?: list.first())
                }
            }
        }
        viewModelScope.launch {
            // Legacy unassigned sessions remain in the database and Journey history.
            if (repository.getOrCreateActiveSession().profileId == null) { /* preserve legacy history */ }
        }
        viewModelScope.launch {
            combine(repository.completedActivities(), repository.plannedActivities(), repository.allCompletedActivities()) { performed, planned, all ->
                DashboardState(performed, planned, calculateStreak(all), sessionsThisWeek(all), all)
            }.collect { _dashboard.value = it }
        }
    }

    fun selectProfile(id: String) = viewModelScope.launch {
        _profiles.value.firstOrNull { it.id == id }?.let { chooseProfileInternal(it) }
    }
    private suspend fun chooseProfileInternal(profile: ChantProfile) {
        selectedProfile = profile
        _selectedProfileId.value = profile.id
        selectedPrefs.edit().putString("id", profile.id).apply()
        activateSession(repository.profileSession(profile))
    }
    fun saveChantProfile(id: String?, name: String, target: Int, seconds: Int) = viewModelScope.launch {
        if (name.isBlank() || target !in 1..10000 || seconds !in 1..3600) return@launch
        if (id == null) {
            repository.createProfile(name, target, seconds)?.let { chooseProfileInternal(it) }
        } else {
            val old = _profiles.value.firstOrNull { it.id == id } ?: return@launch
            val updated = old.copy(name = name.trim(), targetCount = target, intervalSeconds = seconds)
            repository.saveProfile(updated)
            if (_selectedProfileId.value == id) {
                selectedProfile = updated
                // New target applies to the next session; do not mutate prior counts.
            }
        }
    }

    fun addManualTally() = viewModelScope.launch {
        val session = currentSession ?: return@launch
        val result = repository.record(session, TallySource.MANUAL)
        if (result.recorded) {
            ChantFeedback.give(getApplication(), result.count)
            _canUndoManualTally.value = !result.reachedTarget
        }
        if (result.reachedTarget) _targetReached.value = true
    }

    fun undoManualTally() = viewModelScope.launch {
        val session = currentSession ?: return@launch
        if (repository.undoLatestManualTally(session.id)) {
            _canUndoManualTally.value = false
            _targetReached.value = false
        }
    }

    fun resetCurrentPractice() = viewModelScope.launch {
        val session = currentSession ?: return@launch
        if (repository.resetTallies(session.id)) {
            _count.value = 0
            _targetReached.value = false
            _canUndoManualTally.value = false
        }
    }

    fun planEveningPractice() = viewModelScope.launch {
        val now = ZonedDateTime.now()
        var scheduled = now.withHour(18).withMinute(0).withSecond(0).withNano(0)
        if (!scheduled.isAfter(now)) scheduled = scheduled.plusDays(1)
        val plan = repository.plan("Evening practice", scheduled.toInstant(), _defaultTarget.value)
        LocalReminderScheduler.schedule(getApplication(), plan.id, plan.title, plan.scheduledFor.toEpochMilli())
    }

    fun saveIntention(value: String) = viewModelScope.launch {
        val session = currentSession ?: repository.getOrCreateActiveSession()
        repository.setIntention(session.id, value)
        _intention.value = value.trim()
    }

    fun setMeditationToneEnabled(enabled: Boolean) {
        FeedbackPreferences.setSoundEnabled(getApplication(), enabled)
        _meditationToneEnabled.value = enabled
    }
    fun setHapticsEnabled(enabled: Boolean) {
        FeedbackPreferences.setHapticsEnabled(getApplication(), enabled)
        _hapticsEnabled.value = enabled
    }
    fun setDefaultTarget(value: Int) {
        val safeValue = value.coerceIn(1, 10_000)
        FeedbackPreferences.setDefaultTarget(getApplication(), safeValue)
        _defaultTarget.value = safeValue
    }
    fun startPlannedPractice(plan: PracticePlan) = viewModelScope.launch { activateSession(repository.beginSessionFromPlan(plan)) }
    fun editPlan(plan: PracticePlan, title: String, targetCount: Int, reminderEnabled: Boolean) = viewModelScope.launch {
        repository.updatePlan(plan, title, plan.scheduledFor, targetCount, reminderEnabled)
        if (reminderEnabled) {
            LocalReminderScheduler.schedule(getApplication(), plan.id, title.ifBlank { plan.title }, plan.scheduledFor.toEpochMilli())
        } else {
            LocalReminderScheduler.cancel(getApplication(), plan.id)
        }
    }
    fun skipPlan(plan: PracticePlan) = viewModelScope.launch {
        LocalReminderScheduler.cancel(getApplication(), plan.id)
        repository.skipPlan(plan)
    }
    fun deletePlan(plan: PracticePlan) = viewModelScope.launch {
        LocalReminderScheduler.cancel(getApplication(), plan.id)
        repository.deletePlan(plan)
    }
    fun postponePlan(plan: PracticePlan) = viewModelScope.launch {
        val scheduledFor = plan.scheduledFor.atZone(ZoneId.systemDefault()).plusDays(1).toInstant()
        repository.updatePlan(plan, plan.title, scheduledFor, plan.targetCount, plan.reminderEnabled)
        if (plan.reminderEnabled) {
            LocalReminderScheduler.schedule(getApplication(), plan.id, plan.title, scheduledFor.toEpochMilli())
        }
    }

    fun beginNextPractice() = viewModelScope.launch {
        val session = selectedProfile?.let { repository.nextProfileSession(it) }
            ?: repository.beginNextSession()
        activateSession(session)
    }

    private fun activateSession(session: com.nirmalamgroup.nirmalamchant.data.ChantSession) {
        currentSession = session
        _currentTarget.value = session.targetCount
        _intention.value = session.intention.orEmpty()
        _targetReached.value = false
        _count.value = 0
        _canUndoManualTally.value = false
        countJob?.cancel()
        countJob = viewModelScope.launch {
            repository.observeCount(session.id).combine(repository.observeManualCount(session.id)) { count, manual -> count to manual }
                .collect { (rawCount, manualCount) ->
                _count.value = rawCount.coerceAtMost(session.targetCount)
                _targetReached.value = rawCount >= session.targetCount
                // Manual undo must not be enabled by voice-only tallies.
                _canUndoManualTally.value = manualCount > 0 && rawCount < session.targetCount
            }
        }
    }

    private fun calculateStreak(activities: List<com.nirmalamgroup.nirmalamchant.data.CompletedActivity>): Int {
        val activeDays = activities.filter { it.tallyCount > 0 }
            .map { it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
        var day = LocalDate.now()
        var streak = 0
        while (day in activeDays) { streak++; day = day.minusDays(1) }
        return streak
    }

    private fun sessionsThisWeek(activities: List<com.nirmalamgroup.nirmalamchant.data.CompletedActivity>): Int {
        val start = LocalDate.now().minusDays(6)
        return activities.count { it.tallyCount > 0 && !it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate().isBefore(start) }
    }
}
