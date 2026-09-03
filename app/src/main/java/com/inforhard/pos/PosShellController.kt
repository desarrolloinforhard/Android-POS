package com.inforhard.pos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.inforhard.pos.core.domain.*
import com.inforhard.pos.core.hardware.ScanResult
import com.inforhard.pos.core.model.*

enum class PosDestination { WELCOME, CART, ASSISTANCE, CANCEL_CONFIRMATION }

data class PosShellState(
    val destination: PosDestination = PosDestination.WELCOME,
    val cart: Cart = Cart(cartId = "local-draft"),
    val total: Money = Money(0, "ARS"),
    val connectivity: ConnectivityState = ConnectivityState.OFFLINE,
    val scannerMessage: String = "Iniciá para escanear productos",
    val pricingMessage: String? = null,
    val inactivityRevision: Long = 0,
)

class PosShellController(
    private val catalog: CatalogRepository = defaultCatalog(),
    private val pricingContext: PricingContext = defaultPricing(),
    private val cartService: CartService = CartService(FixturePricingEngine()),
) {
    var state by mutableStateOf(PosShellState())
        private set

    fun start() { state = state.copy(destination = PosDestination.CART, scannerMessage = "Escaneá un producto").withActivity() }

    fun onScanResult(result: ScanResult) {
        if (state.destination != PosDestination.CART) return
        state = state.withActivity()
        when (result) {
            ScanResult.Collecting -> state = state.copy(scannerMessage = "Leyendo…")
            ScanResult.Ignored -> Unit
            ScanResult.Cancelled -> state = state.copy(scannerMessage = "Lectura descartada")
            is ScanResult.Barcode -> onBarcode(result.value)
        }
    }

    fun increment(productId: String) {
        state = state.withActivity()
        val item = state.cart.items.firstOrNull { it.productId == productId } ?: return
        applyEvaluation(cartService.changeQuantity(state.cart, productId, item.quantity + 1, pricingContext))
    }

    fun decrement(productId: String) {
        state = state.withActivity()
        val item = state.cart.items.firstOrNull { it.productId == productId } ?: return
        if (item.quantity == 1) remove(productId)
        else applyEvaluation(cartService.changeQuantity(state.cart, productId, item.quantity - 1, pricingContext))
    }

    fun remove(productId: String) {
        state = state.withActivity()
        val item = state.cart.items.firstOrNull { it.productId == productId } ?: return
        applyEvaluation(cartService.remove(state.cart, productId, pricingContext))
        state = state.copy(scannerMessage = if (state.cart.items.isEmpty()) {
            "Carrito vacío"
        } else {
            "${item.description} eliminado"
        })
    }

    fun requestAssistance() { state = state.copy(destination = PosDestination.ASSISTANCE).withActivity() }
    fun dismissAssistance() { state = state.copy(destination = PosDestination.CART).withActivity() }
    fun requestCancellation() { state = state.copy(destination = PosDestination.CANCEL_CONFIRMATION).withActivity() }
    fun keepShopping() { state = state.copy(destination = PosDestination.CART).withActivity() }
    fun confirmCancellation() { state = PosShellState(scannerMessage = "Operación local cancelada") }

    fun onInactivityTimeout(expectedRevision: Long) {
        if (state.destination == PosDestination.WELCOME || state.inactivityRevision != expectedRevision) return
        state = PosShellState(scannerMessage = "Sesión local cerrada por inactividad")
    }

    private fun PosShellState.withActivity() = copy(inactivityRevision = inactivityRevision + 1)

    private fun onBarcode(barcode: String) {
        when (val lookup = catalog.lookup(barcode)) {
            is BarcodeLookup.Found -> {
                applyEvaluation(cartService.scan(state.cart, lookup, pricingContext))
                state = state.copy(scannerMessage = "${lookup.product.description} agregado")
            }
            BarcodeLookup.NotFound -> state = state.copy(scannerMessage = "Producto no encontrado en el catálogo local")
            is BarcodeLookup.Ambiguous -> state = state.copy(scannerMessage = "Código ambiguo; solicitá asistencia")
            BarcodeLookup.CatalogUnavailable -> state = state.copy(scannerMessage = "Catálogo local no disponible")
        }
    }

    private fun applyEvaluation(evaluation: CartEvaluation) {
        state = when (evaluation) {
            is CartEvaluation.Quoted -> state.copy(cart = evaluation.cart, total = evaluation.quote.total, pricingMessage = null)
            is CartEvaluation.PricingUnavailable -> state.copy(cart = evaluation.cart, pricingMessage = "Precio local no disponible")
            is CartEvaluation.AmbiguousPricing -> state.copy(cart = evaluation.cart, pricingMessage = "Reglas sintéticas ambiguas; solicitá asistencia")
        }
    }

    companion object {
        private const val CATALOG_VERSION = "fixture-catalog-v1"

        private fun defaultCatalog() = FixtureCatalogRepository(
            CATALOG_VERSION,
            listOf(
                ProductSummary("fixture-water", setOf("7790000000011"), "Agua sin gas", "unidad", catalogVersion = CATALOG_VERSION),
                ProductSummary("fixture-cereal", setOf("7790000000028"), "Cereal", "unidad", catalogVersion = CATALOG_VERSION),
                ProductSummary("fixture-soap", setOf("7790000000035"), "Jabón", "unidad", catalogVersion = CATALOG_VERSION),
            ),
        )

        private fun defaultPricing() = PricingContext(
            "fixture-pricing-v1",
            "ARS",
            listOf(
                PricingFixtureRule.BasePrice("base-water", "fixture-water", 150_00),
                PricingFixtureRule.ThresholdPrice("threshold-water-3", "fixture-water", 3, 125_00),
                PricingFixtureRule.BasePrice("base-cereal", "fixture-cereal", 320_00),
                PricingFixtureRule.GroupPromotion("group-cereal-2", "fixture-cereal", 2, 550_00),
                PricingFixtureRule.BasePrice("base-soap", "fixture-soap", 210_00),
            ),
        )
    }
}
