package com.example.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class TradeFeedDto(
    val updated: String? = null,
    val channel: String? = null,
    val trades: List<TradeDto> = emptyList()
)

@Serializable
data class TradeDto(
    val id: Long = 0L,
    val name: String = "",
    val symbol: String = "",
    val side: String = "BUY",
    val category: String? = null,
    val entry: String = "0",
    val sl: String = "0",
    val targets: List<String> = emptyList(),
    @SerialName("target_note")
    val targetNote: String? = null,
    val period: String? = null,
    @SerialName("check_levels")
    val checkLevels: Boolean = false,
    @SerialName("entry_label")
    val entryLabel: String? = null,
    val idea: String? = null,
    val date: String? = null,
    val link: String? = null,
    @SerialName("targets_hit")
    val targetsHit: Int? = null,
    @SerialName("entry_time")
    val entryTime: String? = null,
    @SerialName("hit_time")
    val hitTime: String? = null,
    @SerialName("sl_time")
    val slTime: String? = null,
    @SerialName("realised_pct")
    val realisedPct: Double? = null,
    @SerialName("pnl_pct")
    val pnlPct: Double? = null,
    @SerialName("upside_left_pct")
    val upsideLeftPct: Double? = null,
    @SerialName("to_entry_pct")
    val toEntryPct: Double? = null,
    val status: String? = null,
    @SerialName("status_text")
    val statusText: String? = null,
    @SerialName("symbol_nse")
    val symbolNse: String? = null,
    val cmp: JsonPrimitive? = null,
    val ltp: JsonPrimitive? = null,
    @SerialName("cmp_time")
    val cmpTime: String? = null
)

enum class FeedTab(
    val key: String,
    val fileName: String
) {
    STOCKPRO("sp", "positional_trades.json"),
    MOTILAL("mo", "motilal_trades.json"),
    MTF("mtf", "");

    companion object {
        fun fromKey(key: String?): FeedTab = when (key) {
            MOTILAL.key -> MOTILAL
            MTF.key -> MTF
            else -> STOCKPRO
        }
    }
}

enum class TradeStatus {
    TARGET,
    SL,
    ACTIVE,
    WAITING,
    MISSED,
    UNKNOWN
}

data class EvaluatedTrade(
    val id: Long,
    val title: String,
    val subtitleMeta: String,
    val side: String,
    val isSell: Boolean,
    val status: TradeStatus,
    val statusLabel: String,
    val entryLabel: String,
    val entryPriceFormatted: String,
    val cmpSymbolLabel: String,
    val cmpFormatted: String?,
    val slFormatted: String,
    val targetsFormatted: List<String>,
    val hitsCount: Int,
    val movePct: Double?,
    val moveFormatted: String,
    val riskPctFormatted: String,
    val toT1PctFormatted: String,
    val toLastPctFormatted: String,
    val rrT1Formatted: String,
    val rrLastFormatted: String,
    val idea: String?,
    val hasPointsTargetNote: Boolean,
    val hasLevelWarning: Boolean,
    val link: String?,
    val searchTextNormalized: String
)

data class FeedSnapshot(
    val feed: TradeFeedDto,
    val isLive: Boolean,
    val checkedAtMillis: Long
)
