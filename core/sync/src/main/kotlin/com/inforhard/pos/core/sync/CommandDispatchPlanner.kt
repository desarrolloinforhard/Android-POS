package com.inforhard.pos.core.sync

import com.inforhard.pos.core.model.CommandState
import com.inforhard.pos.core.model.LocalCommand

data class CommandDispatchPlan(
    val reconcileFirst: List<LocalCommand>,
    val sendQueued: List<LocalCommand>,
)

class CommandDispatchPlanner(
    private val repository: CommandRepository,
) {
    fun planAfterProcessStart(): CommandDispatchPlan {
        repository.recoverInterruptedSending()
        val uncertain = repository.findByState(CommandState.UNCERTAIN)
        if (uncertain.isNotEmpty()) {
            return CommandDispatchPlan(
                reconcileFirst = uncertain,
                sendQueued = emptyList(),
            )
        }
        return CommandDispatchPlan(
            reconcileFirst = emptyList(),
            sendQueued = repository.findByState(CommandState.QUEUED),
        )
    }
}
