package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.BarcodeLookup
import com.inforhard.pos.core.model.Cart
import com.inforhard.pos.core.model.CartEvaluation
import com.inforhard.pos.core.model.ProductSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartServiceTest {
    private val product = ProductSummary(
        productId = "synthetic-product-a",
        barcodes = setOf("7790000000001"),
        description = "Synthetic product A",
        unitLabel = "unit",
        catalogVersion = "catalog-v1",
    )
    private val lookup = BarcodeLookup.Found(product)
    private val service = CartService(FixturePricingEngine())

    @Test
    fun repeatedScansEqualDirectQuantityChange() {
        val context = baseContext()
        val empty = Cart(cartId = "cart-a")
        val once = service.scan(empty, lookup, context).quotedCart()
        val scannedTwice = service.scan(once, lookup, context).quotedCart()
        val quantityTwo = service.changeQuantity(once, product.productId, 2, context).quotedCart()

        assertEquals(2, scannedTwice.items.single().quantity)
        assertEquals(scannedTwice.items.single().quote, quantityTwo.items.single().quote)
    }

    @Test
    fun thresholdUsesTotalEligibleQuantityForEveryUnit() {
        val context = baseContext(
            PricingFixtureRule.ThresholdPrice("threshold-3", product.productId, 3, 80),
        )
        val first = service.scan(Cart("cart-threshold"), lookup, context).quotedCart()
        val quoted = service.changeQuantity(first, product.productId, 3, context) as CartEvaluation.Quoted

        assertEquals(240, quoted.quote.total.minorUnits)
    }

    @Test
    fun groupPromotionPricesGroupsAndRemainderSeparately() {
        val context = baseContext(
            PricingFixtureRule.GroupPromotion("group-2", product.productId, 2, 150),
        )
        val first = service.scan(Cart("cart-groups"), lookup, context).quotedCart()
        val quoted = service.changeQuantity(first, product.productId, 5, context) as CartEvaluation.Quoted

        assertEquals(400, quoted.quote.total.minorUnits)
    }

    @Test
    fun ambiguousThresholdAndGroupAreNotGivenInventedPrecedence() {
        val context = baseContext(
            PricingFixtureRule.ThresholdPrice("threshold-2", product.productId, 2, 90),
            PricingFixtureRule.GroupPromotion("group-2", product.productId, 2, 150),
        )
        val first = service.scan(Cart("cart-ambiguous"), lookup, context).quotedCart()
        val result = service.changeQuantity(first, product.productId, 2, context)

        assertTrue(result is CartEvaluation.AmbiguousPricing)
    }

    @Test
    fun newPricingVersionRecalculatesWholeCart() {
        val v1 = baseContext()
        val initial = service.scan(Cart("cart-version"), lookup, v1).quotedCart()
        val v2 = PricingContext(
            pricingVersion = "pricing-v2",
            currencyCode = "ARS",
            rules = listOf(PricingFixtureRule.BasePrice("base-v2", product.productId, 120)),
        )
        val repriced = service.reprice(initial, v2) as CartEvaluation.Quoted

        assertEquals("pricing-v2", repriced.cart.pricingVersion)
        assertEquals(120, repriced.quote.total.minorUnits)
    }

    @Test
    fun removalRecalculatesRemainingCart() {
        val context = baseContext()
        val initial = service.scan(Cart("cart-remove"), lookup, context).quotedCart()
        val removed = service.remove(initial, product.productId, context) as CartEvaluation.Quoted

        assertTrue(removed.cart.items.isEmpty())
        assertEquals(0, removed.quote.total.minorUnits)
    }

    private fun baseContext(vararg extra: PricingFixtureRule): PricingContext = PricingContext(
        pricingVersion = "pricing-v1",
        currencyCode = "ARS",
        rules = listOf(PricingFixtureRule.BasePrice("base-a", product.productId, 100)) + extra,
    )

    private fun CartEvaluation.quotedCart(): Cart = (this as CartEvaluation.Quoted).cart
}
