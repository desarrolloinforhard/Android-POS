package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.BarcodeLookup
import com.inforhard.pos.core.model.Cart
import com.inforhard.pos.core.model.CartEvaluation
import com.inforhard.pos.core.model.PriceQuote
import com.inforhard.pos.core.model.ProductSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PricingDeterminismMatrixTest {
    private val productA = product("product-a", "barcode-a")
    private val productB = product("product-b", "barcode-b")
    private val service = CartService(FixturePricingEngine())

    @Test
    fun scanSequenceAndDirectQuantityProduceSameQuote() {
        val context = context(
            PricingFixtureRule.BasePrice("base-a", productA.productId, 100),
            PricingFixtureRule.ThresholdPrice("threshold-a-3", productA.productId, 3, 80),
        )
        val once = scan(Cart("sequence"), productA, context)
        val byScans = scan(scan(once, productA, context), productA, context)
        val byQuantity = service.changeQuantity(once, productA.productId, 3, context).quotedCart()

        assertEquals(byScans.items, byQuantity.items)
        assertEquals(byScans.pricingVersion, byQuantity.pricingVersion)
    }

    @Test
    fun productCaptureOrderDoesNotChangeQuote() {
        val context = twoProductContext()
        val aThenB = scan(scan(Cart("order-a"), productA, context), productB, context)
        val bThenA = scan(scan(Cart("order-b"), productB, context), productA, context)

        assertEquals(aThenB.items, bThenA.items)
        assertEquals(aThenB.total(context), bThenA.total(context))
    }

    @Test
    fun thresholdBoundaryAppliesOnlyWhenReached() {
        val context = context(
            PricingFixtureRule.BasePrice("base-a", productA.productId, 100),
            PricingFixtureRule.ThresholdPrice("threshold-a-3", productA.productId, 3, 80),
        )
        val once = scan(Cart("threshold-boundary"), productA, context)
        val below = service.changeQuantity(once, productA.productId, 2, context).quote()
        val reached = service.changeQuantity(once, productA.productId, 3, context).quote()

        assertEquals(200, below.total.minorUnits)
        assertEquals(240, reached.total.minorUnits)
    }

    @Test
    fun groupPromotionKeepsRemainderAtBasePrice() {
        val context = context(
            PricingFixtureRule.BasePrice("base-a", productA.productId, 100),
            PricingFixtureRule.GroupPromotion("group-a-2", productA.productId, 2, 150),
        )
        val once = scan(Cart("group-boundary"), productA, context)

        val totals = (1..5).map { quantity ->
            service.changeQuantity(once, productA.productId, quantity, context).quote().total.minorUnits
        }

        assertEquals(listOf(100L, 150L, 250L, 300L, 400L), totals)
    }

    @Test
    fun removingPromotionRepricesWholeCartAtBasePrice() {
        val promotedContext = context(
            PricingFixtureRule.BasePrice("base-a", productA.productId, 100),
            PricingFixtureRule.GroupPromotion("group-a-2", productA.productId, 2, 150),
        )
        val baseOnlyContext = context(
            PricingFixtureRule.BasePrice("base-a", productA.productId, 100),
            version = "pricing-without-promotion",
        )
        val once = scan(Cart("promotion-removal"), productA, promotedContext)
        val promoted = service.changeQuantity(once, productA.productId, 3, promotedContext).quotedCart()
        val repriced = service.reprice(promoted, baseOnlyContext).quote()

        assertEquals(300, repriced.total.minorUnits)
        assertEquals("pricing-without-promotion", repriced.pricingVersion)
    }

    @Test
    fun ambiguousRulePrecedenceNeverDependsOnRuleOrder() {
        val base = PricingFixtureRule.BasePrice("base-a", productA.productId, 100)
        val threshold = PricingFixtureRule.ThresholdPrice("threshold-a-2", productA.productId, 2, 90)
        val group = PricingFixtureRule.GroupPromotion("group-a-2", productA.productId, 2, 150)
        val once = scan(Cart("ambiguous-order"), productA, context(base))
        val firstOrder = service.changeQuantity(once, productA.productId, 2, context(base, threshold, group))
        val secondOrder = service.changeQuantity(once, productA.productId, 2, context(group, base, threshold))

        assertTrue(firstOrder is CartEvaluation.AmbiguousPricing)
        assertEquals(firstOrder, secondOrder)
    }

    private fun product(id: String, barcode: String) = ProductSummary(
        productId = id,
        barcodes = setOf(barcode),
        description = "Synthetic $id",
        unitLabel = "unit",
        catalogVersion = "catalog-v1",
    )

    private fun twoProductContext() = context(
        PricingFixtureRule.BasePrice("base-a", productA.productId, 100),
        PricingFixtureRule.BasePrice("base-b", productB.productId, 250),
    )

    private fun context(
        vararg rules: PricingFixtureRule,
        version: String = "pricing-v1",
    ) = PricingContext(version, "ARS", rules.toList())

    private fun scan(cart: Cart, product: ProductSummary, context: PricingContext): Cart =
        service.scan(cart, BarcodeLookup.Found(product), context).quotedCart()

    private fun Cart.total(context: PricingContext): Long = service.reprice(this, context).quote().total.minorUnits
    private fun CartEvaluation.quote(): PriceQuote = (this as CartEvaluation.Quoted).quote
    private fun CartEvaluation.quotedCart(): Cart = (this as CartEvaluation.Quoted).cart
}
