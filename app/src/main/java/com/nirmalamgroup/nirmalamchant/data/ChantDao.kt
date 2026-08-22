package com.nirmalamgroup.nirmalamchant.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChantDao {
    @Insert suspend fun insertSession(session: ChantSession)
    @Insert suspend fun insertTally(tally: ChantTally)
    @Insert suspend fun insertPlan(plan: PracticePlan)

    @Query("DELETE FROM chant_tallies WHERE id = (SELECT id FROM chant_tallies WHERE sessionId = :sessionId AND source = 'MANUAL' ORDER BY recordedAt DESC, rowid DESC LIMIT 1)")
    suspend fun deleteLatestManualTally(sessionId: String): Int

    @Query("DELETE FROM chant_tallies WHERE sessionId = :sessionId")
    suspend fun deleteTalliesForSession(sessionId: String): Int

    @Query("UPDATE chant_sessions SET targetCount = :targetCount WHERE id = :sessionId AND endedAt IS NULL")
    suspend fun updateSessionTarget(sessionId: String, targetCount: Int): Int

    @Query("UPDATE chant_sessions SET intention = :intention WHERE id = :sessionId")
    suspend fun updateIntention(sessionId: String, intention: String?): Int

    @Query("UPDATE chant_sessions SET endedAt = :endedAt WHERE id = :sessionId AND endedAt IS NULL")
    suspend fun endSession(sessionId: String, endedAt: java.time.Instant): Int

    @Query("UPDATE practice_plans SET status = :status WHERE id = :planId")
    suspend fun updatePlanStatus(planId: String, status: PlanStatus): Int

    @Query("UPDATE practice_plans SET title = :title, scheduledFor = :scheduledFor, targetCount = :targetCount, reminderEnabled = :reminderEnabled WHERE id = :planId")
    suspend fun updatePlan(planId: String, title: String, scheduledFor: java.time.Instant, targetCount: Int, reminderEnabled: Boolean): Int

    @Query("DELETE FROM practice_plans WHERE id = :planId")
    suspend fun deletePlan(planId: String): Int

    @Query("SELECT * FROM chant_sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSession(): ChantSession?

    @Query("SELECT COUNT(*) FROM chant_tallies WHERE sessionId = :sessionId")
    fun observeCount(sessionId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM chant_tallies WHERE sessionId = :sessionId")
    suspend fun count(sessionId: String): Int

    @Query("SELECT * FROM chant_sessions ORDER BY startedAt DESC LIMIT 30")
    fun observeRecentSessions(): Flow<List<ChantSession>>

    @Query("""
        SELECT s.id, s.title, s.startedAt, s.targetCount, COUNT(t.id) AS tallyCount, s.intention
        FROM chant_sessions s LEFT JOIN chant_tallies t ON t.sessionId = s.id
        GROUP BY s.id ORDER BY s.startedAt DESC LIMIT 10
    """)
    fun observeCompletedActivities(): Flow<List<CompletedActivity>>

    @Query("SELECT * FROM practice_plans WHERE status = 'PLANNED' ORDER BY scheduledFor ASC LIMIT 10")
    fun observePlannedActivities(): Flow<List<PracticePlan>>
}
