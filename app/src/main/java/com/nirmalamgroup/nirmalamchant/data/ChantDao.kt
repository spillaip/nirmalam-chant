package com.nirmalamgroup.nirmalamchant.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface ChantDao {
    @Insert suspend fun insertProfile(profile: ChantProfile)
    @Query("UPDATE chant_profiles SET name = :name, targetCount = :target, intervalSeconds = :seconds WHERE id = :id")
    suspend fun updateProfile(id: String, name: String, target: Int, seconds: Int)
    @Query("SELECT * FROM chant_profiles ORDER BY rowid ASC")
    fun observeProfiles(): Flow<List<ChantProfile>>
    @Query("SELECT COUNT(*) FROM chant_profiles")
    suspend fun profileCount(): Int
    @Query("SELECT * FROM chant_sessions WHERE profileId = :profileId AND endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSessionForProfile(profileId: String): ChantSession?
    @Query("SELECT * FROM chant_profiles WHERE id = :id LIMIT 1")
    suspend fun profileById(id: String): ChantProfile?
    @Transaction
    suspend fun getOrCreateProfileSession(profile: ChantProfile): ChantSession {
        return activeSessionForProfile(profile.id) ?: ChantSession(title = profile.name, targetCount = profile.targetCount, profileId = profile.id).also { insertSession(it) }
    }
    @Transaction
    suspend fun newProfileSession(profile: ChantProfile): ChantSession {
        activeSessionForProfile(profile.id)?.let { endSession(it.id, Instant.now()) }
        return ChantSession(title = profile.name, targetCount = profile.targetCount, profileId = profile.id).also { insertSession(it) }
    }

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

    @Query("SELECT * FROM chant_sessions WHERE id = :id LIMIT 1")
    suspend fun sessionById(id: String): ChantSession?

    @Query("SELECT COUNT(*) FROM chant_tallies WHERE sessionId = :sessionId AND source = 'MANUAL'")
    fun observeManualCount(sessionId: String): Flow<Int>

    @Transaction
    suspend fun getOrCreateSessionSafely(): ChantSession {
        activeSession()?.let { return it }
        return ChantSession().also { insertSession(it) }
    }

    @Transaction
    suspend fun startSessionSafely(title: String = "Daily practice", targetCount: Int = 108, planId: String? = null): ChantSession {
        activeSession()?.let { endSession(it.id, Instant.now()) }
        return ChantSession(title = title, targetCount = targetCount, practicePlanId = planId)
            .also { insertSession(it) }
    }

    @Transaction
    suspend fun recordTallySafely(sessionId: String, source: TallySource): TallyResult {
        val session = sessionById(sessionId) ?: return TallyResult(0, false, false)
        val before = count(sessionId)
        if (session.endedAt != null || before >= session.targetCount) {
            return TallyResult(before, before >= session.targetCount, false)
        }
        insertTally(ChantTally(sessionId = sessionId, source = source))
        val updated = before + 1
        val reached = updated >= session.targetCount
        if (reached) {
            endSession(sessionId, Instant.now())
            session.practicePlanId?.let { updatePlanStatus(it, PlanStatus.COMPLETED) }
        }
        return TallyResult(updated, reached, true)
    }

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
        WHERE s.endedAt IS NOT NULL
        GROUP BY s.id ORDER BY s.startedAt DESC LIMIT 10
    """)
    fun observeCompletedActivities(): Flow<List<CompletedActivity>>

    // Full completed-session stream for long-term calendar and mala totals.
    // Recent history remains separately limited for the Journey list.
    @Query("""
        SELECT s.id, s.title, s.startedAt, s.targetCount, COUNT(t.id) AS tallyCount, s.intention
        FROM chant_sessions s LEFT JOIN chant_tallies t ON t.sessionId = s.id
        WHERE s.endedAt IS NOT NULL
        GROUP BY s.id ORDER BY s.startedAt DESC
    """)
    fun observeAllCompletedActivities(): Flow<List<CompletedActivity>>

    @Query("SELECT * FROM practice_plans WHERE status = 'PLANNED' ORDER BY scheduledFor ASC LIMIT 10")
    fun observePlannedActivities(): Flow<List<PracticePlan>>
}
