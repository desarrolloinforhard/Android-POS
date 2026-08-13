package com.inforhard.pos.core.network

import com.inforhard.pos.core.model.LocalCommand

sealed interface TransportResult {
    data class Acknowledged(val requestId: String) : TransportResult
    data class Rejected(val code: String) : TransportResult
    data object Uncertain : TransportResult
}

fun interface CommandTransport {
    fun send(command: LocalCommand): TransportResult
}

fun interface ReconciliationTransport {
    fun reconcile(command: LocalCommand): TransportResult
}

class FakeCommandTransport(
    private val result: TransportResult = TransportResult.Uncertain,
) : CommandTransport {
    override fun send(command: LocalCommand): TransportResult = result
}
