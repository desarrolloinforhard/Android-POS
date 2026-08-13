package com.inforhard.pos.core.model

import java.util.UUID

enum class CommandState {
    CREATED,
    QUEUED,
    SENDING,
    UNCERTAIN,
    ACKNOWLEDGED,
    RECONCILED,
    REJECTED,
    EXPIRED,
    CANCELLED,
}

data class LocalCommand(
    val localId: UUID,
    val idempotencyKey: IdempotencyKey,
    val state: CommandState,
)
