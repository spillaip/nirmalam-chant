package org.nirmalam.chant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.nirmalam.chant.data.ChantRepository
import org.nirmalam.chant.data.TallySource
import org.nirmalam.chant.tracking.ChantFeedback
import org.nirmalam.chant.tracking.FeedbackPreferences
import org.nirmalam.chant.reminders.LocalReminderScheduler
import java.time.ZonedDateTime
import java.time.LocalDate
import java.time.ZoneId

data class DashboardState(
    val performed: List<org.nirmalam.chant.data.CompletedActivity> = emptyList(),
    val planned: List<org.nirmalam.chant.data.PracticePlan> = emptyList(),
    val streakDays: Int = 0,
    val sessionsThisWeek: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ChantRepository = (application as NirmalamApplication).repository
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count
    private val _targetReached = MutableStateFlow(false)
    val targetReached: StateFlow<Boolean> = _targetReached
    private val _dashboard = MutableStateFlow(DashboardState())
    val dashboard: StateFlow<DashboardState> = _dashboard
    private val _meditationToneEnabled = MutableStateFlow(FeedbackPreferences.isSoundEnabled(application))
    val meditationToneEnabled: StateFlow<Boolean> = _meditationToneEnabled
    private var currentSession: org.nirmalam.chant.data.ChantSession? = null
    private var countJob: Job? = null

    init {
        viewModelScope.launch {
            activateSession(repository.getOrCreateActiveSession())
        }
        viewModelScope.launch {
            repository.completedActivities().combine(repository.plannedActivities()) { performed, planned ->
                DashboardState(performed, planned, calculateStreak(performed), sessionsThisWeek(performed))
            }.collect { _dashboard.value = it }
        }
    }

    fun addManualTally() = viewModelScope.launch {
        val session = currentSession ?: repository.getOrCreateActiveSession().also { currentSession = it }
        val result = repository.record(session, TallySource.MANUAL)
        if (result.recorded) ChantFeedback.give(getApplication(), result.count)
        if (result.reachedTarget) _targetReached.value = true
    }

    fun planEveningPractice() = viewModelScope.launch {
        val now = ZonedDateTime.now()
        var scheduled = now.withHour(18).withMinute(0).withSecond(0).withNano(0)
        if (!scheduled.isAfter(now)) scheduled = scheduled.plusDays(1)
        val plan = repository.plan("Evening practice", scheduled.toInstant())
        LocalReminderScheduler.schedule(getApplication(), plan.id, plan.title, plan.scheduledFor.toEpochMilli())
    }

    fun saveIntention(value: String) = viewModelScope.launch {
        repository.setIntention(repository.getOrCreateActiveSession().id, value)
    }

    fun setMeditationToneEnabled(enabled: Boolean) {
        FeedbackPreferences.setSoundEnabled(getApplication(), enabled)
        _meditationToneEnabled.value = enabled
    }

    fun beginNextPractice() = viewModelScope.launch {
        activateSession(repository.beginNextSession())
    }

    private fun activateSession(session: org.nirmalam.chant.data.ChantSession) {
        currentSession = session
        _targetReached.value = false
        countJob?.cancel()
        countJob = viewModelScope.launch {
            repository.observeCount(session.id).collect { rawCount ->
                _count.value = rawCount.coerceAtMost(session.targetCount)
                if (rawCount >= session.targetCount) _targetReached.value = true
            }
        }
    }

    private fun calculateStreak(activities: List<org.nirmalam.chant.data.CompletedActivity>): Int {
        val activeDays = activities.filter { it.tallyCount > 0 }
            .map { it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate() }.toSet()
        var day = LocalDate.now()
        var streak = 0
        while (day in activeDays) { streak++; day = day.minusDays(1) }
        return streak
    }

    private fun sessionsThisWeek(activities: List<org.nirmalam.chant.data.CompletedActivity>): Int {
        val start = LocalDate.now().minusDays(6)
        return activities.count { it.tallyCount > 0 && !it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate().isBefore(start) }
    }
}
