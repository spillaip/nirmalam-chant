package com.nirmalamgroup.nirmalamchant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ChantSession::class, ChantTally::class, PracticePlan::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chantDao(): ChantDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase = instance ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "nirmalam.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                .also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS practice_plans (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        scheduledFor INTEGER NOT NULL,
                        targetCount INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_practice_plans_scheduledFor ON practice_plans(scheduledFor)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chant_sessions ADD COLUMN intention TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chant_sessions ADD COLUMN practicePlanId TEXT")
                database.execSQL("ALTER TABLE practice_plans ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
