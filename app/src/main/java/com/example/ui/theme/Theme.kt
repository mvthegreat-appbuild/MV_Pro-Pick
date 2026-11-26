package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val StockProColorScheme = darkColorScheme(
    primary = TabOnBg,
    onPrimary = TabOnText,
    primaryContainer = BuyBg,
    onPrimaryContainer = BuyText,
    secondary = CmpCyan,
    onSecondary = BgDeep,
    tertiary = AmberPrimary,
    onTertiary = BgDeep,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = Panel,
    onSurface = TextPrimary,
    surfaceVariant = PanelDark,
    onSurfaceVariant = TextMuted,
    outline = LineBorder,
    outlineVariant = CardBorder,
    error = RedPrimary,
    onError = BgDeep,
    errorContainer = SellBg,
    onErrorContainer = SellText
)

private val StockProShapes = Shapes(
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun StockProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StockProColorScheme,
        typography = StockProTypography,
        shapes = StockProShapes,
        content = content
    )
}
