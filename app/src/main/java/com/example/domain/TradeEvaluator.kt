package com.example.domain

import com.example.model.EvaluatedTrade
import com.example.model.TradeDto
import com.example.model.TradeStatus
import kotlinx.serialization.json.doubleOrNull
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object TradeEvaluator {

    private val IST_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")
    private val IN_LOCALE: Locale = Locale.forLanguageTag("en-IN")
    private val DATE_ONLY_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")
    private val SUFFIX_REGEX = Regex("""\.(NS|BO)$""")
    private val SPLIT_REGEX = Regex("""[\s(]""")
    private val TICKER_STRIP_REGEX = Regex("""[^A-Z0-9&-]""")
    private val SEARCH_NORM_REGEX = Regex("""[^a-z0-9]""")

    private val NAME_MAP = mapOf(
        "TATA TECH" to "TATATECH",
        "TATA TECHNOLOGIES" to "TATATECH",
        "EICHER MOTORS" to "EICHERMOT",
        "BLS POLYMERS" to "BLSPOLY",
        "TATA MOTORS" to "TATAMOTORS",
        "TATA POWER" to "TATAPOWER",
        "TATA STEEL" to "TATASTEEL",
        "HDFC BANK" to "HDFCBANK",
        "ICICI BANK" to "ICICIBANK",
        "STATE BANK" to "SBIN",
        "INFOSYS" to "INFY",
        "BHARTI AIRTEL" to "BHARTIARTL",
        "BAJAJ FINANCE" to "BAJFINANCE"
    )

    fun normalizeSearch(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return input.lowercase(Locale.ROOT).replace(SEARCH_NORM_REGEX, "")
    }

    fun parseNumber(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        return raw.replace(",", "").trim().toDoubleOrNull()
    }

    fun formatInr(value: Double): String {
        val nf = NumberFormat.getNumberInstance(IN_LOCALE).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
            isGroupingUsed = true
        }
        return "₹${nf.format(value)}"
    }

    fun formatPct(value: Double): String {
        val sign = when {
            value > 0.0 -> "+"
            value < 0.0 -> "-"
            else -> ""
        }
        return String.format(Locale.US, "%s%.2f%%", sign, abs(value))
    }

    fun getTicker(trade: TradeDto): String {
        val rawCandidate = (trade.symbolNse?.takeIf { it.isNotBlank() } ?: trade.symbol)
            .trim()
            .uppercase(Locale.ROOT)
            .replace(SUFFIX_REGEX, "")
        if (rawCandidate.isNotEmpty()) return rawCandidate

        val upperName = trade.name.trim().uppercase(Locale.ROOT)
        for ((key, ticker) in NAME_MAP) {
            if (upperName.contains(key)) return ticker
        }
        val firstToken = upperName.split(SPLIT_REGEX).firstOrNull().orEmpty()
        return firstToken.replace(TICKER_STRIP_REGEX, "")
    }

    fun formatDateIst(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        val trimmed = iso.trim()
        return runCatching {
            if (DATE_ONLY_REGEX.matches(trimmed)) {
                val date = LocalDate.parse(trimmed)
                date.format(DateTimeFormatter.ofPattern("d MMM", IN_LOCALE))
            } else {
                val instant = runCatching { OffsetDateTime.parse(trimmed).toInstant() }
                    .getOrElse { Instant.parse(trimmed) }
                val zoned = instant.atZone(IST_ZONE)
                val datePart = zoned.format(DateTimeFormatter.ofPattern("d MMM", IN_LOCALE))
                val timePart = zoned.format(DateTimeFormatter.ofPattern("hh:mm a", IN_LOCALE))
                    .lowercase(IN_LOCALE)
                "$datePart · $timePart"
            }
        }.getOrDefault("")
    }

    fun formatUpdatedHeaderTime(iso: String?): String {
        if (iso.isNullOrBlank()) return "unknown"
        return runCatching {
            val instant = runCatching { OffsetDateTime.parse(iso.trim()).toInstant() }
                .getOrElse { Instant.parse(iso.trim()) }
            val zoned = instant.atZone(ZoneId.systemDefault())
            zoned.format(DateTimeFormatter.ofPattern("d MMM yyyy, hh:mm:ss a", IN_LOCALE))
        }.getOrDefault("unknown")
    }

    fun formatCheckedAtTime(millis: Long): String {
        val zoned = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return zoned.format(DateTimeFormatter.ofPattern("hh:mm:ss a", IN_LOCALE))
    }

    fun evaluate(trade: TradeDto, cmpMap: Map<String, Double>): EvaluatedTrade {
        val entry = parseNumber(trade.entry) ?: 0.0
        val sl = parseNumber(trade.sl) ?: 0.0
        val targets = trade.targets.mapNotNull { parseNumber(it) }
        val isSell = trade.side.equals("SELL", ignoreCase = true)
        val sym = getTicker(trade)

        val cmpFromTrade = trade.cmp?.doubleOrNull
            ?: trade.cmp?.content?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        val ltpFromTrade = trade.ltp?.doubleOrNull
            ?: trade.ltp?.content?.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        val rawCmp = cmpFromTrade ?: ltpFromTrade ?: cmpMap[sym]
        val cmp = rawCmp?.takeIf { it.isFinite() && it > 0.0 }

        var hits = trade.targetsHit ?: 0
        var statusKey = trade.status?.lowercase(Locale.ROOT).orEmpty()
        val entered = !trade.entryTime.isNullOrBlank() || statusKey in listOf("active", "target", "sl")

        if (cmp != null) {
            val calcHits = targets.count { target -> if (isSell) cmp <= target else cmp >= target }
            if (calcHits > hits) hits = calcHits
            val slHit = entered && sl.isFinite() && sl > 0.0 && (if (isSell) cmp >= sl else cmp <= sl)
            if (statusKey == "target" || statusKey == "sl") {
                // Keep server-evaluated terminal outcome
            } else if (slHit) {
                statusKey = "sl"
            } else if (statusKey.isEmpty() || statusKey == "unknown" || statusKey == "waiting") {
                statusKey = when {
                    hits > 0 -> "target"
                    if (isSell) cmp <= entry else cmp >= entry -> "active"
                    else -> "waiting"
                }
            }
        } else if (statusKey.isEmpty()) {
            statusKey = "unknown"
        }

        val status = when (statusKey) {
            "target" -> TradeStatus.TARGET
            "sl" -> TradeStatus.SL
            "active" -> TradeStatus.ACTIVE
            "waiting" -> TradeStatus.WAITING
            "missed" -> TradeStatus.MISSED
            else -> TradeStatus.UNKNOWN
        }

        val totalTargets = targets.size
        val statusLabel = when (status) {
            TradeStatus.TARGET -> when {
                totalTargets == 1 -> "TARGET HIT"
                hits >= totalTargets -> "ALL TARGETS HIT"
                else -> "TARGET $hits HIT"
            }
            TradeStatus.SL -> "STOP LOSS HIT"
            TradeStatus.ACTIVE -> "ACTIVE"
            TradeStatus.WAITING -> "WAITING ENTRY"
            TradeStatus.MISSED -> "NOT TRIGGERED"
            TradeStatus.UNKNOWN -> (trade.statusText?.takeIf { it.isNotBlank() } ?: "CMP N/A")
                .uppercase(Locale.ROOT)
        }

        val t1 = targets.firstOrNull()
        val tLast = targets.lastOrNull()
        val safeEntry = if (entry > 0.0) entry else 1.0
        val riskPct = abs(entry - sl) / safeEntry * 100.0
        val r1Pct = t1?.let { abs(it - entry) / safeEntry * 100.0 } ?: 0.0
        val rLastPct = tLast?.let { abs(it - entry) / safeEntry * 100.0 } ?: 0.0
        val denom = abs(entry - sl).let { if (it == 0.0) 1.0 else it }
        val rr1 = t1?.let { abs(it - entry) / denom } ?: 0.0
        val rrLast = tLast?.let { abs(it - entry) / denom } ?: 0.0
        val movePct = cmp?.let {
            ((if (isSell) (entry - it) else (it - entry)) / safeEntry) * 100.0
        }

        val title = trade.symbol.takeIf { it.isNotBlank() } ?: trade.name
        val distinctName = trade.name.takeIf {
            it.isNotBlank() && trade.symbol.isNotBlank() && it != trade.symbol
        }
        val subtitleMeta = listOfNotNull(
            distinctName,
            formatDateIst(trade.date).takeIf { it.isNotBlank() },
            trade.period?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")

        val entryLabel = trade.entryLabel?.takeIf { it.isNotBlank() }
            ?: if (isSell) "Entry · below" else "Entry · above"

        val searchBlob = normalizeSearch(
            "${trade.name} ${trade.symbol} ${trade.symbolNse.orEmpty()} $sym"
        )

        return EvaluatedTrade(
            id = trade.id,
            title = title,
            subtitleMeta = subtitleMeta,
            side = trade.side.uppercase(Locale.ROOT).ifBlank { "BUY" },
            isSell = isSell,
            status = status,
            statusLabel = statusLabel,
            entryLabel = entryLabel,
            entryPriceFormatted = formatInr(entry),
            cmpSymbolLabel = sym.ifBlank { "NSE" },
            cmpFormatted = cmp?.let { formatInr(it) },
            slFormatted = formatInr(sl),
            targetsFormatted = targets.map { formatInr(it) },
            hitsCount = hits,
            movePct = movePct,
            moveFormatted = movePct?.let { formatPct(it) } ?: "—",
            riskPctFormatted = String.format(Locale.US, "%.2f%%", riskPct),
            toT1PctFormatted = String.format(Locale.US, "%.2f%%", r1Pct),
            toLastPctFormatted = String.format(Locale.US, "%.2f%%", rLastPct),
            rrT1Formatted = String.format(Locale.US, "1 : %.2f", rr1),
            rrLastFormatted = String.format(Locale.US, "1 : %.2f", rrLast),
            idea = trade.idea?.takeIf { it.isNotBlank() },
            hasPointsTargetNote = !trade.targetNote.isNullOrBlank(),
            hasLevelWarning = trade.checkLevels,
            link = trade.link?.takeIf { it.isNotBlank() && it != "#" },
            searchTextNormalized = searchBlob
        )
    }
}
