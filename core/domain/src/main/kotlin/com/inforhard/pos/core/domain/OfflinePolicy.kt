package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.OfflineCapability
import com.inforhard.pos.core.model.OfflineDecision
import com.inforhard.pos.core.model.OfflineDenialReason
import com.inforhard.pos.core.model.OperationClass

object OfflinePolicy {
    private val alwaysOnline = setOf(
        OperationClass.PAYMENT,
        OperationClass.REFUND,
        OperationClass.REVERSAL,
        OperationClass.SETTLEMENT,
        OperationClass.ADMINISTRATIVE_CHANGE,
        OperationClass.FISCAL,
    )

    fun evaluate(
        operationClass: OperationClass,
        capability: OfflineCapability?,
        serverEpochSeconds: Long?,
    ): OfflineDecision {
        if (operationClass == OperationClass.LOCAL_DRAFT) return OfflineDecision(allowed = true)
        if (operationClass in alwaysOnline) {
            return OfflineDecision(allowed = false, reason = OfflineDenialReason.ALWAYS_ONLINE)
        }
        if (capability == null) {
            return OfflineDecision(allowed = false, reason = OfflineDenialReason.MISSING_CAPABILITY)
        }
        if (!capability.verified) {
            return OfflineDecision(allowed = false, reason = OfflineDenialReason.UNVERIFIED_CAPABILITY)
        }
        if (serverEpochSeconds == null || serverEpochSeconds >= capability.validUntilServerEpochSeconds) {
            return OfflineDecision(allowed = false, reason = OfflineDenialReason.EXPIRED_CAPABILITY)
        }
        if (operationClass !in capability.allowedOperationClasses) {
            return OfflineDecision(allowed = false, reason = OfflineDenialReason.OPERATION_NOT_ALLOWED)
        }
        return OfflineDecision(allowed = true)
    }
}
