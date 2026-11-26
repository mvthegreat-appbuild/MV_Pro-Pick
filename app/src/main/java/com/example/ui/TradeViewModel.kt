package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FetchResult
import com.example.data.TradeRepository
import com.example.domain.MtfCalculationResult
import com.example.domain.MtfCalculator
import com.example.domain.MtfCalculatorInput
import com.example.domain.MtfSizingMode
import com.example.domain.MtfStock
import com.example.domain.TradeEvaluator
import com.example.model.EvaluatedTrade
import com.example.model.FeedSnapshot
import com.example.model.FeedTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

data class MtfCalculatorState(
    val selectedStock: MtfStock = MtfCalculator.STOCK_DB.first(),
    val searchQuery: String = MtfCalculator.STOCK_DB.first().sym,
    val showDropdown: Boolean = false,
    val leverageInput: String = String.format(Locale.US, "%.2f", MtfCalculator.STOCK_DB.first().lev),
    val leverageValue: Double = MtfCalculator.STOCK_DB.first().lev,
    val sizingMode: MtfSizingMode = MtfSizingMode.CAPITAL,
    val sizingInput: String = "50000",
    val entryPriceInput: String = "1720",
    val targetPriceInput: String = "1857.6",
    val slPriceInput: String = "1668.4",
    val holdingDays: Int = 15,
    val holdingDaysInput: String = "15",
    val gstOnInterest: Boolean = true,
    val includePledgeDp: Boolean = true,
    val breakdownExpanded: Boolean = true,
    val result: MtfCalculationResult = MtfCalculator.calculate(
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
)

data class TradeUiState(
    val activeTab: FeedTab = FeedTab.STOCKPRO,
    val searchQuery: String = "",
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val spSnapshot: FeedSnapshot? = null,
    val moSnapshot: FeedSnapshot? = null,
    val cmpMap: Map<String, Double> = emptyMap(),
    val filteredTrades: List<EvaluatedTrade> = emptyList(),
    val lastCheckedAtMillis: Long = System.currentTimeMillis(),
    val mtfState: MtfCalculatorState = MtfCalculatorState()
) {
    val activeSnapshot: FeedSnapshot?
        get() = when (activeTab) {
            FeedTab.STOCKPRO -> spSnapshot
            FeedTab.MOTILAL -> moSnapshot
            FeedTab.MTF -> null
        }
}

class TradeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val REFRESH_INTERVAL_MS = 5 * 60 * 1000L
    }

    private val repository = TradeRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(TradeUiState())
    val uiState: StateFlow<TradeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedTab = repository.selectedTabFlow.first()
            val cached = repository.loadCachedOnly()
            _uiState.update { state ->
                val updated = state.copy(
                    activeTab = savedTab,
                    spSnapshot = cached.stockPro,
                    moSnapshot = cached.motilal,
                    cmpMap = cached.cmpMap,
                    lastCheckedAtMillis = cached.checkedAtMillis,
                    isInitialLoading = cached.stockPro == null && cached.motilal == null
                )
                updated.copy(filteredTrades = computeFilteredTrades(updated))
            }
            if (savedTab != FeedTab.MTF) {
                refresh()
            } else {
                _uiState.update { it.copy(isInitialLoading = false) }
            }
        }
        startAutoRefreshLoop()
    }

    private fun startAutoRefreshLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                if (_uiState.value.activeTab != FeedTab.MTF) {
                    refresh()
                }
            }
        }
    }

    fun selectTab(tab: FeedTab) {
        val prevTab = _uiState.value.activeTab
        _uiState.update { state ->
            val updated = state.copy(activeTab = tab)
            updated.copy(filteredTrades = computeFilteredTrades(updated))
        }
        viewModelScope.launch {
            repository.saveSelectedTab(tab)
        }
        if (prevTab == FeedTab.MTF && tab != FeedTab.MTF) {
            val elapsed = System.currentTimeMillis() - _uiState.value.lastCheckedAtMillis
            if (elapsed >= REFRESH_INTERVAL_MS) {
                refresh()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val updated = state.copy(searchQuery = query)
            updated.copy(filteredTrades = computeFilteredTrades(updated))
        }
    }

    // ---- Groww MTF Calculator Events ----

    fun updateMtfSearchQuery(query: String) {
        _uiState.update { state ->
            state.copy(
                mtfState = state.mtfState.copy(
                    searchQuery = query,
                    showDropdown = true
                )
            )
        }
    }

    fun setMtfDropdownVisible(visible: Boolean) {
        _uiState.update { state ->
            state.copy(mtfState = state.mtfState.copy(showDropdown = visible))
        }
    }

    fun clearMtfSearch() {
        _uiState.update { state ->
            state.copy(
                mtfState = state.mtfState.copy(
                    searchQuery = "",
                    showDropdown = true
                )
            )
        }
    }

    fun selectMtfStock(stock: MtfStock, keepPrices: Boolean = false) {
        updateMtfState { cur ->
            val newEntry = if (!keepPrices && stock.ltp > 0.0) formatCleanDecimal(stock.ltp) else cur.entryPriceInput
            val newTarget = if (!keepPrices && stock.ltp > 0.0) {
                formatCleanDecimal(kotlin.math.round(stock.ltp * 1.08 * 100.0) / 100.0)
            } else {
                cur.targetPriceInput
            }
            val newSl = if (!keepPrices && stock.ltp > 0.0) {
                formatCleanDecimal(kotlin.math.round(stock.ltp * 0.97 * 100.0) / 100.0)
            } else {
                cur.slPriceInput
            }
            cur.copy(
                selectedStock = stock,
                searchQuery = stock.sym,
                showDropdown = false,
                leverageValue = stock.lev.coerceIn(1.0, 10.0),
                leverageInput = String.format(Locale.US, "%.2f", stock.lev),
                entryPriceInput = newEntry,
                targetPriceInput = newTarget,
                slPriceInput = newSl
            )
        }
    }

    fun selectCustomMtfStock(symbolQuery: String) {
        val cleanSym = symbolQuery.trim().uppercase(Locale.ROOT)
        if (cleanSym.isEmpty()) return
        val cur = _uiState.value.mtfState
        val customStock = MtfStock(
            sym = cleanSym,
            name = "Custom Equity",
            sector = "Manual Leverage",
            lev = cur.leverageValue.takeIf { it >= 1.0 } ?: 3.5,
            ltp = cur.entryPriceInput.toDoubleOrNull() ?: 500.0,
            isCustom = true
        )
        selectMtfStock(customStock, keepPrices = true)
    }

    fun updateMtfLeverage(raw: String) {
        val sanitized = sanitizeDecimal(raw)
        updateMtfState { cur ->
            val parsed = sanitized.toDoubleOrNull()
            val clamped = parsed?.coerceIn(1.0, 10.0) ?: cur.leverageValue
            cur.copy(
                leverageInput = sanitized,
                leverageValue = clamped
            )
        }
    }

    fun commitMtfLeverageBlur() {
        updateMtfState { cur ->
            val parsed = cur.leverageInput.toDoubleOrNull()
            val clean = parsed?.coerceIn(1.0, 10.0) ?: 1.0
            cur.copy(
                leverageValue = clean,
                leverageInput = String.format(Locale.US, "%.2f", clean)
            )
        }
    }

    fun setMtfSizingMode(mode: MtfSizingMode) {
        updateMtfState { cur ->
            if (cur.sizingMode == mode) return@updateMtfState cur
            val entry = max(0.05, cur.entryPriceInput.toDoubleOrNull() ?: 0.0)
            val lev = max(1.0, cur.leverageValue)
            val sizingNum = cur.sizingInput.toDoubleOrNull() ?: 0.0
            val converted = when (mode) {
                MtfSizingMode.QUANTITY -> {
                    val estQty = max(1, floor((sizingNum * lev) / entry).toInt())
                    estQty.toString()
                }
                MtfSizingMode.CAPITAL -> {
                    val qty = max(1, floor(sizingNum).toInt())
                    ((qty * entry) / lev).roundToInt().toString()
                }
            }
            cur.copy(
                sizingMode = mode,
                sizingInput = converted
            )
        }
    }

    fun updateMtfSizingInput(raw: String) {
        val sanitized = if (_uiState.value.mtfState.sizingMode == MtfSizingMode.QUANTITY) {
            sanitizeDigits(raw)
        } else {
            sanitizeDecimal(raw)
        }
        updateMtfState { it.copy(sizingInput = sanitized) }
    }

    fun updateMtfEntryPrice(raw: String) =
        updateMtfState { it.copy(entryPriceInput = sanitizeDecimal(raw)) }

    fun updateMtfTargetPrice(raw: String) =
        updateMtfState { it.copy(targetPriceInput = sanitizeDecimal(raw)) }

    fun updateMtfSlPrice(raw: String) =
        updateMtfState { it.copy(slPriceInput = sanitizeDecimal(raw)) }

    fun updateMtfHoldingDaysFromSlider(daysFloat: Float) {
        val d = daysFloat.roundToInt().coerceIn(1, 180)
        updateMtfState {
            it.copy(
                holdingDays = d,
                holdingDaysInput = d.toString()
            )
        }
    }

    fun updateMtfHoldingDaysInput(raw: String) {
        val digits = sanitizeDigits(raw)
        updateMtfState { cur ->
            val parsed = digits.toIntOrNull()
            val clamped = parsed?.coerceIn(1, 365) ?: cur.holdingDays
            cur.copy(
                holdingDaysInput = digits,
                holdingDays = clamped
            )
        }
    }

    fun commitMtfHoldingDaysBlur() {
        updateMtfState { cur ->
            val clean = (cur.holdingDaysInput.toIntOrNull() ?: 1).coerceIn(1, 365)
            cur.copy(
                holdingDays = clean,
                holdingDaysInput = clean.toString()
            )
        }
    }

    fun toggleMtfGstOnInterest(checked: Boolean) =
        updateMtfState { it.copy(gstOnInterest = checked) }

    fun toggleMtfIncludePledgeDp(checked: Boolean) =
        updateMtfState { it.copy(includePledgeDp = checked) }

    fun toggleMtfBreakdownExpanded() {
        _uiState.update { state ->
            state.copy(
                mtfState = state.mtfState.copy(
                    breakdownExpanded = !state.mtfState.breakdownExpanded
                )
            )
        }
    }

    fun selectMtfDurationPreset(days: Int) {
        updateMtfState {
            it.copy(
                holdingDays = days.coerceIn(1, 365),
                holdingDaysInput = days.toString()
            )
        }
    }

    private fun updateMtfState(transform: (MtfCalculatorState) -> MtfCalculatorState) {
        _uiState.update { state ->
            val next = transform(state.mtfState)
            val input = MtfCalculatorInput(
                sizingMode = next.sizingMode,
                sizingValue = next.sizingInput.toDoubleOrNull() ?: 0.0,
                entryPrice = next.entryPriceInput.toDoubleOrNull() ?: 0.0,
                targetPrice = next.targetPriceInput.toDoubleOrNull() ?: 0.0,
                slPrice = next.slPriceInput.toDoubleOrNull() ?: 0.0,
                leverage = next.leverageValue,
                holdingDays = next.holdingDays,
                gstOnInterest = next.gstOnInterest,
                includePledgeDp = next.includePledgeDp
            )
            state.copy(mtfState = next.copy(result = MtfCalculator.calculate(input)))
        }
    }

    private fun formatCleanDecimal(v: Double): String {
        val asLong = v.toLong()
        return if (v == asLong.toDouble()) asLong.toString() else v.toString()
    }

    private fun sanitizeDecimal(raw: String): String {
        val filtered = raw.filter { it.isDigit() || it == '.' }
        val dotIndex = filtered.indexOf('.')
        return if (dotIndex == -1) {
            filtered
        } else {
            filtered.substring(0, dotIndex + 1) + filtered.substring(dotIndex + 1).replace(".", "")
        }
    }

    private fun sanitizeDigits(raw: String): String = raw.filter { it.isDigit() }

    fun onForegroundResumed() {
        if (_uiState.value.activeTab == FeedTab.MTF) return
        val elapsed = System.currentTimeMillis() - _uiState.value.lastCheckedAtMillis
        if (elapsed >= 30_000L) {
            refresh()
        }
    }

    fun refresh() {
        if (_uiState.value.activeTab == FeedTab.MTF) return
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val result: FetchResult = repository.fetchAll()
            _uiState.update { state ->
                val updated = state.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    spSnapshot = result.stockPro ?: state.spSnapshot,
                    moSnapshot = result.motilal ?: state.moSnapshot,
                    cmpMap = if (result.cmpMap.isNotEmpty()) result.cmpMap else state.cmpMap,
                    lastCheckedAtMillis = result.checkedAtMillis
                )
                updated.copy(filteredTrades = computeFilteredTrades(updated))
            }
        }
    }

    private fun computeFilteredTrades(state: TradeUiState): List<EvaluatedTrade> {
        if (state.activeTab == FeedTab.MTF) return emptyList()
        val snapshot = state.activeSnapshot ?: return emptyList()
        val evaluated = snapshot.feed.trades.map { trade ->
            TradeEvaluator.evaluate(trade, state.cmpMap)
        }
        val normalizedQuery = TradeEvaluator.normalizeSearch(state.searchQuery)
        if (normalizedQuery.isEmpty()) return evaluated
        return evaluated.filter { it.searchTextNormalized.contains(normalizedQuery) }
    }
}
