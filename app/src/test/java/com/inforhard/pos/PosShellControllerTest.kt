package com.inforhard.pos

import com.inforhard.pos.core.hardware.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosShellControllerTest {
    @Test fun removingLastItemUpdatesMessageAndRescanStartsAtOne() {
        val controller = PosShellController().also { it.start() }
        repeat(2) { controller.onScanResult(ScanResult.Barcode("7790000000011")) }
        controller.remove("fixture-water")
        assertTrue(controller.state.cart.items.isEmpty())
        assertEquals(0, controller.state.total.minorUnits)
        assertEquals("Carrito vacío", controller.state.scannerMessage)
        controller.onScanResult(ScanResult.Barcode("7790000000011"))
        assertEquals(1, controller.state.cart.items.single().quantity)
        assertEquals(150_00, controller.state.total.minorUnits)
        assertEquals("Agua sin gas agregado", controller.state.scannerMessage)
    }

    @Test fun decrementingLastUnitUpdatesEmptyCartMessage() {
        val controller = PosShellController().also { it.start() }
        controller.onScanResult(ScanResult.Barcode("7790000000011"))
        controller.decrement("fixture-water")
        assertTrue(controller.state.cart.items.isEmpty())
        assertEquals(0, controller.state.total.minorUnits)
        assertEquals("Carrito vacío", controller.state.scannerMessage)
    }

    @Test fun removingOneOfSeveralProductsNamesRemovedItem() {
        val controller = PosShellController().also { it.start() }
        controller.onScanResult(ScanResult.Barcode("7790000000011"))
        controller.onScanResult(ScanResult.Barcode("7790000000035"))
        controller.remove("fixture-water")
        assertEquals("fixture-soap", controller.state.cart.items.single().productId)
        assertEquals(210_00, controller.state.total.minorUnits)
        assertEquals("Agua sin gas eliminado", controller.state.scannerMessage)
        controller.remove("missing")
        assertEquals("Agua sin gas eliminado", controller.state.scannerMessage)
    }

    @Test fun repeatedScansAccumulateAndRepriceWholeCart() {
        val controller = PosShellController().also { it.start() }
        repeat(3) { controller.onScanResult(ScanResult.Barcode("7790000000011")) }
        assertEquals(1, controller.state.cart.items.size)
        assertEquals(3, controller.state.cart.items.single().quantity)
        assertEquals(375_00, controller.state.total.minorUnits)
    }

    @Test fun unknownBarcodeDoesNotChangeCart() {
        val controller = PosShellController().also { it.start() }
        controller.onScanResult(ScanResult.Barcode("unknown"))
        assertTrue(controller.state.cart.items.isEmpty())
        assertEquals("Producto no encontrado en el catálogo local", controller.state.scannerMessage)
    }

    @Test fun assistanceReturnsToSameCart() {
        val controller = PosShellController().also { it.start() }
        controller.onScanResult(ScanResult.Barcode("7790000000028"))
        controller.requestAssistance()
        assertEquals(PosDestination.ASSISTANCE, controller.state.destination)
        controller.dismissAssistance()
        assertEquals(PosDestination.CART, controller.state.destination)
        assertEquals(1, controller.state.cart.items.size)
    }

    @Test fun confirmedCancellationClearsDraftAndReturnsHome() {
        val controller = PosShellController().also { it.start() }
        controller.onScanResult(ScanResult.Barcode("7790000000035"))
        controller.requestCancellation()
        controller.confirmCancellation()
        assertEquals(PosDestination.WELCOME, controller.state.destination)
        assertTrue(controller.state.cart.items.isEmpty())
        assertEquals(0, controller.state.total.minorUnits)
    }

    @Test fun inactivityClosesOnlyMatchingActiveRevision() {
        val controller = PosShellController().also { it.start() }
        controller.onScanResult(ScanResult.Barcode("7790000000035"))
        val staleRevision = controller.state.inactivityRevision
        controller.increment("fixture-soap")

        controller.onInactivityTimeout(staleRevision)
        assertEquals(PosDestination.CART, controller.state.destination)

        controller.onInactivityTimeout(controller.state.inactivityRevision)
        assertEquals(PosDestination.WELCOME, controller.state.destination)
        assertTrue(controller.state.cart.items.isEmpty())
        assertEquals("Sesión local cerrada por inactividad", controller.state.scannerMessage)
    }
}
