package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.MtfSizingMode
import com.example.domain.MtfStock
import com.example.domain.TradeEvaluator
import com.example.model.FeedTab
import com.example.ui.components.MtfCalculatorContent
import com.example.ui.components.TradeCard
import com.example.ui.theme.BgDeep
import com.example.ui.theme.BgGlow
import com.example.ui.theme.FooterText
import com.example.ui.theme.HeaderBottom
import com.example.ui.theme.HeaderTop
import com.example.ui.theme.LineBorder
import com.example.ui.theme.Panel
import com.example.ui.theme.PanelDark
import com.example.ui.theme.SubHeaderText
import com.example.ui.theme.TabOnBg
import com.example.ui.theme.TabOnBorder
import com.example.ui.theme.TabOnText
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockProScreen(
    uiState: TradeUiState,
    onSelectTab: (FeedTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onMtfSearchQueryChange: (String) -> Unit,
    onMtfDropdownVisibilityChange: (Boolean) -> Unit,
    onMtfClearSearch: () -> Unit,
    onMtfSelectStock: (MtfStock) -> Unit,
    onMtfSelectCustomStock: (String) -> Unit,
    onMtfLeverageChange: (String) -> Unit,
    onMtfLeverageBlur: () -> Unit,
    onMtfSizingModeChange: (MtfSizingMode) -> Unit,
    onMtfSizingInputChange: (String) -> Unit,
    onMtfEntryPriceChange: (String) -> Unit,
    onMtfTargetPriceChange: (String) -> Unit,
    onMtfSlPriceChange: (String) -> Unit,
    onMtfHoldingDaysSliderChange: (Float) -> Unit,
    onMtfHoldingDaysInputChange: (String) -> Unit,
    onMtfHoldingDaysBlur: () -> Unit,
    onMtfToggleGstOnInterest: (Boolean) -> Unit,
    onMtfToggleIncludePledgeDp: (Boolean) -> Unit,
    onMtfToggleBreakdown: () -> Unit,
    onMtfSelectDurationPreset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val isMtfTab = uiState.activeTab == FeedTab.MTF

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDeep)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(BgGlow.copy(alpha = 0.72f), BgDeep.copy(alpha = 0f)),
                        center = Offset(size.width * 0.8f, 0f),
                        radius = size.maxDimension * 0.55f
                    ),
                    radius = size.maxDimension * 0.55f,
                    center = Offset(size.width * 0.8f, 0f)
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderSection(uiState = uiState)

            val listContent: @Composable () -> Unit = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = if (isMtfTab) 1220.dp else 850.dp)
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .testTag("trades_list"),
                        contentPadding = PaddingValues(
                            start = 14.dp,
                            end = 14.dp,
                            top = 14.dp,
                            bottom = 28.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "tabs") {
                            TabBar(
                                activeTab = uiState.activeTab,
                                onSelectTab = onSelectTab
                            )
                        }

                        if (isMtfTab) {
                            item(key = "mtf_calculator") {
                                MtfCalculatorContent(
                                    state = uiState.mtfState,
                                    onSearchQueryChange = onMtfSearchQueryChange,
                                    onDropdownVisibilityChange = onMtfDropdownVisibilityChange,
                                    onClearSearch = onMtfClearSearch,
                                    onSelectStock = onMtfSelectStock,
                                    onSelectCustomStock = onMtfSelectCustomStock,
                                    onLeverageChange = onMtfLeverageChange,
                                    onLeverageBlur = onMtfLeverageBlur,
                                    onSizingModeChange = onMtfSizingModeChange,
                                    onSizingInputChange = onMtfSizingInputChange,
                                    onEntryPriceChange = onMtfEntryPriceChange,
                                    onTargetPriceChange = onMtfTargetPriceChange,
                                    onSlPriceChange = onMtfSlPriceChange,
                                    onHoldingDaysSliderChange = onMtfHoldingDaysSliderChange,
                                    onHoldingDaysInputChange = onMtfHoldingDaysInputChange,
                                    onHoldingDaysBlur = onMtfHoldingDaysBlur,
                                    onToggleGstOnInterest = onMtfToggleGstOnInterest,
                                    onToggleIncludePledgeDp = onMtfToggleIncludePledgeDp,
                                    onToggleBreakdown = onMtfToggleBreakdown,
                                    onSelectDurationPreset = onMtfSelectDurationPreset
                                )
                            }

                            item(key = "mtf_footer") {
                                Text(
                                    text = stringResource(R.string.mtf_disclaimer),
                                    fontSize = 10.sp,
                                    lineHeight = 16.sp,
                                    color = FooterText,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else {
                            item(key = "search") {
                                SearchInput(
                                    query = uiState.searchQuery,
                                    onQueryChange = onSearchQueryChange
                                )
                            }

                            if (uiState.isInitialLoading && uiState.activeSnapshot == null) {
                                item(key = "loading") {
                                    EmptyStateCard(
                                        message = stringResource(R.string.loading_label),
                                        showSpinner = true
                                    )
                                }
                            } else if (uiState.activeSnapshot == null) {
                                item(key = "no_data") {
                                    val msg = if (uiState.activeTab == FeedTab.MOTILAL) {
                                        stringResource(R.string.empty_no_motilal)
                                    } else {
                                        stringResource(R.string.empty_offline_error)
                                    }
                                    EmptyStateCard(message = msg)
                                }
                            } else if (uiState.filteredTrades.isEmpty()) {
                                item(key = "empty_search") {
                                    EmptyStateCard(message = stringResource(R.string.empty_no_trades))
                                }
                            } else {
                                items(
                                    items = uiState.filteredTrades,
                                    key = { "${uiState.activeTab.key}_${it.id}" }
                                ) { trade ->
                                    TradeCard(
                                        trade = trade,
                                        onOpenPost = { url ->
                                            runCatching { uriHandler.openUri(url) }
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }

                            item(key = "refresh_button") {
                                Spacer(modifier = Modifier.height(2.dp))
                                Button(
                                    onClick = onRefresh,
                                    enabled = !uiState.isRefreshing,
                                    shape = RoundedCornerShape(9.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TabOnBg,
                                        contentColor = TabOnText,
                                        disabledContainerColor = TabOnBg.copy(alpha = 0.6f),
                                        disabledContentColor = TabOnText.copy(alpha = 0.7f)
                                    ),
                                    border = BorderStroke(1.dp, TabOnBorder),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("refresh_button")
                                ) {
                                    Text(
                                        text = if (uiState.isRefreshing) {
                                            stringResource(R.string.refreshing_label)
                                        } else {
                                            stringResource(R.string.refresh_button)
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            item(key = "footer") {
                                Text(
                                    text = stringResource(R.string.disclaimer_footer),
                                    fontSize = 10.sp,
                                    lineHeight = 16.sp,
                                    color = FooterText,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isMtfTab) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    listContent()
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    listContent()
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(uiState: TradeUiState) {
    val title = when (uiState.activeTab) {
        FeedTab.STOCKPRO -> stringResource(R.string.title_stockpro)
        FeedTab.MOTILAL -> stringResource(R.string.title_motilal)
        FeedTab.MTF -> stringResource(R.string.title_mtf)
    }
    val subtitle = when (uiState.activeTab) {
        FeedTab.STOCKPRO -> stringResource(R.string.subtitle_stockpro)
        FeedTab.MOTILAL -> stringResource(R.string.subtitle_motilal)
        FeedTab.MTF -> stringResource(R.string.subtitle_mtf)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(colors = listOf(HeaderTop, HeaderBottom))
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 17.dp, vertical = 18.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = SubHeaderText,
            lineHeight = 14.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (uiState.activeTab == FeedTab.MTF) {
            Text(
                text = stringResource(R.string.header_mtf_desc),
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = TextMuted
            )
        } else {
            val snapshot = uiState.activeSnapshot
            if (snapshot != null) {
                val statusPrefix = if (snapshot.isLive) "Refreshed" else "Saved copy (offline)"
                val dataTime = TradeEvaluator.formatUpdatedHeaderTime(snapshot.feed.updated)
                val checkedAt = TradeEvaluator.formatCheckedAtTime(uiState.lastCheckedAtMillis)
                Text(
                    text = buildAnnotatedString {
                        append("$statusPrefix · Data time: ")
                        withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                            append(dataTime)
                        }
                        append("\nApp checked at: ")
                        withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                            append(checkedAt)
                        }
                    },
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = TextMuted
                )
            } else {
                val statusText = when {
                    uiState.isInitialLoading -> stringResource(R.string.loading_label)
                    uiState.activeTab == FeedTab.MOTILAL -> stringResource(R.string.empty_no_motilal)
                    else -> stringResource(R.string.empty_offline_error)
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = TextMuted
                )
            }
        }
    }
    HorizontalDivider(thickness = 1.dp, color = LineBorder)
}

@Composable
private fun TabBar(
    activeTab: FeedTab,
    onSelectTab: (FeedTab) -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useWeightedRow = maxWidth >= 260.dp && fontScale <= 1.35f
        val compactText = maxWidth < 345.dp || fontScale > 1.1f
        val rowModifier = if (useWeightedRow) {
            Modifier.fillMaxWidth()
        } else {
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(if (compactText) 6.dp else 8.dp)
        ) {
            val itemModifier: (FeedTab) -> Modifier = { tab ->
                if (useWeightedRow) {
                    val weight = when (tab) {
                        FeedTab.STOCKPRO -> 0.92f
                        FeedTab.MOTILAL -> 1.15f
                        FeedTab.MTF -> 1.03f
                    }
                    Modifier.weight(weight)
                } else {
                    Modifier.widthIn(min = 104.dp)
                }.testTag("tab_${tab.key}")
            }
            TabButton(
                label = stringResource(R.string.tab_stockpro),
                selected = activeTab == FeedTab.STOCKPRO,
                compact = compactText,
                onClick = { onSelectTab(FeedTab.STOCKPRO) },
                modifier = itemModifier(FeedTab.STOCKPRO)
            )
            TabButton(
                label = stringResource(R.string.tab_motilal),
                selected = activeTab == FeedTab.MOTILAL,
                compact = compactText,
                onClick = { onSelectTab(FeedTab.MOTILAL) },
                modifier = itemModifier(FeedTab.MOTILAL)
            )
            TabButton(
                label = stringResource(R.string.tab_mtf),
                selected = activeTab == FeedTab.MTF,
                compact = compactText,
                onClick = { onSelectTab(FeedTab.MTF) },
                modifier = itemModifier(FeedTab.MTF)
            )
        }
    }
}

@Composable
private fun TabButton(
    label: String,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(11.dp)
    val bg = if (selected) TabOnBg else PanelDark
    val border = if (selected) TabOnBorder else LineBorder
    val fg = if (selected) TabOnText else TextMuted

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 4.dp else 8.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = if (compact) 11.sp else 12.sp
            ),
            color = fg,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(PanelDark)
            .border(1.dp, LineBorder, shape)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(R.string.search_placeholder),
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                cursorBrush = SolidColor(TabOnText),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input")
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.testTag("clear_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.clear_search),
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    message: String,
    showSpinner: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Panel,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showSpinner) {
                CircularProgressIndicator(
                    color = TabOnText,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
