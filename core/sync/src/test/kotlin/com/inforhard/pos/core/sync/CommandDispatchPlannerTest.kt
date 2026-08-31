package com.inforhard.pos.core.sync

import com.inforhard.pos.core.model.CommandState
import com.inforhard.pos.core.model.IdempotencyKey
import com.inforhard.pos.core.model.LocalCommand
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandDispatchPlannerTest {
    private val key = IdempotencyKey("0123456789abcdef0123456789abcdef")

    @Test
    fun interruptedSendingBecomesUncertainAndSuppressesQueuedSend() {
        val repository = InMemoryCommandRepository()
        val interrupted = command(CommandState.SENDING)
        val queued = command(CommandState.QUEUED)
        repository.save(interrupted)
        repository.save(queued)

        val plan = CommandDispatchPlanner(repository).planAfterProcessStart()

        assertEquals(
            listOf(interrupted.copy(state = CommandState.UNCERTAIN)),
            plan.reconcileFirst,
        )
        assertTrue(plan.sendQueued.isEmpty())
        assertEquals(CommandState.QUEUED, repository.get(queued.localId)?.state)
    }

    @Test
    fun preexistingUncertainSuppressesQueuedSend() {
        val repository = InMemoryCommandRepository()
        val uncertain = command(CommandState.UNCERTAIN)
        repository.save(uncertain)
        repository.save(command(CommandState.QUEUED))

        val plan = CommandDispatchPlanner(repository).planAfterProcessStart()

        assertEquals(listOf(uncertain), plan.reconcileFirst)
        assertTrue(plan.sendQueued.isEmpty())
    }

    @Test
    fun queuedCommandsAreEligibleOnlyWithoutUncertainState() {
        val repository = InMemoryCommandRepository()
        val queued = command(CommandState.QUEUED)
        repository.save(queued)

        val plan = CommandDispatchPlanner(repository).planAfterProcessStart()

        assertTrue(plan.reconcileFirst.isEmpty())
        assertEquals(listOf(queued), plan.sendQueued)
    }

    private fun command(state: CommandState) = LocalCommand(
        localId = UUID.randomUUID(),
        idempotencyKey = key,
        state = state,
    )
}
