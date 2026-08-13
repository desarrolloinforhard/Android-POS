package com.inforhard.pos.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticSanitizerTest {
    @Test
    fun redactsAuthenticationAndBootstrapMaterial() {
        val sanitized = DiagnosticSanitizer.sanitizeMessage(
            "Authorization: Bearer abc.def token=top-secret bootstrap='single-use'",
        )

        assertFalse(sanitized.contains("abc.def"))
        assertFalse(sanitized.contains("top-secret"))
        assertFalse(sanitized.contains("single-use"))
        assertTrue(sanitized.contains("[REDACTED]"))
    }

    @Test
    fun removesLineBreaksAndLimitsMessageLength() {
        val sanitized = DiagnosticSanitizer.sanitizeMessage("line1\nline2\t" + "x".repeat(600))

        assertFalse(sanitized.contains('\n'))
        assertFalse(sanitized.contains('\t'))
        assertEquals(512, sanitized.length)
    }

    @Test
    fun keepsSafeRequestIdAndErrorCode() {
        val evidence = DiagnosticSanitizer.evidence(
            requestId = "req_01:test",
            errorCode = "DEVICE_REVOKED",
            attemptCount = 2,
            rawMessage = "Device unavailable",
        )

        assertEquals("req_01:test", evidence.requestId)
        assertEquals("DEVICE_REVOKED", evidence.errorCode)
        assertEquals(2, evidence.attemptCount)
    }

    @Test
    fun rejectsUnsafeIdentifiersInsteadOfEscapingThem() {
        val evidence = DiagnosticSanitizer.evidence(
            requestId = "request with user@example.com",
            errorCode = "BAD\nCODE",
            attemptCount = 0,
            rawMessage = "failure",
        )

        assertNull(evidence.requestId)
        assertNull(evidence.errorCode)
    }
}
