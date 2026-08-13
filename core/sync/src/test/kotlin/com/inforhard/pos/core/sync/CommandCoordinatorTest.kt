package com.inforhard.pos.core.sync

import com.inforhard.pos.core.model.CommandState
import com.inforhard.pos.core.model.IdempotencyKey
import com.inforhard.pos.core.model.LocalCommand
import com.inforhard.pos.core.network.CommandTransport
import com.inforhard.pos.core.network.ReconciliationTransport
import com.inforhard.pos.core.network.TransportResult
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandCoordinatorTest {
    private val command = LocalCommand(
        localId = UUID.randomUUID(),
        idempotencyKey = IdempotencyKey("0123456789abcdef0123456789abcdef"),
        state = CommandState.CREATED,
    )

    @Test
    fun timeoutBecomesUncertainAndPreservesIdentity() {
        val repository = InMemoryCommandRepository()
        val coordinator = coordinator(repository, sendResult = TransportResult.Uncertain)

        val queued = coordinator.enqueue(command)
        val uncertain = coordinator.sendQueued(queued)

        assertEquals(CommandState.UNCERTAIN, uncertain.state)
        assertEquals(command.idempotencyKey, uncertain.idempotencyKey)
        assertEquals(uncertain, repository.get(command.localId))
    }

    @Test
    fun reconciliationQueriesBeforeAnyResend() {
        var sendCount = 0
        var reconcileCount = 0
        val repository = InMemoryCommandRepository()
        val coordinator = CommandCoordinator(
            repository = repository,
            commandTransport = CommandTransport {
                sendCount += 1
                TransportResult.Uncertain
            },
            reconciliationTransport = ReconciliationTransport {
                reconcileCount += 1
                TransportResult.Acknowledged("synthetic-request")
            },
        )

        val uncertain = coordinator.sendQueued(coordinator.enqueue(command))
        val reconciled = coordinator.reconcileUncertain(uncertain)

        assertEquals(1, sendCount)
        assertEquals(1, reconcileCount)
        assertEquals(CommandState.RECONCILED, reconciled.state)
        assertEquals(command.idempotencyKey, reconciled.idempotencyKey)
    }

    @Test(expected = IllegalArgumentException::class)
    fun repositoryRejectsChangedIdempotencyKey() {
        val repository = InMemoryCommandRepository()
        repository.save(command)

        repository.save(
            command.copy(
                idempotencyKey = IdempotencyKey("fedcba9876543210fedcba9876543210"),
            ),
        )
    }

    private fun coordinator(
        repository: CommandRepository,
        sendResult: TransportResult,
    ): CommandCoordinator = CommandCoordinator(
        repository = repository,
        commandTransport = CommandTransport { sendResult },
        reconciliationTransport = ReconciliationTransport { TransportResult.Uncertain },
    )
}
