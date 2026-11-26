package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.StockProScreen
import com.example.ui.TradeViewModel
import com.example.ui.theme.StockProTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            StockProTheme {
                val viewModel: TradeViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.onForegroundResumed()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                StockProScreen(
                    uiState = uiState,
                    onSelectTab = viewModel::selectTab,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onRefresh = viewModel::refresh,
                    onMtfSearchQueryChange = viewModel::updateMtfSearchQuery,
                    onMtfDropdownVisibilityChange = viewModel::setMtfDropdownVisible,
                    onMtfClearSearch = viewModel::clearMtfSearch,
                    onMtfSelectStock = { stock -> viewModel.selectMtfStock(stock) },
                    onMtfSelectCustomStock = viewModel::selectCustomMtfStock,
                    onMtfLeverageChange = viewModel::updateMtfLeverage,
                    onMtfLeverageBlur = viewModel::commitMtfLeverageBlur,
                    onMtfSizingModeChange = viewModel::setMtfSizingMode,
                    onMtfSizingInputChange = viewModel::updateMtfSizingInput,
                    onMtfEntryPriceChange = viewModel::updateMtfEntryPrice,
                    onMtfTargetPriceChange = viewModel::updateMtfTargetPrice,
                    onMtfSlPriceChange = viewModel::updateMtfSlPrice,
                    onMtfHoldingDaysSliderChange = viewModel::updateMtfHoldingDaysFromSlider,
                    onMtfHoldingDaysInputChange = viewModel::updateMtfHoldingDaysInput,
                    onMtfHoldingDaysBlur = viewModel::commitMtfHoldingDaysBlur,
                    onMtfToggleGstOnInterest = viewModel::toggleMtfGstOnInterest,
                    onMtfToggleIncludePledgeDp = viewModel::toggleMtfIncludePledgeDp,
                    onMtfToggleBreakdown = viewModel::toggleMtfBreakdownExpanded,
                    onMtfSelectDurationPreset = viewModel::selectMtfDurationPreset
                )
            }
        }
    }
}
