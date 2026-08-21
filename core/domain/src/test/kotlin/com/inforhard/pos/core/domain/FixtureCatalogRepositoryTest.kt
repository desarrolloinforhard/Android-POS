package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.BarcodeLookup
import com.inforhard.pos.core.model.ProductSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixtureCatalogRepositoryTest {
    @Test
    fun returnsSyntheticProductByBarcode() {
        val product = product("a", "7790000000001")
        val repository = FixtureCatalogRepository("catalog-v1", listOf(product))

        assertEquals(BarcodeLookup.Found(product), repository.lookup("7790000000001"))
        assertEquals(BarcodeLookup.NotFound, repository.lookup("missing"))
    }

    @Test
    fun duplicatedBarcodeIsExplicitlyAmbiguous() {
        val repository = FixtureCatalogRepository(
            "catalog-v1",
            listOf(product("a", "shared"), product("b", "shared")),
        )

        assertTrue(repository.lookup("shared") is BarcodeLookup.Ambiguous)
    }

    private fun product(id: String, barcode: String) = ProductSummary(
        productId = id,
        barcodes = setOf(barcode),
        description = "Synthetic $id",
        unitLabel = "unit",
        catalogVersion = "catalog-v1",
    )
}
