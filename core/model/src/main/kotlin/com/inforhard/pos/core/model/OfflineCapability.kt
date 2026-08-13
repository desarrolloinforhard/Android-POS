package com.inforhard.pos.core.model

enum class OperationClass {
    LOCAL_DRAFT,
    NON_FINANCIAL_REMOTE,
    PAYMENT,
    REFUND,
    REVERSAL,
    SETTLEMENT,
    ADMINISTRATIVE_CHANGE,
    FISCAL,
}

/**
 * Server-issued policy represented locally after contractual verification.
 * Signature format, issuer and wire payload remain pending API definition.
 */
data class OfflineCapability(
    val policyVersion: String,
    val allowedOperationClasses: Set<OperationClass>,
    val validUntilServerEpochSeconds: Long,
    val verified: Boolean,
)

data class OfflineDecision(
    val allowed: Boolean,
    val reason: OfflineDenialReason? = null,
)

enum class OfflineDenialReason {
    ALWAYS_ONLINE,
    MISSING_CAPABILITY,
    UNVERIFIED_CAPABILITY,
    EXPIRED_CAPABILITY,
    OPERATION_NOT_ALLOWED,
}
