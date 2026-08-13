package com.inforhard.pos.core.hardware

import android.view.KeyEvent

class AndroidHidKeyAdapter(
    private val scanner: BarcodeScanner,
) {
    fun onKeyEvent(event: KeyEvent): ScanResult {
        if (event.action != KeyEvent.ACTION_DOWN) return ScanResult.Ignored

        return when (event.keyCode) {
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> scanner.accept(ScannerInput.Submit)
            KeyEvent.KEYCODE_ESCAPE -> scanner.accept(ScannerInput.Cancel)
            else -> event.unicodeChar
                .takeIf { it != 0 }
                ?.toChar()
                ?.let { scanner.accept(ScannerInput.Character(it)) }
                ?: ScanResult.Ignored
        }
    }
}
