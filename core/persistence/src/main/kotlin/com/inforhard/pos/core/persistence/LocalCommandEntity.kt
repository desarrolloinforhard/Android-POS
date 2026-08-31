package com.inforhard.pos.core.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Durable command metadata only; commercial payloads are intentionally absent. */
@Entity(tableName = "local_commands")
data class LocalCommandEntity(
    @PrimaryKey val localId: String,
    val idempotencyKey: String,
    val state: String,
)
