package com.inforhard.pos.core.persistence

/** Demonstrates atomic publication semantics using synthetic metadata only. */
class SyntheticSnapshotStore(
    private val database: SyntheticPilotDatabase,
) {
    fun publish(snapshot: SyntheticSnapshotEntity) {
        require(!snapshot.active) { "Callers cannot pre-activate a snapshot" }
        database.runInTransaction {
            database.snapshots().deactivateAll()
            database.snapshots().save(snapshot.copy(active = true))
        }
    }

    fun rollbackTo(snapshotId: String) {
        database.runInTransaction {
            requireNotNull(database.snapshots().get(snapshotId)) { "Unknown synthetic snapshot" }
            database.snapshots().activateOnly(snapshotId)
        }
    }
}
