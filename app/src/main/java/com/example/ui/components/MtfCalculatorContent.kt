package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.MtfCalculationResult
import com.example.domain.MtfCalculator
import com.example.domain.MtfDurationCard
import com.example.domain.MtfSizingMode
import com.example.domain.MtfStock
import com.example.domain.RiskRewardVerdict
import com.example.ui.MtfCalculatorState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private val MtfMint = Color(0xFF00D09C)
private val MtfMintLight = Color(0xFF6EE7B7)
private val MtfCyan = Color(0xFF38BDF8)
private val MtfIndigo = Color(0xFF818CF8)
private val MtfGold = Color(0xFFFBBF24)
private val MtfGoldLight = Color(0xFFFDE68A)
private val MtfProfit = Color(0xFF10B981)
private val MtfProfitBright = Color(0xFF34D399)
private val MtfLoss = Color(0xFFFB7185)
private val MtfLossLight = Color(0xFFFDA4AF)
private val MtfLossDeep = Color(0xFFF43F5E)
private val MtfTxt = Color(0xFFF1F5F9)
private val MtfSub = Color(0xFF94A3B8)
private val MtfMuted = Color(0xFF64748B)
private val MtfFieldBg = Color(0x99060E18)
private val MtfDarkBg = Color(0xAA050B12)
private val MtfDropdownBg = Color(0xF50A1422)
private val MtfBorder = Color(0x29FFFFFF)
private val MtfBorderSubtle = Color(0x1EFFFFFF)

private val IN_LOCALE: Locale = Locale.forLanguageTag("en-IN")

@Composable
fun MtfCalculatorContent(
    state: MtfCalculatorState,
    onSearchQueryChange: (String) -> Unit,
    onDropdownVisibilityChange: (Boolean) -> Unit,
    onClearSearch: () -> Unit,
    onSelectStock: (MtfStock) -> Unit,
    onSelectCustomStock: (String) -> Unit,
    onLeverageChange: (String) -> Unit,
    onLeverageBlur: () -> Unit,
    onSizingModeChange: (MtfSizingMode) -> Unit,
    onSizingInputChange: (String) -> Unit,
    onEntryPriceChange: (String) -> Unit,
    onTargetPriceChange: (String) -> Unit,
    onSlPriceChange: (String) -> Unit,
    onHoldingDaysSliderChange: (Float) -> Unit,
    onHoldingDaysInputChange: (String) -> Unit,
    onHoldingDaysBlur: () -> Unit,
    onToggleGstOnInterest: (Boolean) -> Unit,
    onToggleIncludePledgeDp: (Boolean) -> Unit,
    onToggleBreakdown: () -> Unit,
    onSelectDurationPreset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mtf_calculator_root")
    ) {
        val isTwoColumn = maxWidth >= 820.dp
        if (isTwoColumn) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                LeftInputPanel(
                    state = state,
                    onSearchQueryChange = onSearchQueryChange,
                    onDropdownVisibilityChange = onDropdownVisibilityChange,
                    onClearSearch = onClearSearch,
                    onSelectStock = onSelectStock,
                    onSelectCustomStock = onSelectCustomStock,
                    onLeverageChange = onLeverageChange,
                    onLeverageBlur = onLeverageBlur,
                    onSizingModeChange = onSizingModeChange,
                    onSizingInputChange = onSizingInputChange,
                    onEntryPriceChange = onEntryPriceChange,
                    onTargetPriceChange = onTargetPriceChange,
                    onSlPriceChange = onSlPriceChange,
                    onHoldingDaysSliderChange = onHoldingDaysSliderChange,
                    onHoldingDaysInputChange = onHoldingDaysInputChange,
                    onHoldingDaysBlur = onHoldingDaysBlur,
                    onToggleGstOnInterest = onToggleGstOnInterest,
                    onToggleIncludePledgeDp = onToggleIncludePledgeDp,
                    modifier = Modifier.weight(1.04f)
                )
                RightOutputPanel(
                    state = state,
                    onToggleBreakdown = onToggleBreakdown,
                    onSelectDurationPreset = onSelectDurationPreset,
                    modifier = Modifier.weight(1.16f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LeftInputPanel(
                    state = state,
                    onSearchQueryChange = onSearchQueryChange,
                    onDropdownVisibilityChange = onDropdownVisibilityChange,
                    onClearSearch = onClearSearch,
                    onSelectStock = onSelectStock,
                    onSelectCustomStock = onSelectCustomStock,
                    onLeverageChange = onLeverageChange,
                    onLeverageBlur = onLeverageBlur,
                    onSizingModeChange = onSizingModeChange,
                    onSizingInputChange = onSizingInputChange,
                    onEntryPriceChange = onEntryPriceChange,
                    onTargetPriceChange = onTargetPriceChange,
                    onSlPriceChange = onSlPriceChange,
                    onHoldingDaysSliderChange = onHoldingDaysSliderChange,
                    onHoldingDaysInputChange = onHoldingDaysInputChange,
                    onHoldingDaysBlur = onHoldingDaysBlur,
                    onToggleGstOnInterest = onToggleGstOnInterest,
                    onToggleIncludePledgeDp = onToggleIncludePledgeDp
                )
                RightOutputPanel(
                    state = state,
                    onToggleBreakdown = onToggleBreakdown,
                    onSelectDurationPreset = onSelectDurationPreset
                )
            }
        }
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x1AFFFFFF),
                        Color(0x09FFFFFF),
                        Color(0x10FFFFFF)
                    )
                )
            )
            .border(1.dp, MtfBorder, shape)
            .padding(16.dp)
    ) {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeftInputPanel(
    state: MtfCalculatorState,
    onSearchQueryChange: (String) -> Unit,
    onDropdownVisibilityChange: (Boolean) -> Unit,
    onClearSearch: () -> Unit,
    onSelectStock: (MtfStock) -> Unit,
    onSelectCustomStock: (String) -> Unit,
    onLeverageChange: (String) -> Unit,
    onLeverageBlur: () -> Unit,
    onSizingModeChange: (MtfSizingMode) -> Unit,
    onSizingInputChange: (String) -> Unit,
    onEntryPriceChange: (String) -> Unit,
    onTargetPriceChange: (String) -> Unit,
    onSlPriceChange: (String) -> Unit,
    onHoldingDaysSliderChange: (Float) -> Unit,
    onHoldingDaysInputChange: (String) -> Unit,
    onHoldingDaysBlur: () -> Unit,
    onToggleGstOnInterest: (Boolean) -> Unit,
    onToggleIncludePledgeDp: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val r = state.result

    GlassPanel(modifier = modifier) {
        // 1. Stock Search & Autocomplete Dropdown
        StockSearchSection(
            query = state.searchQuery,
            showDropdown = state.showDropdown,
            onQueryChange = onSearchQueryChange,
            onFocusChanged = onDropdownVisibilityChange,
            onClear = onClearSearch,
            onSelectStock = onSelectStock,
            onSelectCustomStock = onSelectCustomStock
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Stock Banner & Manual Leverage
        StockBanner(
            stock = state.selectedStock,
            leverageInput = state.leverageInput,
            result = r,
            onLeverageChange = onLeverageChange,
            onLeverageBlur = onLeverageBlur
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "POSITION & PRICE LEVELS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MtfSub
        )
        Spacer(modifier = Modifier.height(10.dp))

        // 3. Segmented Sizing Mode
        SegmentedModeSwitch(
            mode = state.sizingMode,
            onModeChange = onSizingModeChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. 2x2 Input Grid
        val isCapitalMode = state.sizingMode == MtfSizingMode.CAPITAL
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FieldCard(
                labelLeft = if (isCapitalMode) "Your Capital" else "Share Qty",
                labelRight = if (isCapitalMode) "Margin" else "Units",
                labelRightColor = MtfMint,
                prefix = if (isCapitalMode) "₹" else "Qty",
                value = state.sizingInput,
                onValueChange = onSizingInputChange,
                subText = if (isCapitalMode) "Upfront capital" else "Shares to buy",
                subColor = MtfMuted,
                keyboardType = if (isCapitalMode) KeyboardType.Decimal else KeyboardType.Number,
                focusBorderColor = MtfMint.copy(alpha = 0.65f),
                testTag = "mtf_sizing_input",
                modifier = Modifier.weight(1f)
            )
            FieldCard(
                labelLeft = "Entry (CMP)",
                labelRight = "Buy",
                labelRightColor = MtfMuted,
                prefix = "₹",
                value = state.entryPriceInput,
                onValueChange = onEntryPriceChange,
                subText = "Shares: ${fmtNum(r.qty.toDouble(), 0)}",
                subColor = MtfMuted,
                keyboardType = KeyboardType.Decimal,
                focusBorderColor = MtfMint.copy(alpha = 0.65f),
                testTag = "mtf_entry_input",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FieldCard(
                labelLeft = "Target Price",
                labelLeftColor = MtfMintLight,
                labelRight = fmtPct(r.targetPctFromEntry),
                labelRightColor = MtfMintLight,
                prefix = "₹",
                value = state.targetPriceInput,
                onValueChange = onTargetPriceChange,
                subText = "${fmtInr(r.targetPerShareDelta, 2, true)} / sh",
                subColor = MtfMintLight,
                keyboardType = KeyboardType.Decimal,
                focusBorderColor = MtfProfit,
                testTag = "mtf_target_input",
                modifier = Modifier.weight(1f)
            )
            FieldCard(
                labelLeft = "Stop-Loss",
                labelLeftColor = MtfLossLight,
                labelRight = fmtPct(r.slPctFromEntry),
                labelRightColor = MtfLossLight,
                prefix = "₹",
                value = state.slPriceInput,
                onValueChange = onSlPriceChange,
                subText = "${fmtInr(r.slPerShareDelta, 2, true)} / sh",
                subColor = MtfLossLight,
                keyboardType = KeyboardType.Decimal,
                focusBorderColor = MtfLoss,
                testTag = "mtf_sl_input",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Compact Holding Period Slider + Editable Days Pill
        HoldingPeriodCard(
            holdingDays = state.holdingDays,
            holdingDaysInput = state.holdingDaysInput,
            onSliderChange = onHoldingDaysSliderChange,
            onDaysInputChange = onHoldingDaysInputChange,
            onDaysBlur = onHoldingDaysBlur
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 6. Toggles Row
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToggleOption(
                checked = state.gstOnInterest,
                onCheckedChange = onToggleGstOnInterest,
                label = "Include 18% GST on MTF Interest",
                tag = "mtf_gst_toggle"
            )
            ToggleOption(
                checked = state.includePledgeDp,
                onCheckedChange = onToggleIncludePledgeDp,
                label = "Include Pledge (₹40) & DP (₹20) + GST",
                tag = "mtf_pledge_toggle"
            )
        }

        // 7. Validation Banner
        if (r.warnings.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x24FBBF24))
                    .border(1.dp, Color(0x59FBBF24), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("mtf_validation_banner")
            ) {
                Text(
                    text = r.warnings.joinToString(" "),
                    fontSize = 12.sp,
                    color = MtfGoldLight,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun StockSearchSection(
    query: String,
    showDropdown: Boolean,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onClear: () -> Unit,
    onSelectStock: (MtfStock) -> Unit,
    onSelectCustomStock: (String) -> Unit
) {
    val matches = remember(query) { MtfCalculator.filterStocks(query) }
    val trimmedUpper = query.trim().uppercase(Locale.ROOT)
    val showCustomOption = trimmedUpper.isNotEmpty() &&
        MtfCalculator.STOCK_DB.none { it.sym == trimmedUpper }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MtfFieldBg)
                .border(
                    width = 1.dp,
                    color = if (showDropdown) MtfMint else Color(0x2EFFFFFF),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search stock",
                tint = MtfMint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search stock (e.g., SAIL, HDFCBANK, Suzlon)...",
                        fontSize = 14.sp,
                        color = MtfMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MtfTxt
                    ),
                    cursorBrush = SolidColor(MtfMint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onFocusChanged(true)
                            }
                        }
                        .testTag("mtf_stock_search")
                )
            }
            if (query.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                        .clickable(onClick = onClear)
                        .testTag("mtf_clear_search"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = MtfSub,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = showDropdown) {
            Column(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .heightIn(max = 250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MtfDropdownBg)
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(6.dp)
                    .testTag("mtf_stock_dropdown")
            ) {
                matches.forEach { stock ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .clickable { onSelectStock(stock) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = stock.sym,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MtfTxt
                            )
                            Text(
                                text = "${stock.name} • ${stock.sector}",
                                fontSize = 11.sp,
                                color = MtfSub,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "~₹${fmtNum(stock.ltp, 0)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = MtfMuted
                            )
                            LeverageTag(
                                text = String.format(Locale.US, "%.2fx", stock.lev),
                                isWarn = false
                            )
                        }
                    }
                }

                if (showCustomOption) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .clickable { onSelectCustomStock(trimmedUpper) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Use \"$trimmedUpper\" as Custom Stock",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MtfTxt
                            )
                            Text(
                                text = "Edit leverage manually below",
                                fontSize = 11.sp,
                                color = MtfSub
                            )
                        }
                        LeverageTag(text = "Custom", isWarn = true)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StockBanner(
    stock: MtfStock,
    leverageInput: String,
    result: MtfCalculationResult,
    onLeverageChange: (String) -> Unit,
    onLeverageBlur: () -> Unit
) {
    var hadFocus by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x1A00D09C), Color(0x0F38BDF8))
                )
            )
            .border(1.dp, Color(0x4700D09C), shape)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stock.sym,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MtfTxt
                    )
                    val (badgeText, badgeWarn) = when {
                        stock.isCustom -> "Custom Stock" to true
                        result.leverage <= 1.01 -> "Delivery Only (1x)" to true
                        else -> "Groww MTF Eligible" to false
                    }
                    LeverageTag(text = badgeText, isWarn = badgeWarn)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stock.name} • ${stock.sector}",
                    fontSize = 12.sp,
                    color = MtfSub,
                    lineHeight = 16.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "MANUAL LEVERAGE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = MtfSub
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xB8060E18))
                        .border(1.dp, Color(0x8000D09C), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = leverageInput,
                        onValueChange = onLeverageChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MtfMint,
                            textAlign = TextAlign.End
                        ),
                        cursorBrush = SolidColor(MtfMint),
                        modifier = Modifier
                            .width(56.dp)
                            .onFocusChanged { focus ->
                                if (focus.isFocused) {
                                    hadFocus = true
                                } else if (hadFocus) {
                                    hadFocus = false
                                    onLeverageBlur()
                                }
                            }
                            .testTag("mtf_leverage_input")
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "x",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MtfMint
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "%.1f%% Margin", result.ownMarginPct),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = MtfSub
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Own vs Groww Funded Progress Bar
        val ownFraction = (result.ownMarginPct / 100.0).toFloat().coerceIn(0.05f, 1f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x14FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(ownFraction)
                    .background(Brush.horizontalGradient(listOf(MtfMint, MtfProfitBright)))
            )
            if (ownFraction < 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f - ownFraction)
                        .background(
                            Brush.horizontalGradient(
                                listOf(MtfCyan.copy(alpha = 0.78f), MtfIndigo.copy(alpha = 0.78f))
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format(Locale.US, "You Pay: %.1f%%", result.ownMarginPct),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = MtfSub
            )
            Text(
                text = String.format(Locale.US, "Groww Funds: %.1f%%", result.growwFundedPct),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = MtfSub
            )
        }
    }
}

@Composable
private fun LeverageTag(text: String, isWarn: Boolean) {
    val bg = if (isWarn) Color(0x29FBBF24) else Color(0x2900D09C)
    val fg = if (isWarn) MtfGoldLight else MtfMintLight
    val border = if (isWarn) Color(0x59FBBF24) else Color(0x5900D09C)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SegmentedModeSwitch(
    mode: MtfSizingMode,
    onModeChange: (MtfSizingMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xA6050B12))
            .border(1.dp, MtfBorderSubtle, RoundedCornerShape(999.dp))
            .padding(4.dp)
    ) {
        SegmentButton(
            label = "By Your Capital (₹)",
            selected = mode == MtfSizingMode.CAPITAL,
            onClick = { onModeChange(MtfSizingMode.CAPITAL) },
            tag = "mtf_mode_capital",
            modifier = Modifier.weight(1f)
        )
        SegmentButton(
            label = "By Share Quantity",
            selected = mode == MtfSizingMode.QUANTITY,
            onClick = { onModeChange(MtfSizingMode.QUANTITY) },
            tag = "mtf_mode_qty",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(999.dp)
    val bgBrush = if (selected) {
        Brush.linearGradient(listOf(Color(0x4700D09C), Color(0x3838BDF8)))
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .background(bgBrush)
            .then(
                if (selected) Modifier.border(1.dp, Color(0x38FFFFFF), shape) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MtfTxt else MtfSub,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FieldCard(
    labelLeft: String,
    labelRight: String,
    labelLeftColor: Color = MtfSub,
    labelRightColor: Color = MtfMuted,
    prefix: String,
    value: String,
    onValueChange: (String) -> Unit,
    subText: String,
    subColor: Color,
    keyboardType: KeyboardType,
    focusBorderColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    val border = if (focused) focusBorderColor else Color(0x21FFFFFF)

    Column(
        modifier = modifier
            .clip(shape)
            .background(MtfFieldBg)
            .border(1.dp, border, shape)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = labelLeft,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelLeftColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 4.dp)
            )
            Text(
                text = labelRight,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelRightColor,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = prefix,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MtfMuted
            )
            Spacer(modifier = Modifier.width(5.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = MtfTxt
                ),
                cursorBrush = SolidColor(MtfMint),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused }
                    .testTag(testTag)
            )
        }

        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = subText,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = subColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HoldingPeriodCard(
    holdingDays: Int,
    holdingDaysInput: String,
    onSliderChange: (Float) -> Unit,
    onDaysInputChange: (String) -> Unit,
    onDaysBlur: () -> Unit
) {
    var hadFocus by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MtfFieldBg)
            .border(1.dp, Color(0x21FFFFFF), shape)
            .padding(horizontal = 13.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Expected Holding Period",
                fontSize = 12.sp,
                color = MtfSub
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0x0FFFFFFF))
                    .border(1.dp, Color(0x5900D09C), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = holdingDaysInput,
                    onValueChange = onDaysInputChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MtfMint,
                        textAlign = TextAlign.End
                    ),
                    cursorBrush = SolidColor(MtfMint),
                    modifier = Modifier
                        .width(38.dp)
                        .onFocusChanged { focus ->
                            if (focus.isFocused) {
                                hadFocus = true
                            } else if (hadFocus) {
                                hadFocus = false
                                onDaysBlur()
                            }
                        }
                        .testTag("mtf_days_input")
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (holdingDays == 1) "Day" else "Days",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MtfSub
                )
            }
        }

        Slider(
            value = holdingDays.coerceIn(1, 180).toFloat(),
            onValueChange = onSliderChange,
            valueRange = 1f..180f,
            colors = SliderDefaults.colors(
                thumbColor = MtfMint,
                activeTrackColor = MtfMint,
                inactiveTrackColor = Color(0x38FFFFFF)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mtf_days_slider")
        )
    }
}

@Composable
private fun ToggleOption(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    tag: String
) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(end = 4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MtfMint,
                uncheckedColor = MtfSub,
                checkmarkColor = Color(0xFF04130E)
            )
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MtfSub
        )
    }
}

@Composable
private fun RightOutputPanel(
    state: MtfCalculatorState,
    onToggleBreakdown: () -> Unit,
    onSelectDurationPreset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val r = state.result

    GlassPanel(modifier = modifier) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BUYING POWER & RISK-REWARD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MtfSub
            )
            Text(
                text = "${fmtNum(r.qty.toDouble(), 0)} Shares @ ${fmtInr(r.entryPrice, 2, false)}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MtfMint
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2x2 Summary Stat Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatGlassTile(
                label = "Total Trade Value",
                value = fmtInr(r.buyValue, 0, false),
                valueColor = MtfTxt,
                sub = String.format(Locale.US, "%.2fx Buying Power", r.leverage),
                modifier = Modifier.weight(1f)
            )
            StatGlassTile(
                label = "Your Upfront Margin",
                value = fmtInr(r.yourMargin, 0, false),
                valueColor = MtfMint,
                sub = String.format(Locale.US, "%.1f%% upfront margin", r.ownMarginPct),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatGlassTile(
                label = "Groww Funded Loan",
                value = fmtInr(r.growwLoan, 0, false),
                valueColor = MtfCyan,
                sub = "${fmtInr(r.targetCharges.dailyInt, 2, false)} / day interest",
                modifier = Modifier.weight(1f)
            )
            StatGlassTile(
                label = "Net Risk : Reward",
                labelColor = MtfGoldLight,
                value = r.netRR?.let { String.format(Locale.US, "1 : %.2f", it) } ?: "—",
                valueColor = MtfGold,
                isGoldTile = true,
                sub = r.grossRR?.let { String.format(Locale.US, "Gross 1 : %.2f", it) } ?: "Gross —",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Price Ladder Card
        PriceLadderCard(result = r)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SCENARIO OUTCOMES (AFTER ALL GROWW MTF CHARGES)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MtfSub
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Scenario Outcomes Grid
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 500.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TargetOutcomeCard(result = r, modifier = Modifier.weight(1f))
                    StopLossOutcomeCard(result = r, modifier = Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TargetOutcomeCard(result = r, modifier = Modifier.fillMaxWidth())
                    StopLossOutcomeCard(result = r, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Collapsible Detailed Groww MTF Cost & Tax Breakdown
        CostBreakdownBox(
            expanded = state.breakdownExpanded,
            result = r,
            onToggle = onToggleBreakdown
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "TARGET NET GAIN BY HOLDING PERIOD",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MtfSub
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Holding Duration Strip
        DurationStrip(
            cards = r.durationCards,
            onSelectDuration = onSelectDurationPreset
        )
    }
}

@Composable
private fun StatGlassTile(
    label: String,
    value: String,
    valueColor: Color,
    sub: String,
    labelColor: Color = MtfSub,
    isGoldTile: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val bg = if (isGoldTile) {
        Brush.linearGradient(listOf(Color(0x1FFBBF24), Color(0x0BFFFFFF)))
    } else {
        Brush.linearGradient(listOf(Color(0x0EFFFFFF), Color(0x0EFFFFFF)))
    }
    val border = if (isGoldTile) Color(0x61FBBF24) else Color(0x24FFFFFF)

    Column(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(horizontal = 13.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = if (isGoldTile) 19.sp else 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = sub,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = MtfMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PriceLadderCard(result: MtfCalculationResult) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(Color(0x1438BDF8), Color(0x12818CF8)))
            )
            .border(1.dp, Color(0x3894A3B8), shape)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Price Ladder",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MtfSub
            )
            val (vBg, vBorder, vFg) = when (result.verdict) {
                RiskRewardVerdict.STRONG -> Triple(Color(0x2E10B981), Color(0x7310B981), MtfMintLight)
                RiskRewardVerdict.MODERATE -> Triple(Color(0x29FBBF24), Color(0x73FBBF24), MtfGoldLight)
                RiskRewardVerdict.POOR,
                RiskRewardVerdict.INVALID -> Triple(Color(0x29FB7185), Color(0x73FB7185), MtfLossLight)
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = vBg,
                border = BorderStroke(1.dp, vBorder)
            ) {
                Text(
                    text = result.verdict.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = vFg,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val riskWeight = (result.ladderRiskBarPct / 100f).coerceIn(0.15f, 0.85f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(riskWeight)
                    .background(Brush.horizontalGradient(listOf(MtfLossDeep, MtfLoss)))
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f - riskWeight)
                    .background(Brush.horizontalGradient(listOf(MtfMint, MtfProfit)))
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val beText = "${fmtInr(result.breakevenPrice, 2, false)} (${
            String.format(Locale.US, "+%.2f%%", result.breakevenPctOverEntry)
        })"

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 480.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LadderNode("Stop-Loss", fmtInr(result.slPrice, 2, false), MtfLoss, Modifier.weight(1f))
                    LadderNode("Entry CMP", fmtInr(result.entryPrice, 2, false), MtfTxt, Modifier.weight(1f))
                    LadderNode("Breakeven", beText, MtfGoldLight, Modifier.weight(1f))
                    LadderNode("Target", fmtInr(result.targetPrice, 2, false), MtfProfitBright, Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LadderNode("Stop-Loss", fmtInr(result.slPrice, 2, false), MtfLoss, Modifier.weight(1f))
                        LadderNode("Entry CMP", fmtInr(result.entryPrice, 2, false), MtfTxt, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LadderNode("Breakeven", beText, MtfGoldLight, Modifier.weight(1f))
                        LadderNode("Target", fmtInr(result.targetPrice, 2, false), MtfProfitBright, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LadderNode(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x73050B12))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 8.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = MtfMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 2
        )
    }
}

@Composable
private fun TargetOutcomeCard(result: MtfCalculationResult, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    val netColor = if (result.netTargetGain >= 0.0) MtfProfitBright else MtfLoss
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0x2B10B981), Color(0xA6060E18))))
            .border(1.dp, Color(0x6B10B981), shape)
            .padding(15.dp)
    ) {
        Text(
            text = "▲ IF TARGET HIT (NET GAIN)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MtfMintLight
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = fmtInr(result.netTargetGain, 0, true),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = netColor,
            modifier = Modifier.testTag("mtf_target_net")
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${fmtPct(result.netTargetRoiPct)} ROI on your ${fmtInr(result.yourMargin, 0, false)} margin",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MtfSub
        )
    }
}

@Composable
private fun StopLossOutcomeCard(result: MtfCalculationResult, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0x2BFB7185), Color(0xA6060E18))))
            .border(1.dp, Color(0x6BFB7185), shape)
            .padding(15.dp)
    ) {
        Text(
            text = "▼ IF STOP-LOSS HIT (NET LOSS)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MtfLossLight
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = fmtInr(result.netSlLoss, 0, true),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MtfLoss,
            modifier = Modifier.testTag("mtf_sl_net")
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${fmtPct(result.netSlRoiPct)} impact on your ${fmtInr(result.yourMargin, 0, false)} margin",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MtfSub
        )

        if (result.showMarginCallWarning) {
            Spacer(modifier = Modifier.height(9.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x2EFB7185))
                    .border(1.dp, Color(0x73FB7185), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "⚠ High Margin Shortfall Risk: Loss exceeds 60% of your upfront margin.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFECDD3),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CostBreakdownBox(
    expanded: Boolean,
    result: MtfCalculationResult,
    onToggle: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val tc = result.targetCharges
    val sc = result.slCharges

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MtfDarkBg)
            .border(1.dp, Color(0x1CFFFFFF), shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .background(Color(0x08FFFFFF))
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("mtf_breakdown_toggle"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Detailed Groww MTF Cost & Tax Breakdown",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MtfTxt
            )
            Text(
                text = if (expanded) "▾" else "▸",
                fontSize = 14.sp,
                color = MtfSub
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                FeeRow("Charge Component", "At Target", "At Stop-Loss", isHeader = true)
                HorizontalDivider(color = Color(0x17FFFFFF))
                FeeRow(
                    "Groww MTF Brokerage (0.1% Buy + 0.1% Sell)",
                    fmtInr(tc.brok, 2, false),
                    fmtInr(sc.brok, 2, false)
                )
                FeeRow(
                    "MTF Interest (14.95% p.a. on Funded Amt)",
                    fmtInr(tc.totalInt, 2, false),
                    fmtInr(sc.totalInt, 2, false)
                )
                FeeRow(
                    "Pledge + Unpledge (₹20 + ₹20) & DP Sell (₹20)",
                    fmtInr(tc.pDp, 2, false),
                    fmtInr(sc.pDp, 2, false)
                )
                FeeRow(
                    "STT (0.1% Buy & Sell) + Stamp Duty (0.015% Buy)",
                    fmtInr(tc.sttStamp, 2, false),
                    fmtInr(sc.sttStamp, 2, false)
                )
                FeeRow(
                    "NSE Txn + SEBI + IPFT + 18% GST",
                    fmtInr(tc.exchGst, 2, false),
                    fmtInr(sc.exchGst, 2, false)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    color = Color(0x2EFFFFFF)
                )
                FeeRow(
                    "Total Charges + Interest",
                    fmtInr(tc.total, 2, false),
                    fmtInr(sc.total, 2, false),
                    isTotal = true
                )
            }
        }
    }
}

@Composable
private fun FeeRow(
    component: String,
    atTarget: String,
    atSl: String,
    isHeader: Boolean = false,
    isTotal: Boolean = false
) {
    val textColor = when {
        isHeader -> MtfMuted
        isTotal -> MtfTxt
        else -> MtfSub
    }
    val weight = if (isHeader || isTotal) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isTotal) 9.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = component,
            fontSize = 11.sp,
            fontWeight = weight,
            color = textColor,
            modifier = Modifier
                .weight(1.55f)
                .padding(end = 6.dp)
        )
        Text(
            text = atTarget,
            fontFamily = if (isHeader) PlusJakartaSansFontFamily else JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = weight,
            color = textColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(0.72f)
        )
        Text(
            text = atSl,
            fontFamily = if (isHeader) PlusJakartaSansFontFamily else JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = weight,
            color = textColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.weight(0.72f)
        )
    }
    if (!isHeader && !isTotal) {
        HorizontalDivider(color = Color(0x0DFFFFFF))
    }
}

@Composable
private fun DurationStrip(
    cards: List<MtfDurationCard>,
    onSelectDuration: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 480.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cards.forEach { card ->
                    DurationCardView(
                        card = card,
                        onClick = { onSelectDuration(card.days) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { card ->
                            DurationCardView(
                                card = card,
                                onClick = { onSelectDuration(card.days) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationCardView(
    card: MtfDurationCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val bg = if (card.isActive) Color(0x2400D09C) else Color(0x0AFFFFFF)
    val border = if (card.isActive) Color(0x7300D09C) else Color(0x17FFFFFF)
    val netColor = if (card.netGain >= 0.0) MtfMintLight else MtfLossLight

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("mtf_dur_${card.days}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${card.days} Days",
            fontSize = 11.sp,
            color = MtfSub
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = fmtInr(card.netGain, 0, true),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = netColor,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Int: ${fmtInr(card.interestCost, 0, false)}",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = MtfMuted,
            maxLines = 1
        )
    }
}

private fun fmtNum(num: Double, decimals: Int): String {
    val safe = if (num.isFinite()) abs(num) else 0.0
    val nf = NumberFormat.getNumberInstance(IN_LOCALE).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
        isGroupingUsed = true
    }
    return nf.format(safe)
}

private fun fmtInr(num: Double, decimals: Int, withSign: Boolean): String {
    val safe = if (num.isFinite()) num else 0.0
    val formatted = fmtNum(safe, decimals)
    return when {
        safe < 0.0 -> "-₹$formatted"
        withSign && safe > 0.0 -> "+₹$formatted"
        else -> "₹$formatted"
    }
}

private fun fmtPct(num: Double): String {
    val safe = if (num.isFinite()) num else 0.0
    val sign = if (safe > 0.0) "+" else ""
    return String.format(Locale.US, "%s%.2f%%", sign, safe)
}
