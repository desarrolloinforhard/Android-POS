package com.inforhard.pos.core.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Pilot-only snapshot metadata. It is not a catalog or Pricing contract. */
@Entity(tableName = "synthetic_snapshots")
data class SyntheticSnapshotEntity(
    @PrimaryKey val snapshotId: String,
    val sequence: Long,
    val checksum: String,
    val active: Boolean = false,
) {
    init {
        require(snapshotId.isNotBlank())
        require(sequence >= 0)
        require(checksum.isNotBlank())
    }
}
