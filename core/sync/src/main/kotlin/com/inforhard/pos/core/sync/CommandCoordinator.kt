package com.inforhard.pos.core.sync

import com.inforhard.pos.core.domain.CommandStateMachine
import com.inforhard.pos.core.model.CommandState
import com.inforhard.pos.core.model.LocalCommand
import com.inforhard.pos.core.network.CommandTransport
import com.inforhard.pos.core.network.ReconciliationTransport
import com.inforhard.pos.core.network.TransportResult

class CommandCoordinator(
    private val repository: CommandRepository,
    private val commandTransport: CommandTransport,
    private val reconciliationTransport: ReconciliationTransport,
) {
    fun enqueue(command: LocalCommand): LocalCommand {
        require(command.state == CommandState.CREATED)
        return transition(command, CommandState.QUEUED)
    }

    fun sendQueued(command: LocalCommand): LocalCommand {
        require(command.state == CommandState.QUEUED)
        val sending = transition(command, CommandState.SENDING)
        return applyResult(sending, commandTransport.send(sending))
    }

    fun reconcileUncertain(command: LocalCommand): LocalCommand {
        require(command.state == CommandState.UNCERTAIN)
        return applyResult(command, reconciliationTransport.reconcile(command))
    }

    private fun applyResult(command: LocalCommand, result: TransportResult): LocalCommand =
        when (result) {
            is TransportResult.Acknowledged -> transition(
                command,
                if (command.state == CommandState.UNCERTAIN) {
                    CommandState.RECONCILED
                } else {
                    CommandState.ACKNOWLEDGED
                },
            )

            is TransportResult.Rejected -> transition(command, CommandState.REJECTED)
            TransportResult.Uncertain -> if (command.state == CommandState.UNCERTAIN) {
                command
            } else {
                transition(command, CommandState.UNCERTAIN)
            }
        }

    private fun transition(command: LocalCommand, next: CommandState): LocalCommand {
        check(CommandStateMachine.canTransition(command.state, next)) {
            "Invalid command transition: ${command.state} -> $next"
        }
        return command.copy(state = next).also(repository::save)
    }
}
