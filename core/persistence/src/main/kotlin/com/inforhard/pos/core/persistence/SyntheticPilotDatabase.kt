package com.inforhard.pos.core.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SyntheticRecordEntity::class, SyntheticSnapshotEntity::class, LocalCommandEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class SyntheticPilotDatabase : RoomDatabase() {
    abstract fun records(): SyntheticRecordDao
    abstract fun snapshots(): SyntheticSnapshotDao
    abstract fun commands(): LocalCommandDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE synthetic_records ADD COLUMN revision INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS synthetic_snapshots (
                        snapshotId TEXT NOT NULL PRIMARY KEY,
                        sequence INTEGER NOT NULL,
                        checksum TEXT NOT NULL,
                        active INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS local_commands (
                        localId TEXT NOT NULL PRIMARY KEY,
                        idempotencyKey TEXT NOT NULL,
                        state TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
