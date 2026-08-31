package com.inforhard.pos.core.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Pilot-only record. It must never contain credentials or commercial payloads. */
@Entity(tableName = "synthetic_records")
data class SyntheticRecordEntity(
    @PrimaryKey val recordId: String,
    val label: String,
    val revision: Long = 0,
) {
    init {
        require(recordId.isNotBlank())
        require(label.isNotBlank())
        require(revision >= 0)
    }
}
