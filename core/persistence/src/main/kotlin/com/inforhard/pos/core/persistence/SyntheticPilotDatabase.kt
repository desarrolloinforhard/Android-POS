package com.inforhard.pos.core.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SyntheticRecordEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class SyntheticPilotDatabase : RoomDatabase() {
    abstract fun records(): SyntheticRecordDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE synthetic_records ADD COLUMN revision INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}
