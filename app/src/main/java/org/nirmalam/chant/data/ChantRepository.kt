package org.nirmalam.chant.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant

class ChantRepository(private val dao: ChantDao) {
    suspend fun getOrCreateActiveSession(): ChantSession {
        dao.activeSession()?.let { return it }
        val session = ChantSession()
        dao.insertSession(session)
        return session
    }
    suspend fun record(sessionId: String, source: TallySource) = dao.insertTally(ChantTally(sessionId = sessionId, source = source))
    suspend fun count(sessionId: String): Int = dao.count(sessionId)
    fun observeCount(sessionId: String): Flow<Int> = dao.observeCount(sessionId)
    fun recentSessions(): Flow<List<ChantSession>> = dao.observeRecentSessions()
    fun completedActivities(): Flow<List<CompletedActivity>> = dao.observeCompletedActivities()
    fun plannedActivities(): Flow<List<PracticePlan>> = dao.observePlannedActivities()
    suspend fun plan(title: String, scheduledFor: Instant, targetCount: Int = 108): PracticePlan {
        val plan = PracticePlan(title = title, scheduledFor = scheduledFor, targetCount = targetCount)
        dao.insertPlan(plan)
        return plan
    }
    suspend fun setIntention(sessionId: String, intention: String?): Int =
        dao.updateIntention(sessionId, intention?.trim()?.ifBlank { null })
}
