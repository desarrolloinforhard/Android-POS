package com.inforhard.pos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.inforhard.pos.core.hardware.ScanResult

data class PosShellState(
    val lastBarcode: String? = null,
    val scannerMessage: String = "Esperando lectura local",
)

class PosShellController {
    var state by mutableStateOf(PosShellState())
        private set

    fun onScanResult(result: ScanResult) {
        state = when (result) {
            ScanResult.Collecting -> state.copy(scannerMessage = "Leyendo…")
            ScanResult.Ignored -> state
            ScanResult.Cancelled -> state.copy(scannerMessage = "Lectura descartada")
            is ScanResult.Barcode -> PosShellState(
                lastBarcode = result.value,
                scannerMessage = "Captura completa; sin consulta comercial",
            )
        }
    }
}
