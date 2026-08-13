package com.inforhard.pos.core.hardware

class HidBarcodeScanner(
    private val maximumLength: Int = DEFAULT_MAXIMUM_LENGTH,
) : BarcodeScanner {
    private val buffer = StringBuilder()

    init {
        require(maximumLength > 0)
    }

    @Synchronized
    override fun accept(input: ScannerInput): ScanResult = when (input) {
        is ScannerInput.Character -> collect(input.value)
        ScannerInput.Submit -> submit()
        ScannerInput.Cancel -> cancel()
    }

    private fun collect(character: Char): ScanResult {
        if (character.isISOControl()) return ScanResult.Ignored
        if (buffer.length >= maximumLength) {
            buffer.clear()
            return ScanResult.Cancelled
        }
        buffer.append(character)
        return ScanResult.Collecting
    }

    private fun submit(): ScanResult {
        if (buffer.isEmpty()) return ScanResult.Ignored
        val barcode = buffer.toString()
        buffer.clear()
        return ScanResult.Barcode(barcode)
    }

    private fun cancel(): ScanResult {
        buffer.clear()
        return ScanResult.Cancelled
    }

    private companion object {
        const val DEFAULT_MAXIMUM_LENGTH = 128
    }
}
