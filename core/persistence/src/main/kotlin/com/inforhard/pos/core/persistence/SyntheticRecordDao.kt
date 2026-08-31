package com.inforhard.pos.core.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyntheticRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(record: SyntheticRecordEntity)

    @Query("SELECT * FROM synthetic_records WHERE recordId = :recordId")
    fun get(recordId: String): SyntheticRecordEntity?

    @Query("SELECT COUNT(*) FROM synthetic_records")
    fun count(): Int
}
