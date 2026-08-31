package com.inforhard.pos.core.persistence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyntheticPilotDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "synthetic-pilot-test.db"

    @Before
    fun cleanBefore() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun cleanAfter() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migratesVersionOneThroughThreeWithoutLosingSyntheticRecord() {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL(
                "CREATE TABLE synthetic_records (recordId TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL)",
            )
            database.execSQL("INSERT INTO synthetic_records (recordId, label) VALUES ('record-a', 'fixture')")
            database.version = 1
        }

        val database = openDatabase()
        try {
            assertEquals(
                SyntheticRecordEntity("record-a", "fixture", revision = 0),
                database.records().get("record-a"),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun migratesVersionTwoToThreeAndCreatesEmptySnapshotStore() {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL(
                "CREATE TABLE synthetic_records (recordId TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, revision INTEGER NOT NULL)",
            )
            database.execSQL("INSERT INTO synthetic_records VALUES ('record-v2', 'fixture', 2)")
            database.version = 2
        }

        val database = openDatabase()
        try {
            assertEquals(2L, database.records().get("record-v2")?.revision)
            assertNull(database.snapshots().active())
        } finally {
            database.close()
        }
    }

    @Test
    fun publishesAndRollsBackSyntheticSnapshotsAtomically() {
        val database = openDatabase()
        try {
            val store = SyntheticSnapshotStore(database)
            val first = SyntheticSnapshotEntity("snapshot-1", 1, "checksum-1")
            val second = SyntheticSnapshotEntity("snapshot-2", 2, "checksum-2")

            store.publish(first)
            assertEquals("snapshot-1", database.snapshots().active()?.snapshotId)
            store.publish(second)
            assertEquals("snapshot-2", database.snapshots().active()?.snapshotId)

            store.rollbackTo(first.snapshotId)
            assertEquals("snapshot-1", database.snapshots().active()?.snapshotId)
            assertEquals(false, database.snapshots().get(second.snapshotId)?.active)
        } finally {
            database.close()
        }
    }

    @Test
    fun rollbackToUnknownSnapshotPreservesActiveVersion() {
        val database = openDatabase()
        try {
            val store = SyntheticSnapshotStore(database)
            store.publish(SyntheticSnapshotEntity("snapshot-safe", 1, "checksum-safe"))

            runCatching { store.rollbackTo("missing") }

            assertEquals("snapshot-safe", database.snapshots().active()?.snapshotId)
        } finally {
            database.close()
        }
    }

    @Test
    fun failedTransactionRollsBackEntireSyntheticWrite() {
        val database = openDatabase()
        try {
            runCatching {
                database.runInTransaction {
                    database.records().save(SyntheticRecordEntity("rollback", "fixture", revision = 1))
                    error("synthetic failure")
                }
            }

            assertNull(database.records().get("rollback"))
            assertEquals(0, database.records().count())
        } finally {
            database.close()
        }
    }

    private fun openDatabase() = Room.databaseBuilder(
        context,
        SyntheticPilotDatabase::class.java,
        databaseName,
    ).addMigrations(
        SyntheticPilotDatabase.MIGRATION_1_2,
        SyntheticPilotDatabase.MIGRATION_2_3,
    ).build()
}
