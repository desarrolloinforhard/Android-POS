package com.inforhard.pos.core.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyntheticSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(snapshot: SyntheticSnapshotEntity)

    @Query("SELECT * FROM synthetic_snapshots WHERE snapshotId = :snapshotId")
    fun get(snapshotId: String): SyntheticSnapshotEntity?

    @Query("SELECT * FROM synthetic_snapshots WHERE active = 1")
    fun active(): SyntheticSnapshotEntity?

    @Query("UPDATE synthetic_snapshots SET active = CASE WHEN snapshotId = :snapshotId THEN 1 ELSE 0 END")
    fun activateOnly(snapshotId: String)

    @Query("UPDATE synthetic_snapshots SET active = 0")
    fun deactivateAll()
}
