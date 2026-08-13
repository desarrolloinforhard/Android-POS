package com.inforhard.pos.core.hardware

import org.junit.Assert.assertEquals
import org.junit.Test

class HidBarcodeScannerTest {
    @Test
    fun submitEmitsCompleteBarcodeAndClearsBuffer() {
        val scanner = HidBarcodeScanner()
        "7791234567890".forEach { scanner.accept(ScannerInput.Character(it)) }

        assertEquals(ScanResult.Barcode("7791234567890"), scanner.accept(ScannerInput.Submit))
        assertEquals(ScanResult.Ignored, scanner.accept(ScannerInput.Submit))
    }

    @Test
    fun cancelDiscardsPartialInput() {
        val scanner = HidBarcodeScanner()
        "123".forEach { scanner.accept(ScannerInput.Character(it)) }

        assertEquals(ScanResult.Cancelled, scanner.accept(ScannerInput.Cancel))
        assertEquals(ScanResult.Ignored, scanner.accept(ScannerInput.Submit))
    }

    @Test
    fun overflowCancelsInsteadOfReturningTruncatedBarcode() {
        val scanner = HidBarcodeScanner(maximumLength = 3)
        "123".forEach { scanner.accept(ScannerInput.Character(it)) }

        assertEquals(ScanResult.Cancelled, scanner.accept(ScannerInput.Character('4')))
        assertEquals(ScanResult.Ignored, scanner.accept(ScannerInput.Submit))
    }

    @Test
    fun controlCharactersAreIgnored() {
        val scanner = HidBarcodeScanner()

        assertEquals(ScanResult.Ignored, scanner.accept(ScannerInput.Character('\n')))
    }
}
