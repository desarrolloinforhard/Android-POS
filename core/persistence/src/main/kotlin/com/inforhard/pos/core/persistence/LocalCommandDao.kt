package com.inforhard.pos.core.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalCommandDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(command: LocalCommandEntity)

    @Query("SELECT * FROM local_commands WHERE localId = :localId")
    fun get(localId: String): LocalCommandEntity?

    @Query("SELECT * FROM local_commands WHERE state = :state ORDER BY localId")
    fun findByState(state: String): List<LocalCommandEntity>
}
