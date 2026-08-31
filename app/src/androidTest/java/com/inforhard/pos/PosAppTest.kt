package com.inforhard.pos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class PosAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun assistanceAndCancellationAreExplicitlyLocal() {
        val controller = PosShellController()
        composeRule.setContent { PosApp(controller.state, controller, inactivityTimeoutMillis = 60_000) }

        composeRule.onNodeWithTag("start_button").performClick()
        composeRule.onNodeWithTag("assistance_button").performClick()
        composeRule.onNodeWithTag("assistance_title").assertIsDisplayed()
        composeRule.onNodeWithText("Este aviso es únicamente local; no se contactó ningún servicio.").assertIsDisplayed()
        composeRule.onNodeWithText("Volver al carrito").performClick()
        composeRule.onNodeWithTag("cancel_button").performClick()
        composeRule.onNodeWithTag("cancel_title").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_cancel_button").performClick()
        composeRule.onNodeWithTag("start_button").assertIsDisplayed()
    }

    @Test
    fun inactivityReturnsToWelcomeWithoutCommercialAction() {
        val controller = PosShellController()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { PosApp(controller.state, controller, inactivityTimeoutMillis = 1_000) }

        composeRule.onNodeWithTag("start_button").performClick()
        composeRule.mainClock.advanceTimeBy(1_100)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("start_button").assertIsDisplayed()
        composeRule.onNodeWithText("Sesión local cerrada por inactividad").assertIsDisplayed()
    }
}
