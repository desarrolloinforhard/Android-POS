package com.inforhard.pos.core.hardware

import android.view.KeyEvent

/** Routes scan keystrokes before focused views, without consuming standalone navigation. */
class AndroidHidKeyRouter {
    private var adapter = AndroidHidKeyAdapter(HidBarcodeScanner())
    private val consumedKeys = mutableSetOf<Pair<Int, Int>>()
    private var sequenceDevice: Int? = null

    fun dispatch(event: KeyEvent, scanEnabled: Boolean, onResult: (ScanResult) -> Unit): Boolean {
        val key = event.deviceId to event.keyCode
        if (event.action == KeyEvent.ACTION_UP) return consumedKeys.remove(key)
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (key in consumedKeys) return true
        if (!scanEnabled || event.repeatCount > 0) return false
        if (sequenceDevice != null && sequenceDevice != event.deviceId) return false

        val terminator = event.keyCode == KeyEvent.KEYCODE_ENTER ||
            event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || event.keyCode == KeyEvent.KEYCODE_ESCAPE
        if (terminator && sequenceDevice == null) return false
        if (!terminator && (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed)) return false

        val result = adapter.onKeyEvent(event)
        if (result == ScanResult.Ignored) return false
        consumedKeys.add(key)
        sequenceDevice = if (result == ScanResult.Collecting) event.deviceId else null
        onResult(result)
        return true
    }

    fun reset() {
        adapter = AndroidHidKeyAdapter(HidBarcodeScanner())
        consumedKeys.clear()
        sequenceDevice = null
    }
}
