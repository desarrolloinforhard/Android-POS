package com.inforhard.pos

import com.inforhard.pos.core.hardware.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PosShellControllerTest {
    @Test
    fun barcodeIsDisplayedOnlyAsLocalCapture() {
        val controller = PosShellController()

        controller.onScanResult(ScanResult.Barcode("7791234567890"))

        assertEquals("7791234567890", controller.state.lastBarcode)
        assertEquals("Captura completa; sin consulta comercial", controller.state.scannerMessage)
    }

    @Test
    fun cancelledScanDoesNotCreateBarcode() {
        val controller = PosShellController()

        controller.onScanResult(ScanResult.Cancelled)

        assertNull(controller.state.lastBarcode)
        assertEquals("Lectura descartada", controller.state.scannerMessage)
    }
}
