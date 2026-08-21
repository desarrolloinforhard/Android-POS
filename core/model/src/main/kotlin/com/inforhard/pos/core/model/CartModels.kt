package com.inforhard.pos.core.model

data class Money(
    val minorUnits: Long,
    val currencyCode: String,
) {
    init {
        require(minorUnits >= 0)
        require(currencyCode.matches(Regex("[A-Z]{3}")))
    }

    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode)
        return copy(minorUnits = Math.addExact(minorUnits, other.minorUnits))
    }
}

data class Cart(
    val cartId: String,
    val revision: Long = 0,
    val items: List<CartItem> = emptyList(),
    val pricingVersion: String? = null,
) {
    init {
        require(cartId.isNotBlank())
        require(revision >= 0)
        require(items.map(CartItem::productId).distinct().size == items.size) {
            "Cart must contain at most one line per product"
        }
    }
}

data class CartItem(
    val productId: String,
    val description: String,
    val quantity: Int,
    val quote: CartItemQuote? = null,
) {
    init {
        require(productId.isNotBlank())
        require(description.isNotBlank())
        require(quantity > 0)
    }
}

data class CartItemQuote(
    val unitPrice: Money,
    val lineTotal: Money,
    val appliedRules: List<AppliedPriceRule>,
)

data class PriceQuote(
    val pricingVersion: String,
    val currencyCode: String,
    val items: List<QuotedItem>,
    val total: Money,
) {
    init {
        require(pricingVersion.isNotBlank())
        require(total.currencyCode == currencyCode)
    }
}

data class QuotedItem(
    val productId: String,
    val quantity: Int,
    val unitPrice: Money,
    val lineTotal: Money,
    val appliedRules: List<AppliedPriceRule>,
)

data class AppliedPriceRule(
    val ruleId: String,
    val kind: AppliedPriceRuleKind,
    val affectedUnits: Int,
    val explanation: String,
)

enum class AppliedPriceRuleKind {
    BASE,
    THRESHOLD,
    GROUP_PROMOTION,
}

sealed interface CartEvaluation {
    data class Quoted(val cart: Cart, val quote: PriceQuote) : CartEvaluation
    data class PricingUnavailable(val cart: Cart) : CartEvaluation
    data class AmbiguousPricing(val cart: Cart, val productIds: Set<String>) : CartEvaluation
}
