package com.nirmalamgroup.nirmalamchant.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

class ChantRepository(private val dao: ChantDao) {
    private val tallyMutex = Mutex()
    suspend fun getOrCreateActiveSession(): ChantSession {
        dao.activeSession()?.let { return it }
        val session = ChantSession()
        dao.insertSession(session)
        return session
    }
    suspend fun beginNextSession(): ChantSession {
        dao.activeSession()?.let { dao.endSession(it.id, Instant.now()) }
        val session = ChantSession()
        dao.insertSession(session)
        return session
    }
    suspend fun beginSessionFromPlan(plan: PracticePlan): ChantSession {
        dao.activeSession()?.let { dao.endSession(it.id, Instant.now()) }
        val session = ChantSession(title = plan.title, targetCount = plan.targetCount, practicePlanId = plan.id)
        dao.insertSession(session)
        return session
    }
    suspend fun updateActiveTarget(session: ChantSession, targetCount: Int): ChantSession {
        val safeTarget = targetCount.coerceIn(1, 10_000)
        dao.updateSessionTarget(session.id, safeTarget)
        return session.copy(targetCount = safeTarget)
    }
    suspend fun record(session: ChantSession, source: TallySource): TallyResult = tallyMutex.withLock {
        val before = dao.count(session.id)
        if (before >= session.targetCount) {
            dao.endSession(session.id, Instant.now())
            return@withLock TallyResult(before, reachedTarget = true, recorded = false)
        }
        dao.insertTally(ChantTally(sessionId = session.id, source = source))
        val updated = before + 1
        val reached = updated >= session.targetCount
        if (reached) {
            dao.endSession(session.id, Instant.now())
            session.practicePlanId?.let { dao.updatePlanStatus(it, PlanStatus.COMPLETED) }
        }
        TallyResult(updated, reachedTarget = reached, recorded = true)
    }
    suspend fun count(sessionId: String): Int = dao.count(sessionId)
    suspend fun undoLatestManualTally(sessionId: String): Boolean = dao.deleteLatestManualTally(sessionId) > 0
    suspend fun resetTallies(sessionId: String): Boolean = tallyMutex.withLock {
        dao.deleteTalliesForSession(sessionId) > 0
    }
    fun observeCount(sessionId: String): Flow<Int> = dao.observeCount(sessionId)
    fun recentSessions(): Flow<List<ChantSession>> = dao.observeRecentSessions()
    fun completedActivities(): Flow<List<CompletedActivity>> = dao.observeCompletedActivities()
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
