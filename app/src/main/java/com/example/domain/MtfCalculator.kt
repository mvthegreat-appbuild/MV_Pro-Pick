package com.example.domain

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

data class MtfStock(
    val sym: String,
    val name: String,
    val sector: String,
    val lev: Double,
    val ltp: Double,
    val isCustom: Boolean = false
)

enum class MtfSizingMode {
    CAPITAL,
    QUANTITY
}

enum class RiskRewardVerdict(val label: String) {
    STRONG("Strong Setup (≥ 1:2)"),
    MODERATE("Moderate Setup"),
    POOR("Poor Risk-Reward (< 1:1.25)"),
    INVALID("Invalid / Negative Reward")
}

data class MtfChargeBreakdown(
    val brok: Double,
    val dailyInt: Double,
    val totalInt: Double,
    val pDp: Double,
    val sttStamp: Double,
    val exchGst: Double,
    val total: Double
)

data class MtfDurationCard(
    val days: Int,
    val netGain: Double,
    val interestCost: Double,
    val isActive: Boolean
)

data class MtfCalculationResult(
    val leverage: Double,
    val ownMarginPct: Double,
    val growwFundedPct: Double,
    val entryPrice: Double,
    val targetPrice: Double,
    val slPrice: Double,
    val holdingDays: Int,
    val qty: Int,
    val buyValue: Double,
    val yourMargin: Double,
    val growwLoan: Double,
    val targetPctFromEntry: Double,
    val slPctFromEntry: Double,
    val targetPerShareDelta: Double,
    val slPerShareDelta: Double,
    val targetCharges: MtfChargeBreakdown,
    val slCharges: MtfChargeBreakdown,
    val grossGain: Double,
    val netTargetGain: Double,
    val netTargetRoiPct: Double,
    val netSlLoss: Double,
    val netSlRoiPct: Double,
    val showMarginCallWarning: Boolean,
    val breakevenPrice: Double,
    val breakevenPctOverEntry: Double,
    val grossRR: Double?,
    val netRR: Double?,
    val verdict: RiskRewardVerdict,
    val ladderRiskBarPct: Float,
    val warnings: List<String>,
    val durationCards: List<MtfDurationCard>
)

data class MtfCalculatorInput(
    val sizingMode: MtfSizingMode,
    val sizingValue: Double,
    val entryPrice: Double,
    val targetPrice: Double,
    val slPrice: Double,
    val leverage: Double,
    val holdingDays: Int,
    val gstOnInterest: Boolean,
    val includePledgeDp: Boolean
)

object MtfCalculator {

    val STOCK_DB: List<MtfStock> = listOf(
        MtfStock("HDFCBANK", "HDFC Bank Ltd", "Banking", 4.00, 1720.0),
        MtfStock("RELIANCE", "Reliance Industries Ltd", "Energy", 4.00, 1310.0),
        MtfStock("TCS", "Tata Consultancy Services", "IT", 4.00, 4120.0),
        MtfStock("INFY", "Infosys Ltd", "IT", 4.00, 1860.0),
        MtfStock("ICICIBANK", "ICICI Bank Ltd", "Banking", 4.00, 1295.0),
        MtfStock("SBIN", "State Bank of India", "Banking", 4.00, 830.0),
        MtfStock("BHARTIARTL", "Bharti Airtel Ltd", "Telecom", 4.00, 1625.0),
        MtfStock("ITC", "ITC Ltd", "FMCG", 4.00, 482.0),
        MtfStock("LT", "Larsen & Toubro Ltd", "Infra", 4.00, 3640.0),
        MtfStock("KOTAKBANK", "Kotak Mahindra Bank", "Banking", 4.00, 1785.0),
        MtfStock("AXISBANK", "Axis Bank Ltd", "Banking", 4.00, 1170.0),
        MtfStock("BAJFINANCE", "Bajaj Finance Ltd", "NBFC", 4.00, 6950.0),
        MtfStock("TATAMOTORS", "Tata Motors Ltd", "Auto", 4.00, 815.0),
        MtfStock("TATASTEEL", "Tata Steel Ltd", "Metals", 3.95, 158.0),
        MtfStock("TATAPOWER", "Tata Power Company", "Power", 3.70, 425.0),
        MtfStock("SUNPHARMA", "Sun Pharma Ind", "Pharma", 4.00, 1815.0),
        MtfStock("TITAN", "Titan Company Ltd", "Consumer", 4.00, 3310.0),
        MtfStock("WIPRO", "Wipro Ltd", "IT", 4.00, 295.0),
        MtfStock("HCLTECH", "HCL Technologies", "IT", 4.00, 1840.0),
        MtfStock("NTPC", "NTPC Ltd", "Power", 4.00, 378.0),
        MtfStock("ONGC", "Oil & Natural Gas Corp", "Energy", 3.90, 262.0),
        MtfStock("COALINDIA", "Coal India Ltd", "Mining", 4.00, 432.0),
        MtfStock("ADANIPORTS", "Adani Ports & SEZ", "Logistics", 3.60, 1265.0),
        MtfStock("ADANIENT", "Adani Enterprises", "Diversified", 3.25, 2480.0),
        MtfStock("ADANIPOWER", "Adani Power Ltd", "Power", 3.00, 545.0),
        MtfStock("JSWSTEEL", "JSW Steel Ltd", "Metals", 4.00, 970.0),
        MtfStock("HINDALCO", "Hindalco Industries", "Metals", 4.00, 655.0),
        MtfStock("JIOFIN", "Jio Financial Services", "Finance", 3.75, 325.0),
        MtfStock("ETERNAL", "Eternal Ltd (Zomato)", "Tech", 3.50, 268.0),
        MtfStock("ZOMATO", "Zomato / Eternal Ltd", "Tech", 3.50, 268.0),
        MtfStock("BEL", "Bharat Electronics", "Defence", 3.85, 302.0),
        MtfStock("HAL", "Hindustan Aeronautics", "Defence", 3.65, 4280.0),
        MtfStock("MAZDOCK", "Mazagon Dock", "Defence", 3.05, 2380.0),
        MtfStock("TRENT", "Trent Ltd", "Retail", 3.70, 6680.0),
        MtfStock("VEDL", "Vedanta Ltd", "Metals", 3.55, 455.0),
        MtfStock("DLF", "DLF Ltd", "Realty", 3.70, 825.0),
        MtfStock("BHEL", "Bharat Heavy Electricals", "Capital Goods", 3.25, 242.0),
        MtfStock("SUZLON", "Suzlon Energy Ltd", "Renewables", 2.85, 62.0),
        MtfStock("IRFC", "IRFC Ltd", "PSU Finance", 3.15, 152.0),
        MtfStock("IREDA", "IREDA Ltd", "PSU Finance", 2.95, 205.0),
        MtfStock("RVNL", "Rail Vikas Nigam", "Railways", 3.00, 445.0),
        MtfStock("NHPC", "NHPC Ltd", "Power", 3.35, 84.0),
        MtfStock("PFC", "Power Finance Corp", "PSU Finance", 3.70, 468.0),
        MtfStock("RECLTD", "REC Ltd", "PSU Finance", 3.70, 525.0),
        MtfStock("BANKBARODA", "Bank of Baroda", "Banking", 3.85, 248.0),
        MtfStock("PNB", "Punjab National Bank", "Banking", 3.75, 106.0),
        MtfStock("CANBK", "Canara Bank", "Banking", 3.80, 103.0),
        MtfStock("IDFCFIRSTB", "IDFC First Bank", "Banking", 3.60, 66.0),
        MtfStock("YESBANK", "Yes Bank Ltd", "Banking", 2.75, 20.5),
        MtfStock("CDSL", "CDSL Ltd", "Markets", 3.30, 1680.0),
        MtfStock("BSE", "BSE Ltd", "Markets", 3.10, 4950.0),
        MtfStock("ANGELONE", "Angel One Ltd", "Broking", 3.15, 2890.0),
        MtfStock("DIXON", "Dixon Technologies", "Electronics", 3.45, 16200.0),
        MtfStock("IRCTC", "IRCTC Ltd", "Travel", 3.75, 825.0),
        MtfStock("PAYTM", "Paytm (One 97)", "Fintech", 2.85, 890.0),
        MtfStock("SWIGGY", "Swiggy Ltd", "Tech", 3.10, 465.0),
        MtfStock("SAIL", "Steel Authority of India", "Metals", 3.50, 118.0),
        MtfStock("NMDC", "NMDC Ltd", "Mining", 3.75, 232.0),
        MtfStock("BPCL", "Bharat Petroleum", "Energy", 3.95, 302.0),
        MtfStock("IOC", "Indian Oil Corp", "Energy", 3.95, 142.0),
        MtfStock("GOLDBEES", "Nippon Gold BeES", "ETF", 4.00, 64.0),
        MtfStock("NIFTYBEES", "Nippon Nifty BeES", "ETF", 4.00, 272.0),
        MtfStock("GROWW", "Groww (Billionbrains)", "Markets", 3.06, 217.0)
    )

    private val DURATION_PRESETS = listOf(5, 15, 30, 60, 90)

    fun computeCharges(
        buyVal: Double,
        sellVal: Double,
        loanAmt: Double,
        days: Int,
        gstInt: Boolean,
        pledgeDp: Boolean
    ): MtfChargeBreakdown {
        val brok = (buyVal + sellVal) * 0.001
        val dailyBase = loanAmt * (0.1495 / 365.0)
        val rawInt = dailyBase * days
        val intMult = if (gstInt) 1.18 else 1.0
        val totalInt = rawInt * intMult

        val stt = round(buyVal * 0.001) + round(sellVal * 0.001)
        val stamp = round(buyVal * 0.00015)
        val exch = (buyVal + sellVal) * 0.0000307
        val sebiIpft = (buyVal + sellVal) * 0.000002
        val pledgeBase = if (pledgeDp && loanAmt > 0.0) 40.0 else 0.0
        val dpBase = when {
            pledgeDp && sellVal >= 100.0 -> 20.0
            pledgeDp && sellVal > 0.0 -> 3.5
            else -> 0.0
        }
        val pDp = pledgeBase + dpBase
        val gst = (brok + exch + sebiIpft + pDp) * 0.18
        val nonInt = brok + stt + stamp + exch + sebiIpft + pDp + gst

        return MtfChargeBreakdown(
            brok = brok,
            dailyInt = dailyBase * intMult,
            totalInt = totalInt,
            pDp = pDp,
            sttStamp = stt + stamp,
            exchGst = exch + sebiIpft + gst,
            total = nonInt + totalInt
        )
    }

    fun calculate(input: MtfCalculatorInput): MtfCalculationResult {
        val lev = input.leverage.coerceIn(1.0, 10.0)
        val ownPct = 100.0 / lev
        val growwPct = 100.0 - ownPct

        val entry = max(0.05, input.entryPrice)
        val target = max(0.05, input.targetPrice)
        val sl = max(0.05, input.slPrice)
        val days = max(1, input.holdingDays)
        val sizing = max(0.0, input.sizingValue)

        val qty = when (input.sizingMode) {
            MtfSizingMode.CAPITAL -> if (entry > 0.0) floor((sizing * lev) / entry).toInt() else 0
            MtfSizingMode.QUANTITY -> floor(sizing).toInt()
        }

        val buyValue = qty * entry
        val margin = buyValue / lev
        val loan = max(0.0, buyValue - margin)

        val warnings = buildList {
            if (qty <= 0) add("Increase capital or lower entry price to buy at least 1 share.")
            if (target <= entry) add("Target should be above Entry price.")
            if (sl >= entry) add("Stop-Loss should be below Entry price.")
        }

        val targetSellVal = qty * target
        val slSellVal = qty * sl
        val tc = computeCharges(buyValue, targetSellVal, loan, days, input.gstOnInterest, input.includePledgeDp)
        val sc = computeCharges(buyValue, slSellVal, loan, days, input.gstOnInterest, input.includePledgeDp)

        val grossGain = targetSellVal - buyValue
        val netGain = grossGain - tc.total
        val netTargetRoi = if (margin > 0.0) (netGain / margin) * 100.0 else 0.0

        val netSL = (slSellVal - buyValue) - sc.total
        val netSLRoi = if (margin > 0.0) (netSL / margin) * 100.0 else 0.0

        val bePrice = if (qty > 0) entry + (tc.total / qty) else entry
        val bePct = if (entry > 0.0) ((bePrice - entry) / entry) * 100.0 else 0.0

        val gRisk = max(0.0, entry - sl)
        val gRew = max(0.0, target - entry)
        val grossRR = if (gRisk > 0.0 && gRew > 0.0) gRew / gRisk else null

        val nRisk = abs(min(0.0, netSL))
        val netRR = if (nRisk > 0.0 && target > entry && sl < entry) netGain / nRisk else null

        val verdict = when {
            netGain <= 0.0 || target <= entry || sl >= entry -> RiskRewardVerdict.INVALID
            (netRR ?: 0.0) >= 2.0 -> RiskRewardVerdict.STRONG
            (netRR ?: 0.0) >= 1.25 -> RiskRewardVerdict.MODERATE
            else -> RiskRewardVerdict.POOR
        }

        val span = gRisk + gRew
        val riskBarPct = if (span > 0.0) ((gRisk / span) * 100.0).coerceIn(15.0, 85.0).toFloat() else 50f

        val showMarginCall = margin > 0.0 && abs(netSL) > margin * 0.6 && lev > 1.0

        val durationCards = DURATION_PRESETS.map { d ->
            val ch = computeCharges(buyValue, targetSellVal, loan, d, input.gstOnInterest, input.includePledgeDp)
            MtfDurationCard(
                days = d,
                netGain = grossGain - ch.total,
                interestCost = ch.totalInt,
                isActive = days == d
            )
        }

        return MtfCalculationResult(
            leverage = lev,
            ownMarginPct = ownPct,
            growwFundedPct = growwPct,
            entryPrice = entry,
            targetPrice = target,
            slPrice = sl,
            holdingDays = days,
            qty = qty,
            buyValue = buyValue,
            yourMargin = margin,
            growwLoan = loan,
            targetPctFromEntry = ((target - entry) / entry) * 100.0,
            slPctFromEntry = ((sl - entry) / entry) * 100.0,
            targetPerShareDelta = target - entry,
            slPerShareDelta = sl - entry,
            targetCharges = tc,
            slCharges = sc,
            grossGain = grossGain,
            netTargetGain = netGain,
            netTargetRoiPct = netTargetRoi,
            netSlLoss = netSL,
            netSlRoiPct = netSLRoi,
            showMarginCallWarning = showMarginCall,
            breakevenPrice = bePrice,
            breakevenPctOverEntry = bePct,
            grossRR = grossRR,
            netRR = netRR,
            verdict = verdict,
            ladderRiskBarPct = riskBarPct,
            warnings = warnings,
            durationCards = durationCards
        )
    }

    fun filterStocks(query: String): List<MtfStock> {
        val q = query.trim().uppercase()
        if (q.isEmpty()) return STOCK_DB.take(12)
        return STOCK_DB.filter { stock ->
            stock.sym.contains(q) ||
                stock.name.uppercase().contains(q) ||
                stock.sector.uppercase().contains(q)
        }.take(12)
    }
}
