package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.DerivedTerminalContext
import com.inforhard.pos.core.model.DeviceIdentityState
import com.inforhard.pos.core.model.LocalSessionSnapshot
import com.inforhard.pos.core.model.OperatorSessionState

sealed interface SessionEvent {
    data class DeviceEnrolled(val context: DerivedTerminalContext) : SessionEvent
    data class DeviceStateChanged(val state: DeviceIdentityState) : SessionEvent
    data object OperatorAuthenticated : SessionEvent
    data object OperatorExpired : SessionEvent
    data object OperatorLoggedOut : SessionEvent
}

object SessionStateMachine {
    fun reduce(current: LocalSessionSnapshot, event: SessionEvent): LocalSessionSnapshot =
        when (event) {
            is SessionEvent.DeviceEnrolled -> current.copy(
                deviceState = DeviceIdentityState.ENROLLED,
                operatorState = OperatorSessionState.SIGNED_OUT,
                derivedContext = event.context,
            )

            is SessionEvent.DeviceStateChanged -> when (event.state) {
                DeviceIdentityState.ENROLLED -> error("Enrollment requires server-derived context")
                else -> current.copy(
                    deviceState = event.state,
                    operatorState = OperatorSessionState.SIGNED_OUT,
                    derivedContext = null,
                )
            }

            SessionEvent.OperatorAuthenticated -> {
                check(current.deviceState == DeviceIdentityState.ENROLLED) {
                    "Operator authentication requires an enrolled device"
                }
                current.copy(operatorState = OperatorSessionState.ACTIVE)
            }

            SessionEvent.OperatorExpired -> {
                check(current.deviceState == DeviceIdentityState.ENROLLED) {
                    "Operator expiry requires an enrolled device"
                }
                current.copy(operatorState = OperatorSessionState.EXPIRED)
            }
            SessionEvent.OperatorLoggedOut -> current.copy(operatorState = OperatorSessionState.SIGNED_OUT)
        }
}

object IntegrationPolicy {
    const val REAL_SERVICES_ENABLED: Boolean = false

    fun canAttemptRemoteWrite(snapshot: LocalSessionSnapshot): Boolean =
        REAL_SERVICES_ENABLED &&
            snapshot.deviceState == DeviceIdentityState.ENROLLED &&
            snapshot.operatorState == OperatorSessionState.ACTIVE
}
