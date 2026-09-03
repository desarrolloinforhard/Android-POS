package com.inforhard.pos.core.persistence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.inforhard.pos.core.model.CommandState
import com.inforhard.pos.core.model.IdempotencyKey
import com.inforhard.pos.core.model.LocalCommand
import java.util.UUID
import com.inforhard.pos.core.sync.CommandCoordinator
import com.inforhard.pos.core.sync.CommandDispatchPlanner
import com.inforhard.pos.core.network.CommandTransport
import com.inforhard.pos.core.network.ReconciliationTransport
import com.inforhard.pos.core.network.TransportResult
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
    fun migratesVersionThreeToFourWithEmptyDurableOutbox() {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL(
                "CREATE TABLE synthetic_records (recordId TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, revision INTEGER NOT NULL)",
            )
            database.execSQL(
                "CREATE TABLE synthetic_snapshots (snapshotId TEXT NOT NULL PRIMARY KEY, sequence INTEGER NOT NULL, checksum TEXT NOT NULL, active INTEGER NOT NULL)",
            )
            database.version = 3
        }

        val database = openDatabase()
        try {
            assertEquals(emptyList<LocalCommandEntity>(), database.commands().findByState(CommandState.UNCERTAIN.name))
        } finally {
            database.close()
        }
    }

    @Test
    fun uncertainCommandAndStableIdempotencySurviveDatabaseReopen() {
        val localId = UUID.randomUUID()
        val key = IdempotencyKey("0123456789abcdef0123456789abcdef")
        val database = openDatabase()
        RoomCommandRepository(database).save(LocalCommand(localId, key, CommandState.UNCERTAIN))
        database.close()

        val reopened = openDatabase()
        try {
            assertEquals(
                LocalCommand(localId, key, CommandState.UNCERTAIN),
                RoomCommandRepository(reopened).get(localId),
            )
        } finally {
            reopened.close()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun durableRepositoryRejectsIdempotencyKeyMutation() {
        val database = openDatabase()
        try {
            val repository = RoomCommandRepository(database)
            val command = LocalCommand(
                UUID.randomUUID(),
                IdempotencyKey("0123456789abcdef0123456789abcdef"),
                CommandState.QUEUED,
            )
            repository.save(command)
            repository.save(command.copy(idempotencyKey = IdempotencyKey("fedcba9876543210fedcba9876543210")))
        } finally {
            database.close()
        }
    }

    @Test
    fun interruptedSendingRecoversAsUncertainAfterProcessRestart() {
        val localId = UUID.randomUUID()
        val alreadyUncertainId = UUID.randomUUID()
        val key = IdempotencyKey("0123456789abcdef0123456789abcdef")
        val database = openDatabase()
        RoomCommandRepository(database).apply {
            save(LocalCommand(localId, key, CommandState.SENDING))
            save(LocalCommand(alreadyUncertainId, key, CommandState.UNCERTAIN))
        }
        database.close()

        val reopened = openDatabase()
        try {
            val repository = RoomCommandRepository(reopened)

            assertEquals(
                listOf(LocalCommand(localId, key, CommandState.UNCERTAIN)),
                repository.recoverInterruptedSending(),
            )
            assertEquals(LocalCommand(localId, key, CommandState.UNCERTAIN), repository.get(localId))
            assertEquals(
                LocalCommand(alreadyUncertainId, key, CommandState.UNCERTAIN),
                repository.get(alreadyUncertainId),
            )
        } finally {
            reopened.close()
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

    @Test
    fun reopenedQueueRequiresReconciliationBeforeSendingOtherCommands() {
        val interrupted = LocalCommand(UUID.randomUUID(),
            IdempotencyKey("0123456789abcdef0123456789abcdef"), CommandState.SENDING)
        val queued = LocalCommand(UUID.randomUUID(),
            IdempotencyKey("fedcba9876543210fedcba9876543210"), CommandState.QUEUED)
        val initial = openDatabase()
        try {
            RoomCommandRepository(initial).apply { save(interrupted); save(queued) }
        } finally { initial.close() }

        val reopened = openDatabase()
        try {
            val repository = RoomCommandRepository(reopened)
            val planner = CommandDispatchPlanner(repository)
            val sent = mutableListOf<LocalCommand>()
            val reconciled = mutableListOf<LocalCommand>()
            var answer: TransportResult = TransportResult.Uncertain
            val coordinator = CommandCoordinator(repository,
                CommandTransport {
                    assertEquals(CommandState.SENDING, repository.get(it.localId)?.state)
                    sent.add(it)
                    TransportResult.Acknowledged("synthetic-send")
                },
                ReconciliationTransport {
                    reconciled.add(it)
                    answer
                })

            val firstPlan = planner.planAfterProcessStart()
            val uncertain = interrupted.copy(state = CommandState.UNCERTAIN)
            assertEquals(listOf(uncertain), firstPlan.reconcileFirst)
            assertEquals(emptyList<LocalCommand>(), firstPlan.sendQueued)
            assertEquals(queued, repository.get(queued.localId))
            assertEquals(uncertain, coordinator.reconcileUncertain(firstPlan.reconcileFirst.single()))
            val stillBlocked = planner.planAfterProcessStart()
            assertEquals(listOf(uncertain), stillBlocked.reconcileFirst)
            assertEquals(emptyList<LocalCommand>(), stillBlocked.sendQueued)
            assertEquals(emptyList<LocalCommand>(), sent)

            answer = TransportResult.Acknowledged("synthetic-reconcile")
            coordinator.reconcileUncertain(stillBlocked.reconcileFirst.single())
            val ready = planner.planAfterProcessStart()
            assertEquals(emptyList<LocalCommand>(), ready.reconcileFirst)
            assertEquals(listOf(queued), ready.sendQueued)
            coordinator.sendQueued(ready.sendQueued.single())
            assertEquals(listOf(queued.copy(state = CommandState.SENDING)), sent)
            assertEquals(listOf(uncertain, uncertain), reconciled)
            assertEquals(interrupted.copy(state = CommandState.RECONCILED), repository.get(interrupted.localId))
            assertEquals(queued.copy(state = CommandState.ACKNOWLEDGED), repository.get(queued.localId))
        } finally { reopened.close() }

        val finalDatabase = openDatabase()
        try {
            val repository = RoomCommandRepository(finalDatabase)
            assertEquals(interrupted.copy(state = CommandState.RECONCILED), repository.get(interrupted.localId))
            assertEquals(queued.copy(state = CommandState.ACKNOWLEDGED), repository.get(queued.localId))
            val plan = CommandDispatchPlanner(repository).planAfterProcessStart()
            assertEquals(emptyList<LocalCommand>(), plan.reconcileFirst)
            assertEquals(emptyList<LocalCommand>(), plan.sendQueued)
        } finally { finalDatabase.close() }
    }


    private fun openDatabase() = Room.databaseBuilder(
        context,
        SyntheticPilotDatabase::class.java,
        databaseName,
    ).addMigrations(
        SyntheticPilotDatabase.MIGRATION_1_2,
        SyntheticPilotDatabase.MIGRATION_2_3,
        SyntheticPilotDatabase.MIGRATION_3_4,
    ).build()
}
