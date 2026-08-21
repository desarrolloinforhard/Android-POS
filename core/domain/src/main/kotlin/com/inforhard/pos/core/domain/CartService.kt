package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.BarcodeLookup
import com.inforhard.pos.core.model.Cart
import com.inforhard.pos.core.model.CartEvaluation
import com.inforhard.pos.core.model.CartItem
import com.inforhard.pos.core.model.CartItemQuote

class CartService(
    private val pricingEngine: PricingEngine,
) {
    fun scan(
        cart: Cart,
        lookup: BarcodeLookup.Found,
        pricingContext: PricingContext,
    ): CartEvaluation {
        val product = lookup.product
        val existing = cart.items.firstOrNull { it.productId == product.productId }
        val updatedItems = if (existing == null) {
            cart.items + CartItem(
                productId = product.productId,
                description = product.description,
                quantity = 1,
            )
        } else {
            cart.items.map { item ->
                if (item.productId == product.productId) {
                    item.copy(quantity = Math.addExact(item.quantity, 1), quote = null)
                } else {
                    item.copy(quote = null)
                }
            }
        }
        return evaluate(cart.copy(revision = cart.revision + 1, items = updatedItems), pricingContext)
    }

    fun changeQuantity(
        cart: Cart,
        productId: String,
        quantity: Int,
        pricingContext: PricingContext,
    ): CartEvaluation {
        require(quantity > 0)
        require(cart.items.any { it.productId == productId })
        val updated = cart.copy(
            revision = cart.revision + 1,
            items = cart.items.map { item ->
                if (item.productId == productId) item.copy(quantity = quantity, quote = null)
                else item.copy(quote = null)
            },
        )
        return evaluate(updated, pricingContext)
    }

    fun remove(
        cart: Cart,
        productId: String,
        pricingContext: PricingContext,
    ): CartEvaluation = evaluate(
        cart.copy(
            revision = cart.revision + 1,
            items = cart.items.filterNot { it.productId == productId }.map { it.copy(quote = null) },
        ),
        pricingContext,
    )

    fun reprice(cart: Cart, pricingContext: PricingContext): CartEvaluation =
        evaluate(cart.copy(revision = cart.revision + 1, items = cart.items.map { it.copy(quote = null) }), pricingContext)

    private fun evaluate(cart: Cart, pricingContext: PricingContext): CartEvaluation =
        when (val result = pricingEngine.quote(cart, pricingContext)) {
            PricingResult.Unavailable -> CartEvaluation.PricingUnavailable(cart)
            is PricingResult.Ambiguous -> CartEvaluation.AmbiguousPricing(cart, result.productIds)
            is PricingResult.Success -> {
                val quotes = result.quote.items.associateBy { it.productId }
                val quotedCart = cart.copy(
                    pricingVersion = result.quote.pricingVersion,
                    items = cart.items.sortedBy { it.productId }.map { item ->
                        val quote = requireNotNull(quotes[item.productId])
                        item.copy(
                            quote = CartItemQuote(
                                unitPrice = quote.unitPrice,
                                lineTotal = quote.lineTotal,
                                appliedRules = quote.appliedRules,
                            ),
                        )
                    },
                )
                CartEvaluation.Quoted(quotedCart, result.quote)
            }
        }
}
