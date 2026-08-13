package com.inforhard.pos.core.model

data class DiagnosticEvidence(
    val requestId: String?,
    val errorCode: String?,
    val attemptCount: Int,
    val message: String,
) {
    init {
        require(attemptCount >= 0)
    }
}
