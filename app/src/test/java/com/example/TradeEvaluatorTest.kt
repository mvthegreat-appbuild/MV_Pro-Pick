package com.example

import com.example.domain.TradeEvaluator
import com.example.model.TradeDto
import com.example.model.TradeStatus
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeEvaluatorTest {

    @Test
    fun getTicker_prefersSymbolNseAndFallsBackToNameMap() {
        val withNse = TradeDto(name = "SURMA SGS", symbol = "", symbolNse = "syrma")
        assertEquals("SYRMA", TradeEvaluator.getTicker(withNse))

        val mapped = TradeDto(name = "TATA TECH", symbol = "")
        assertEquals("TATATECH", TradeEvaluator.getTicker(mapped))

        val stripped = TradeDto(name = "Eicher Motors", symbol = "EICHERMOT.NS")
        assertEquals("EICHERMOT", TradeEvaluator.getTicker(stripped))
    }

    @Test
    fun evaluate_computesHitsStatusAndMetrics() {
        val trade = TradeDto(
            id = 137463L,
            name = "TECHM",
            symbol = "",
            side = "BUY",
            entry = "1835",
            sl = "1815",
            targets = listOf("1845", "1855", "1865", "1875"),
            targetNote = "points from entry",
            period = "Hold few days",
            date = "2026-11-26T04:25:51+00:00",
            targetsHit = 1,
            entryTime = "2026-11-26T14:43:10.826645+05:30",
            status = "target",
            symbolNse = "TECHM",
            cmp = JsonPrimitive(1854.7)
        )

        val evaluated = TradeEvaluator.evaluate(trade, emptyMap())
        assertEquals("TECHM", evaluated.title)
        assertEquals(TradeStatus.TARGET, evaluated.status)
        assertEquals("TARGET 1 HIT", evaluated.statusLabel)
        assertEquals(1, evaluated.hitsCount)
        assertEquals("1 : 0.50", evaluated.rrT1Formatted)
        assertEquals("1 : 2.00", evaluated.rrLastFormatted)
        assertTrue(evaluated.hasPointsTargetNote)
        assertFalse(evaluated.hasLevelWarning)
    }

    @Test
    fun evaluate_usesCmpMapFallbackAndEvaluatesSellCall() {
        val sellTrade = TradeDto(
            id = 999L,
            name = "INFOSYS",
            symbol = "",
            side = "SELL",
            entry = "1500",
            sl = "1530",
            targets = listOf("1470", "1440"),
            status = "waiting"
        )

        val evaluated = TradeEvaluator.evaluate(sellTrade, mapOf("INFY" to 1465.0))
        assertEquals("INFY", evaluated.cmpSymbolLabel)
        assertEquals(TradeStatus.TARGET, evaluated.status)
        assertEquals(1, evaluated.hitsCount)
        assertEquals("Entry · below", evaluated.entryLabel)
        assertEquals("+2.33%", evaluated.moveFormatted)
    }

    @Test
    fun formatDateIst_formatsDateOnlyAndFullTimestamp() {
        assertEquals("25 Nov", TradeEvaluator.formatDateIst("2026-11-25"))
        val full = TradeEvaluator.formatDateIst("2026-11-26T04:25:51+00:00")
        assertTrue(full.startsWith("26 Nov · 09:55"))
    }
}
