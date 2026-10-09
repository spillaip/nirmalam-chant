package com.nirmalamgroup.nirmalamchant.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

class ChantRepository(private val dao: ChantDao) {
    private val tallyMutex = Mutex()
    fun profiles(): Flow<List<ChantProfile>> = dao.observeProfiles()
    suspend fun createProfile(name: String, target: Int, seconds: Int): ChantProfile? {
        if (dao.profileCount() >= 5) return null
        return ChantProfile(name = name.trim().take(80), targetCount = target.coerceIn(1, 10000), intervalSeconds = seconds.coerceIn(1, 3600)).also { dao.insertProfile(it) }
    }
    suspend fun saveProfile(profile: ChantProfile) = dao.updateProfile(profile.id, profile.name.trim().take(80), profile.targetCount.coerceIn(1, 10000), profile.intervalSeconds.coerceIn(1, 3600))
    suspend fun profileSession(profile: ChantProfile) = dao.getOrCreateProfileSession(profile)
    suspend fun nextProfileSession(profile: ChantProfile) = dao.newProfileSession(profile)

    suspend fun getOrCreateActiveSession(): ChantSession = dao.getOrCreateSessionSafely()
    suspend fun beginNextSession(): ChantSession = dao.startSessionSafely()
    suspend fun beginSessionFromPlan(plan: PracticePlan): ChantSession =
        dao.startSessionSafely(plan.title, plan.targetCount, plan.id)
    suspend fun updateActiveTarget(session: ChantSession, targetCount: Int): ChantSession {
        val safeTarget = targetCount.coerceIn(1, 10_000)
        dao.updateSessionTarget(session.id, safeTarget)
        return session.copy(targetCount = safeTarget)
    }
    suspend fun record(session: ChantSession, source: TallySource): TallyResult =
        dao.recordTallySafely(session.id, source)
    suspend fun count(sessionId: String): Int = dao.count(sessionId)
    suspend fun undoLatestManualTally(sessionId: String): Boolean = dao.deleteLatestManualTally(sessionId) > 0
    suspend fun resetTallies(sessionId: String): Boolean = tallyMutex.withLock {
        dao.deleteTalliesForSession(sessionId) > 0
    }
    fun observeCount(sessionId: String): Flow<Int> = dao.observeCount(sessionId)
    fun observeManualCount(sessionId: String): Flow<Int> = dao.observeManualCount(sessionId)
    fun recentSessions(): Flow<List<ChantSession>> = dao.observeRecentSessions()
    fun completedActivities(): Flow<List<CompletedActivity>> = dao.observeCompletedActivities()
    fun allCompletedActivities(): Flow<List<CompletedActivity>> = dao.observeAllCompletedActivities()
    fun plannedActivities(): Flow<List<PracticePlan>> = dao.observePlannedActivities()
    suspend fun plan(title: String, scheduledFor: Instant, targetCount: Int = 108): PracticePlan {
        val plan = PracticePlan(title = title, scheduledFor = scheduledFor, targetCount = targetCount)
        dao.insertPlan(plan)
        return plan
    }
    suspend fun updatePlan(plan: PracticePlan, title: String, scheduledFor: Instant, targetCount: Int, reminderEnabled: Boolean): Int =
        dao.updatePlan(plan.id, title.trim().ifBlank { plan.title }, scheduledFor, targetCount.coerceIn(1, 10_000), reminderEnabled)
    suspend fun skipPlan(plan: PracticePlan): Int = dao.updatePlanStatus(plan.id, PlanStatus.SKIPPED)
    suspend fun deletePlan(plan: PracticePlan): Int = dao.deletePlan(plan.id)
    suspend fun setIntention(sessionId: String, intention: String?): Int =
        dao.updateIntention(sessionId, intention?.trim()?.ifBlank { null })
}
