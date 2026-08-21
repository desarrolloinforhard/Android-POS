package com.inforhard.pos.core.model

data class ProductSummary(
    val productId: String,
    val barcodes: Set<String>,
    val description: String,
    val unitLabel: String,
    val imageReference: String? = null,
    val catalogVersion: String,
) {
    init {
        require(productId.isNotBlank())
        require(barcodes.isNotEmpty())
        require(barcodes.all(String::isNotBlank))
        require(description.isNotBlank())
        require(unitLabel.isNotBlank())
        require(catalogVersion.isNotBlank())
    }
}

sealed interface BarcodeLookup {
    data class Found(val product: ProductSummary) : BarcodeLookup
    data object NotFound : BarcodeLookup
    data class Ambiguous(val productIds: Set<String>) : BarcodeLookup
    data object CatalogUnavailable : BarcodeLookup
}

enum class ConnectivityState {
    ONLINE,
    DEGRADED,
    OFFLINE,
    RECONNECTING,
}

data class AppConfiguration(
    val configurationVersion: String,
    val selfServiceEnabled: Boolean,
    val cashierModeVisible: Boolean,
    val assistanceEnabled: Boolean,
) {
    init {
        require(configurationVersion.isNotBlank())
    }
}
