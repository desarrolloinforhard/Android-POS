package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.DerivedTerminalContext
import com.inforhard.pos.core.model.DeviceIdentityState
import com.inforhard.pos.core.model.LocalSessionSnapshot
import com.inforhard.pos.core.model.OperatorSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SessionStateMachineTest {
    private val context = DerivedTerminalContext(
        companyDisplayName = "Empresa sintética",
        branchDisplayName = "Sucursal sintética",
        terminalDisplayName = "Terminal sintética",
    )

    @Test
    fun contextCanOnlyEnterThroughEnrollmentEvent() {
        val enrolled = SessionStateMachine.reduce(
            LocalSessionSnapshot(),
            SessionEvent.DeviceEnrolled(context),
        )

        assertEquals(DeviceIdentityState.ENROLLED, enrolled.deviceState)
        assertEquals(context, enrolled.derivedContext)
        assertEquals(OperatorSessionState.SIGNED_OUT, enrolled.operatorState)
    }

    @Test
    fun revocationSignsOutButPreservesPendingCommands() {
        val active = LocalSessionSnapshot(
            deviceState = DeviceIdentityState.ENROLLED,
            operatorState = OperatorSessionState.ACTIVE,
            derivedContext = context,
            pendingCommandCount = 3,
        )

        val revoked = SessionStateMachine.reduce(
            active,
            SessionEvent.DeviceStateChanged(DeviceIdentityState.REVOKED),
        )

        assertEquals(OperatorSessionState.SIGNED_OUT, revoked.operatorState)
        assertEquals(3, revoked.pendingCommandCount)
        assertEquals(null, revoked.derivedContext)
    }

    @Test(expected = IllegalStateException::class)
    fun operatorCannotAuthenticateBeforeDeviceEnrollment() {
        SessionStateMachine.reduce(
            LocalSessionSnapshot(),
            SessionEvent.OperatorAuthenticated,
        )
    }

    @Test
    fun noGoBlocksWritesEvenWithSyntheticActiveSession() {
        val active = LocalSessionSnapshot(
            deviceState = DeviceIdentityState.ENROLLED,
            operatorState = OperatorSessionState.ACTIVE,
            derivedContext = context,
        )

        assertFalse(IntegrationPolicy.canAttemptRemoteWrite(active))
    }
}
