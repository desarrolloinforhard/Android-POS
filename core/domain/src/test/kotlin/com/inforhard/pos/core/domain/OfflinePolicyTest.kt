package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.OfflineCapability
import com.inforhard.pos.core.model.OfflineDenialReason
import com.inforhard.pos.core.model.OperationClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePolicyTest {
    private val verifiedCapability = OfflineCapability(
        policyVersion = "synthetic-v1",
        allowedOperationClasses = setOf(OperationClass.NON_FINANCIAL_REMOTE),
        validUntilServerEpochSeconds = 2_000,
        verified = true,
    )

    @Test
    fun localDraftNeedsNoRemoteCapability() {
        assertTrue(
            OfflinePolicy.evaluate(
                operationClass = OperationClass.LOCAL_DRAFT,
                capability = null,
                serverEpochSeconds = null,
            ).allowed,
        )
    }

    @Test
    fun remoteOperationIsDeniedWithoutCapability() {
        val decision = OfflinePolicy.evaluate(
            operationClass = OperationClass.NON_FINANCIAL_REMOTE,
            capability = null,
            serverEpochSeconds = 1_000,
        )

        assertFalse(decision.allowed)
        assertEquals(OfflineDenialReason.MISSING_CAPABILITY, decision.reason)
    }

    @Test
    fun unverifiedCapabilityIsDenied() {
        val decision = OfflinePolicy.evaluate(
            operationClass = OperationClass.NON_FINANCIAL_REMOTE,
            capability = verifiedCapability.copy(verified = false),
            serverEpochSeconds = 1_000,
        )

        assertEquals(OfflineDenialReason.UNVERIFIED_CAPABILITY, decision.reason)
    }

    @Test
    fun serverTimeIsRequiredAndExpiryIsExclusive() {
        val missingTime = OfflinePolicy.evaluate(
            OperationClass.NON_FINANCIAL_REMOTE,
            verifiedCapability,
            serverEpochSeconds = null,
        )
        val expired = OfflinePolicy.evaluate(
            OperationClass.NON_FINANCIAL_REMOTE,
            verifiedCapability,
            serverEpochSeconds = 2_000,
        )

        assertEquals(OfflineDenialReason.EXPIRED_CAPABILITY, missingTime.reason)
        assertEquals(OfflineDenialReason.EXPIRED_CAPABILITY, expired.reason)
    }

    @Test
    fun financialAndAdministrativeClassesRemainBlockedEvenIfListed() {
        val prohibited = OperationClass.entries.filter {
            it !in setOf(OperationClass.LOCAL_DRAFT, OperationClass.NON_FINANCIAL_REMOTE)
        }
        val permissiveCapability = verifiedCapability.copy(
            allowedOperationClasses = OperationClass.entries.toSet(),
        )

        prohibited.forEach { operationClass ->
            val decision = OfflinePolicy.evaluate(
                operationClass,
                permissiveCapability,
                serverEpochSeconds = 1_000,
            )
            assertEquals(OfflineDenialReason.ALWAYS_ONLINE, decision.reason)
        }
    }

    @Test
    fun verifiedCurrentCapabilityCanAllowEnumeratedNonFinancialClass() {
        assertTrue(
            OfflinePolicy.evaluate(
                OperationClass.NON_FINANCIAL_REMOTE,
                verifiedCapability,
                serverEpochSeconds = 1_999,
            ).allowed,
        )
    }
}
