package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.CommandState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandStateMachineTest {
    @Test
    fun timeoutCanMoveSendingToUncertain() {
        assertTrue(CommandStateMachine.canTransition(CommandState.SENDING, CommandState.UNCERTAIN))
    }

    @Test
    fun timeoutCannotRejectAutomatically() {
        assertFalse(CommandStateMachine.canTransition(CommandState.UNCERTAIN, CommandState.CANCELLED))
    }
}

