package com.inforhard.pos.core.model

enum class DeviceIdentityState {
    NOT_ENROLLED,
    ENROLLED,
    SUSPENDED,
    REVOKED,
    REPLACED,
}

enum class OperatorSessionState {
    SIGNED_OUT,
    ACTIVE,
    EXPIRED,
}

/** Context asserted by the server. None of these values is locally authoritative. */
data class DerivedTerminalContext(
    val companyDisplayName: String,
    val branchDisplayName: String,
    val terminalDisplayName: String,
)

data class LocalSessionSnapshot(
    val deviceState: DeviceIdentityState = DeviceIdentityState.NOT_ENROLLED,
    val operatorState: OperatorSessionState = OperatorSessionState.SIGNED_OUT,
    val derivedContext: DerivedTerminalContext? = null,
    val pendingCommandCount: Int = 0,
) {
    init {
        require(pendingCommandCount >= 0)
        require(deviceState == DeviceIdentityState.ENROLLED || derivedContext == null) {
            "Derived context is valid only for an enrolled device"
        }
        require(deviceState == DeviceIdentityState.ENROLLED || operatorState == OperatorSessionState.SIGNED_OUT) {
            "An operator session requires an enrolled device"
        }
    }
}
