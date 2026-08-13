package com.inforhard.pos.core.hardware

fun interface BarcodeScanner {
    fun accept(input: ScannerInput): ScanResult
}

sealed interface ScannerInput {
    data class Character(val value: Char) : ScannerInput
    data object Submit : ScannerInput
    data object Cancel : ScannerInput
}

sealed interface ScanResult {
    data object Collecting : ScanResult
    data object Ignored : ScanResult
    data object Cancelled : ScanResult
    data class Barcode(val value: String) : ScanResult
}
