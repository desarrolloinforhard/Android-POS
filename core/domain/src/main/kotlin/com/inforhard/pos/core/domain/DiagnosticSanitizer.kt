package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.DiagnosticEvidence

object DiagnosticSanitizer {
    private const val REDACTED = "[REDACTED]"
    private const val MAX_MESSAGE_LENGTH = 512

    private val sensitiveAssignments = Regex(
        pattern = "(?i)\\b(authorization|bearer|token|password|passwd|bootstrap|api[_-]?key|secret)" +
            "\\s*[:=]\\s*(?:\\\"[^\\\"]*\\\"|'[^']*'|[^,;\\s}]*)",
    )
    private val bearerValue = Regex("(?i)\\bbearer\\s+[A-Za-z0-9._~+/=-]+")

    fun evidence(
        requestId: String?,
        errorCode: String?,
        attemptCount: Int,
        rawMessage: String,
    ): DiagnosticEvidence = DiagnosticEvidence(
        requestId = sanitizeIdentifier(requestId),
        errorCode = sanitizeIdentifier(errorCode),
        attemptCount = attemptCount,
        message = sanitizeMessage(rawMessage),
    )

    fun sanitizeMessage(raw: String): String = raw
        .replace(bearerValue, REDACTED)
        .replace(sensitiveAssignments) { match ->
            val field = match.groupValues[1]
            "$field=$REDACTED"
        }
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .take(MAX_MESSAGE_LENGTH)

    private fun sanitizeIdentifier(value: String?): String? = value
        ?.takeIf { SAFE_IDENTIFIER.matches(it) }
        ?.take(MAX_IDENTIFIER_LENGTH)

    private val SAFE_IDENTIFIER = Regex("[A-Za-z0-9._:-]+")
    private const val MAX_IDENTIFIER_LENGTH = 128
}
