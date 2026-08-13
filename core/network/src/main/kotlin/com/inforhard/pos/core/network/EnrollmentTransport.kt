package com.inforhard.pos.core.network

/**
 * Local seam for a future versioned enrollment contract.
 * No route, payload or productive behavior is defined here.
 */
fun interface EnrollmentTransport {
    fun request(): EnrollmentResult
}

sealed interface EnrollmentResult {
    data object ContractUnavailable : EnrollmentResult
}

class FakeEnrollmentTransport : EnrollmentTransport {
    override fun request(): EnrollmentResult = EnrollmentResult.ContractUnavailable
}
