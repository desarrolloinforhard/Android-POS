package com.inforhard.pos.core.sync

import com.inforhard.pos.core.model.CommandState
import com.inforhard.pos.core.model.LocalCommand
import java.util.UUID

interface CommandRepository {
    fun save(command: LocalCommand)
    fun get(localId: UUID): LocalCommand?
    fun findByState(state: CommandState): List<LocalCommand>
}

class InMemoryCommandRepository : CommandRepository {
    private val commands = linkedMapOf<UUID, LocalCommand>()

    @Synchronized
    override fun save(command: LocalCommand) {
        val existing = commands[command.localId]
        require(existing == null || existing.idempotencyKey == command.idempotencyKey) {
            "Idempotency-Key cannot change for an existing local command"
        }
        commands[command.localId] = command
    }

    @Synchronized
    override fun get(localId: UUID): LocalCommand? = commands[localId]

    @Synchronized
    override fun findByState(state: CommandState): List<LocalCommand> =
        commands.values.filter { it.state == state }
}
