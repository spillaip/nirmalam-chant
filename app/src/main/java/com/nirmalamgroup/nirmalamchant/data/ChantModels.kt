package com.nirmalamgroup.nirmalamchant.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.time.Instant
import java.util.UUID

@Entity(tableName = "chant_sessions")
data class ChantSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val startedAt: Instant = Instant.now(),
    val endedAt: Instant? = null,
    val title: String = "Daily practice",
    val targetCount: Int = 108,
    val intention: String? = null,
    val practicePlanId: String? = null
)

@Entity(
    tableName = "chant_tallies",
    foreignKeys = [ForeignKey(
        entity = ChantSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId"), Index("recordedAt")]
)
data class ChantTally(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    /** Microsecond-resolution timestamp recorded off the UI thread. */
    val recordedAt: Instant = Instant.now(),
    val source: TallySource
)

enum class TallySource { VOICE, MANUAL, HARDWARE_BUTTON }

data class TallyResult(val count: Int, val reachedTarget: Boolean, val recorded: Boolean)

@Entity(tableName = "practice_plans", indices = [Index("scheduledFor")])
data class PracticePlan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val scheduledFor: Instant,
    val targetCount: Int = 108,
    val status: PlanStatus = PlanStatus.PLANNED,
    val reminderEnabled: Boolean = true
)

enum class PlanStatus { PLANNED, COMPLETED, SKIPPED }

data class CompletedActivity(
    val id: String,
    val title: String,
    val startedAt: Instant,
    val targetCount: Int,
    val tallyCount: Int,
    val intention: String?
)

class Converters {
    @TypeConverter fun instantToLong(value: Instant?): Long? = value?.let {
        Math.addExact(Math.multiplyExact(it.epochSecond, 1_000_000L), it.nano.toLong() / 1_000L)
    }
    @TypeConverter fun longToInstant(value: Long?): Instant? = value?.let {
        Instant.ofEpochSecond(Math.floorDiv(it, 1_000_000L), Math.floorMod(it, 1_000_000L) * 1_000L)
    }
    @TypeConverter fun sourceToString(value: TallySource): String = value.name
    @TypeConverter fun stringToSource(value: String): TallySource = TallySource.valueOf(value)
    @TypeConverter fun planStatusToString(value: PlanStatus): String = value.name
    @TypeConverter fun stringToPlanStatus(value: String): PlanStatus = PlanStatus.valueOf(value)
}
