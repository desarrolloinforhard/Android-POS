package com.inforhard.pos

import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Rule
import org.junit.Test

class ScannerFocusRegressionTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun threeScanKeySequencesDoNotActivateFocusedAssistance() {
        composeRule.onNodeWithTag("start_button").performClick()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        composeRule.onNodeWithTag("assistance_button")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .assertIsFocused()
        repeat(3) {
            composeRule.runOnIdle {
                val events = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
                    .getEvents("7790000000011".toCharArray())!!
                events.forEach { composeRule.activity.dispatchKeyEvent(it) }
                enter()
            }
        }
        composeRule.onNodeWithText("Tu carrito").assertIsDisplayed()
        composeRule.onNodeWithText("Agua sin gas").assertIsDisplayed()
        composeRule.onNodeWithText("Cantidad: 3").assertIsDisplayed()
        composeRule.onNodeWithText("Total local: ARS 375,00").assertIsDisplayed()
    }

    @Test
    fun standaloneEnterStillActivatesFocusedAssistance() {
        composeRule.onNodeWithTag("start_button").performClick()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        composeRule.onNodeWithTag("assistance_button")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .assertIsFocused()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
        composeRule.onNodeWithTag("assistance_title").assertIsDisplayed()
    }

    private fun enter() {
        val time = SystemClock.uptimeMillis()
        for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            composeRule.activity.dispatchKeyEvent(KeyEvent(
                time, time, action, KeyEvent.KEYCODE_ENTER, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_KEYBOARD,
            ))
        }
    }
}
