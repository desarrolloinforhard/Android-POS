package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.BarcodeLookup
import com.inforhard.pos.core.model.ProductSummary

interface CatalogRepository {
    val catalogVersion: String
    fun lookup(barcode: String): BarcodeLookup
}

class FixtureCatalogRepository(
    override val catalogVersion: String,
    products: List<ProductSummary>,
) : CatalogRepository {
    private val productsByBarcode = products
        .flatMap { product -> product.barcodes.map { barcode -> barcode to product } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })

    init {
        require(catalogVersion.isNotBlank())
        require(products.all { it.catalogVersion == catalogVersion })
    }

    override fun lookup(barcode: String): BarcodeLookup {
        val matches = productsByBarcode[barcode].orEmpty()
        return when (matches.size) {
            0 -> BarcodeLookup.NotFound
            1 -> BarcodeLookup.Found(matches.single())
            else -> BarcodeLookup.Ambiguous(matches.map(ProductSummary::productId).toSet())
        }
    }
}
