package com.inforhard.pos

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inforhard.pos.core.hardware.AndroidHidKeyAdapter
import com.inforhard.pos.core.hardware.HidBarcodeScanner
import com.inforhard.pos.core.hardware.ScanResult

class MainActivity : ComponentActivity() {
    private val shellController = PosShellController()
    private val scannerAdapter = AndroidHidKeyAdapter(HidBarcodeScanner())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PosShell(shellController.state) }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val result = scannerAdapter.onKeyEvent(event)
        shellController.onScanResult(result)
        return if (result == ScanResult.Ignored) super.onKeyDown(keyCode, event) else true
    }
}

@Composable
private fun PosShell(state: PosShellState) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Inforhard POS", style = MaterialTheme.typography.headlineLarge)
                Text("Modo laboratorio local — servicios reales deshabilitados")
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Scanner HID", style = MaterialTheme.typography.titleLarge)
                        Text(state.scannerMessage)
                        state.lastBarcode?.let { barcode ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Última captura local", style = MaterialTheme.typography.labelLarge)
                            Text(barcode, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}
