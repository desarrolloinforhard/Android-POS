package com.inforhard.pos

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.inforhard.pos.core.hardware.*
import com.inforhard.pos.core.model.CartItem
import com.inforhard.pos.core.model.Money

class MainActivity : ComponentActivity() {
    private val shellController = PosShellController()
    private val scannerAdapter = AndroidHidKeyAdapter(HidBarcodeScanner())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PosApp(shellController.state, shellController) }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val result = scannerAdapter.onKeyEvent(event)
        shellController.onScanResult(result)
        return if (result == ScanResult.Ignored) super.onKeyDown(keyCode, event) else true
    }
}

@Composable
internal fun PosApp(state: PosShellState, controller: PosShellController) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            when (state.destination) {
                PosDestination.WELCOME -> WelcomeScreen(state, controller::start)
                PosDestination.CART -> CartScreen(state, controller)
                PosDestination.ASSISTANCE -> AssistanceScreen(controller::dismissAssistance)
                PosDestination.CANCEL_CONFIRMATION -> CancellationScreen(controller::keepShopping, controller::confirmCancellation)
            }
        }
    }
}

@Composable
private fun WelcomeScreen(state: PosShellState, onStart: () -> Unit) = CenteredScreen {
    Text("Inforhard POS", style = MaterialTheme.typography.displaySmall)
    Text("Autoservicio local de prueba")
    Text("Servicios comerciales deshabilitados")
    Spacer(Modifier.height(28.dp))
    Button(onStart, Modifier.semantics { contentDescription = "Iniciar compra local" }) { Text("Comenzar") }
    Spacer(Modifier.height(20.dp))
    Text(state.scannerMessage, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun CartScreen(state: PosShellState, controller: PosShellController) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Tu carrito", style = MaterialTheme.typography.headlineLarge)
        Text("Catálogo y precios sintéticos — no genera una venta")
        Text("Estado: ${state.connectivity.name.lowercase()} · operación sólo local")
        Spacer(Modifier.height(16.dp))
        if (state.cart.items.isEmpty()) Text("Escaneá un producto para agregarlo")
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.cart.items, key = CartItem::productId) { CartItemCard(it, controller) }
        }
        state.pricingMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text(state.scannerMessage)
        Text("Total local: ${state.total.display()}", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(controller::requestAssistance) { Text("Solicitar asistencia") }
            OutlinedButton(controller::requestCancellation) { Text("Cancelar") }
        }
    }
}

@Composable
private fun CartItemCard(item: CartItem, controller: PosShellController) = Card(Modifier.fillMaxWidth()) {
    Column(Modifier.padding(16.dp)) {
        Text(item.description, style = MaterialTheme.typography.titleLarge)
        Text("Cantidad: ${item.quantity}")
        Text("Subtotal: ${item.quote?.lineTotal?.display() ?: "pendiente"}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton({ controller.decrement(item.productId) }) { Text("−") }
            OutlinedButton({ controller.increment(item.productId) }) { Text("+") }
            OutlinedButton({ controller.remove(item.productId) }) { Text("Eliminar") }
        }
    }
}

@Composable
private fun AssistanceScreen(onBack: () -> Unit) = CenteredScreen {
    Text("Asistencia solicitada", style = MaterialTheme.typography.headlineLarge)
    Text("Este aviso es únicamente local; no se contactó ningún servicio.")
    Spacer(Modifier.height(24.dp))
    Button(onBack) { Text("Volver al carrito") }
}

@Composable
private fun CancellationScreen(onKeep: () -> Unit, onConfirm: () -> Unit) = CenteredScreen {
    Text("¿Cancelar esta operación?", style = MaterialTheme.typography.headlineLarge)
    Text("Se vaciará únicamente este carrito local.")
    Spacer(Modifier.height(24.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onKeep) { Text("Seguir comprando") }
        Button(onConfirm) { Text("Sí, cancelar") }
    }
}

@Composable
private fun CenteredScreen(content: @Composable ColumnScope.() -> Unit) = Column(
    Modifier.fillMaxSize().padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    content = content,
)

private fun Money.display() = "$currencyCode ${minorUnits / 100},${(minorUnits % 100).toString().padStart(2, '0')}"
