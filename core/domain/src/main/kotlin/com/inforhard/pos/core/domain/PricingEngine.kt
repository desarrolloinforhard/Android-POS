package com.inforhard.pos.core.domain

import com.inforhard.pos.core.model.AppliedPriceRule
import com.inforhard.pos.core.model.AppliedPriceRuleKind
import com.inforhard.pos.core.model.Cart
import com.inforhard.pos.core.model.Money
import com.inforhard.pos.core.model.PriceQuote
import com.inforhard.pos.core.model.QuotedItem

sealed interface PricingResult {
    data class Success(val quote: PriceQuote) : PricingResult
    data class Ambiguous(val productIds: Set<String>) : PricingResult
    data object Unavailable : PricingResult
}

fun interface PricingEngine {
    fun quote(cart: Cart, context: PricingContext): PricingResult
}

data class PricingContext(
    val pricingVersion: String,
    val currencyCode: String,
    val rules: List<PricingFixtureRule>,
)

sealed interface PricingFixtureRule {
    val ruleId: String
    val productId: String

    data class BasePrice(
        override val ruleId: String,
        override val productId: String,
        val unitPriceMinor: Long,
    ) : PricingFixtureRule

    data class ThresholdPrice(
        override val ruleId: String,
        override val productId: String,
        val minimumQuantity: Int,
        val unitPriceMinor: Long,
    ) : PricingFixtureRule

    data class GroupPromotion(
        override val ruleId: String,
        override val productId: String,
        val groupSize: Int,
        val groupPriceMinor: Long,
    ) : PricingFixtureRule
}

class FixturePricingEngine : PricingEngine {
    override fun quote(cart: Cart, context: PricingContext): PricingResult {
        if (context.pricingVersion.isBlank()) return PricingResult.Unavailable
        require(context.currencyCode.matches(Regex("[A-Z]{3}")))
        context.rules.forEach { rule ->
            require(rule.ruleId.isNotBlank() && rule.productId.isNotBlank())
            when (rule) {
                is PricingFixtureRule.BasePrice -> require(rule.unitPriceMinor >= 0)
                is PricingFixtureRule.ThresholdPrice -> {
                    require(rule.minimumQuantity > 0)
                    require(rule.unitPriceMinor >= 0)
                }
                is PricingFixtureRule.GroupPromotion -> {
                    require(rule.groupSize > 0)
                    require(rule.groupPriceMinor >= 0)
                }
            }
        }
        val quotedItems = mutableListOf<QuotedItem>()
        val ambiguous = mutableSetOf<String>()

        cart.items.sortedBy { it.productId }.forEach { item ->
            val productRules = context.rules.filter { it.productId == item.productId }
            val base = productRules.filterIsInstance<PricingFixtureRule.BasePrice>().singleOrNull()
                ?: return PricingResult.Unavailable
            val threshold = productRules.filterIsInstance<PricingFixtureRule.ThresholdPrice>()
                .filter { item.quantity >= it.minimumQuantity }
                .maxByOrNull { it.minimumQuantity }
            val group = productRules.filterIsInstance<PricingFixtureRule.GroupPromotion>()
                .singleOrNull()
                ?.takeIf { item.quantity >= it.groupSize }

            if (threshold != null && group != null) {
                ambiguous += item.productId
                return@forEach
            }

            quotedItems += when {
                threshold != null -> thresholdQuote(item.productId, item.quantity, base, threshold, context)
                group != null -> groupQuote(item.productId, item.quantity, base, group, context)
                else -> baseQuote(item.productId, item.quantity, base, context)
            }
        }

        if (ambiguous.isNotEmpty()) return PricingResult.Ambiguous(ambiguous)
        val zero = Money(0, context.currencyCode)
        return PricingResult.Success(
            PriceQuote(
                pricingVersion = context.pricingVersion,
                currencyCode = context.currencyCode,
                items = quotedItems,
                total = quotedItems.fold(zero) { total, item -> total + item.lineTotal },
            ),
        )
    }

    private fun baseQuote(
        productId: String,
        quantity: Int,
        base: PricingFixtureRule.BasePrice,
        context: PricingContext,
    ): QuotedItem {
        val unit = Money(base.unitPriceMinor, context.currencyCode)
        return QuotedItem(
            productId = productId,
            quantity = quantity,
            unitPrice = unit,
            lineTotal = Money(Math.multiplyExact(base.unitPriceMinor, quantity.toLong()), context.currencyCode),
            appliedRules = listOf(rule(base.ruleId, AppliedPriceRuleKind.BASE, quantity, "Synthetic base price")),
        )
    }

    private fun thresholdQuote(
        productId: String,
        quantity: Int,
        base: PricingFixtureRule.BasePrice,
        threshold: PricingFixtureRule.ThresholdPrice,
        context: PricingContext,
    ): QuotedItem {
        val unit = Money(threshold.unitPriceMinor, context.currencyCode)
        return QuotedItem(
            productId = productId,
            quantity = quantity,
            unitPrice = unit,
            lineTotal = Money(Math.multiplyExact(threshold.unitPriceMinor, quantity.toLong()), context.currencyCode),
            appliedRules = listOf(
                rule(base.ruleId, AppliedPriceRuleKind.BASE, 0, "Synthetic base reference"),
                rule(threshold.ruleId, AppliedPriceRuleKind.THRESHOLD, quantity, "Synthetic threshold price"),
            ),
        )
    }

    private fun groupQuote(
        productId: String,
        quantity: Int,
        base: PricingFixtureRule.BasePrice,
        group: PricingFixtureRule.GroupPromotion,
        context: PricingContext,
    ): QuotedItem {
        val promotedGroups = quantity / group.groupSize
        val promotedUnits = promotedGroups * group.groupSize
        val remainder = quantity - promotedUnits
        val totalMinor = Math.addExact(
            Math.multiplyExact(promotedGroups.toLong(), group.groupPriceMinor),
            Math.multiplyExact(remainder.toLong(), base.unitPriceMinor),
        )
        return QuotedItem(
            productId = productId,
            quantity = quantity,
            unitPrice = Money(base.unitPriceMinor, context.currencyCode),
            lineTotal = Money(totalMinor, context.currencyCode),
            appliedRules = listOf(
                rule(base.ruleId, AppliedPriceRuleKind.BASE, remainder, "Synthetic remainder at base price"),
                rule(group.ruleId, AppliedPriceRuleKind.GROUP_PROMOTION, promotedUnits, "Synthetic group promotion"),
            ),
        )
    }

    private fun rule(
        id: String,
        kind: AppliedPriceRuleKind,
        affectedUnits: Int,
        explanation: String,
    ) = AppliedPriceRule(id, kind, affectedUnits, explanation)
}
