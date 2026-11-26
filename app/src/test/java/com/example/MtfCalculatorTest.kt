package com.example

import com.example.domain.MtfCalculator
import com.example.domain.MtfCalculatorInput
import com.example.domain.MtfSizingMode
import com.example.domain.RiskRewardVerdict
import com.example.model.FeedTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MtfCalculatorTest {

    @Test
    fun calculate_defaultHdfcBankMatchesHtmlValues() {
        val result = MtfCalculator.calculate(
            MtfCalculatorInput(
                sizingMode = MtfSizingMode.CAPITAL,
                sizingValue = 50000.0,
                entryPrice = 1720.0,
                targetPrice = 1857.6,
                slPrice = 1668.4,
                leverage = 4.00,
                holdingDays = 15,
                gstOnInterest = true,
                includePledgeDp = true
            )
        )

        assertEquals(116, result.qty)
        assertEquals(199_520.0, result.buyValue, 0.01)
        assertEquals(49_880.0, result.yourMargin, 0.01)
        assertEquals(149_640.0, result.growwLoan, 0.01)
        assertEquals(72.32, result.targetCharges.dailyInt, 0.05)
        assertEquals(13_855.24, result.netTargetGain, 1.0)
        assertEquals(-8_044.22, result.netSlLoss, 1.0)
        assertNotNull(result.netRR)
        assertEquals(1.72, result.netRR!!, 0.02)
        assertNotNull(result.grossRR)
        assertEquals(2.67, result.grossRR!!, 0.02)
        assertEquals(RiskRewardVerdict.MODERATE, result.verdict)
        assertFalse(result.showMarginCallWarning)
        assertEquals(listOf(5, 15, 30, 60, 90), result.durationCards.map { it.days })
        assertTrue(result.durationCards.first { it.days == 15 }.isActive)
    }

    @Test
    fun calculate_quantityModeAnd1xLeverageHaveNoLoanOrPledgeFees() {
        val result = MtfCalculator.calculate(
            MtfCalculatorInput(
                sizingMode = MtfSizingMode.QUANTITY,
                sizingValue = 20.0,
                entryPrice = 500.0,
                targetPrice = 550.0,
                slPrice = 475.0,
                leverage = 1.0,
                holdingDays = 30,
                gstOnInterest = false,
                includePledgeDp = true
            )
        )

        assertEquals(20, result.qty)
        assertEquals(10_000.0, result.yourMargin, 0.01)
        assertEquals(0.0, result.growwLoan, 0.01)
        assertEquals(0.0, result.targetCharges.totalInt, 0.01)
        // Only DP (₹20), zero pledge fee because loanAmt == 0
        assertEquals(20.0, result.targetCharges.pDp, 0.01)
    }

    @Test
    fun filterStocks_findsBySymbolNameAndSector() {
        assertTrue(MtfCalculator.filterStocks("SUZLON").any { it.sym == "SUZLON" })
        assertTrue(MtfCalculator.filterStocks("Defence").any { it.sym == "HAL" })
        assertEquals(63, MtfCalculator.STOCK_DB.size)
    }

    @Test
    fun feedTab_parsesMtfKey() {
        assertEquals(FeedTab.MTF, FeedTab.fromKey("mtf"))
        assertEquals(FeedTab.MOTILAL, FeedTab.fromKey("mo"))
        assertEquals(FeedTab.STOCKPRO, FeedTab.fromKey("sp"))
    }
}
