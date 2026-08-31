package com.inforhard.pos.core.persistence

import com.inforhard.pos.core.model.CommandState
import com.inforhard.pos.core.model.IdempotencyKey
import com.inforhard.pos.core.model.LocalCommand
import com.inforhard.pos.core.sync.CommandRepository
import java.util.UUID

class RoomCommandRepository(
    private val database: SyntheticPilotDatabase,
) : CommandRepository {
    override fun save(command: LocalCommand) {
        database.runInTransaction {
            val existing = database.commands().get(command.localId.toString())
            require(existing == null || existing.idempotencyKey == command.idempotencyKey.value) {
                "Idempotency-Key cannot change for an existing local command"
            }
            database.commands().save(command.toEntity())
        }
    }

    override fun get(localId: UUID): LocalCommand? =
        database.commands().get(localId.toString())?.toModel()

    override fun findByState(state: CommandState): List<LocalCommand> =
        database.commands().findByState(state.name).map { it.toModel() }

    private fun LocalCommand.toEntity() = LocalCommandEntity(
        localId = localId.toString(),
        idempotencyKey = idempotencyKey.value,
        state = state.name,
    )

    private fun LocalCommandEntity.toModel() = LocalCommand(
        localId = UUID.fromString(localId),
        idempotencyKey = IdempotencyKey(idempotencyKey),
        state = CommandState.valueOf(state),
    )
}
