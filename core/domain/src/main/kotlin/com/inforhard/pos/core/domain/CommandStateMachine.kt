package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.CommandState

object CommandStateMachine {
    private val transitions = mapOf(
        CommandState.CREATED to setOf(CommandState.QUEUED, CommandState.CANCELLED),
        CommandState.QUEUED to setOf(CommandState.SENDING, CommandState.EXPIRED, CommandState.CANCELLED),
        CommandState.SENDING to setOf(
            CommandState.UNCERTAIN,
            CommandState.ACKNOWLEDGED,
            CommandState.REJECTED,
        ),
        CommandState.UNCERTAIN to setOf(CommandState.SENDING, CommandState.RECONCILED, CommandState.REJECTED),
        CommandState.ACKNOWLEDGED to setOf(CommandState.RECONCILED, CommandState.REJECTED),
    )

    fun canTransition(from: CommandState, to: CommandState): Boolean =
        transitions[from]?.contains(to) == true
}

