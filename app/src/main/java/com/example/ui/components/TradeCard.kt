package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.EvaluatedTrade
import com.example.model.TradeStatus
import com.example.ui.theme.ActiveBg
import com.example.ui.theme.ActiveBorder
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.BoxBg
import com.example.ui.theme.BoxBorder
import com.example.ui.theme.BuyBg
import com.example.ui.theme.BuyText
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardBottom
import com.example.ui.theme.CardTop
import com.example.ui.theme.ChipBg
import com.example.ui.theme.ChipBorder
import com.example.ui.theme.ChipDoneBg
import com.example.ui.theme.CmpBoxBorder
import com.example.ui.theme.CmpCyan
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LineBorder
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.SellBg
import com.example.ui.theme.SellText
import com.example.ui.theme.SlBorder
import com.example.ui.theme.TargetBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WaitingBg

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TradeCard(
    trade: EvaluatedTrade,
    onOpenPost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(CardTop, CardBottom)
                )
            )
            .border(1.dp, CardBorder, cardShape)
            .padding(14.dp)
            .testTag("trade_card_${trade.id}")
    ) {
        // Top row: Name/meta + Status/Side badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = trade.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (trade.subtitleMeta.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = trade.subtitleMeta,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusTag(status = trade.status, label = trade.statusLabel)
                SideTag(side = trade.side, isSell = trade.isSell)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3-column Price Box Row: Entry | CMP | Stop Loss
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PriceBox(
                label = trade.entryLabel,
                value = trade.entryPriceFormatted,
                valueColor = TextPrimary,
                borderColor = BoxBorder,
                modifier = Modifier.weight(1f)
            )
            PriceBox(
                label = "CMP (${trade.cmpSymbolLabel})",
                value = trade.cmpFormatted ?: stringResource(R.string.unavailable_label),
                valueColor = if (trade.cmpFormatted != null) CmpCyan else TextMuted,
                borderColor = CmpBoxBorder,
                modifier = Modifier.weight(1f)
            )
            PriceBox(
                label = stringResource(R.string.stop_loss_label),
                value = trade.slFormatted,
                valueColor = RedPrimary,
                borderColor = BoxBorder,
                modifier = Modifier.weight(1f)
            )
        }

        // Target Chips
        if (trade.targetsFormatted.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                trade.targetsFormatted.forEachIndexed { index, formattedTarget ->
                    val isDone = index < trade.hitsCount
                    TargetChip(
                        index = index + 1,
                        formattedPrice = formattedTarget,
                        isDone = isDone
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Metrics Row
        val moveColor = when {
            trade.movePct == null -> TextMuted
            trade.movePct >= 0.0 -> GreenPrimary
            else -> RedPrimary
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MetricLabel("Move", trade.moveFormatted, moveColor)
            MetricLabel("Risk", trade.riskPctFormatted, RedPrimary)
            MetricLabel("To T1", trade.toT1PctFormatted, GreenPrimary)
            MetricLabel("To last", trade.toLastPctFormatted, GreenPrimary)
            MetricLabel("R:R T1", trade.rrT1Formatted, TextPrimary)
            MetricLabel("R:R last", trade.rrLastFormatted, TextPrimary)
        }

        // Optional notes, warnings & link
        if (!trade.idea.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = trade.idea,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                lineHeight = 16.sp
            )
        }

        if (trade.hasPointsTargetNote) {
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = stringResource(R.string.points_note),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                lineHeight = 16.sp
            )
        }

        if (trade.hasLevelWarning) {
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = stringResource(R.string.levels_warning),
                style = MaterialTheme.typography.bodySmall,
                color = AmberPrimary,
                lineHeight = 16.sp
            )
        }

        if (!trade.link.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenPost(trade.link) }
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 2.dp, vertical = 12.dp)
                    .testTag("open_post_${trade.id}"),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.open_original_post),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = CmpCyan
                    )
                )
            }
        }
    }
}

@Composable
private fun StatusTag(status: TradeStatus, label: String) {
    val (bg, fg, border) = when (status) {
        TradeStatus.TARGET -> Triple(BuyBg, GreenPrimary, TargetBorder)
        TradeStatus.SL -> Triple(SellBg, RedPrimary, SlBorder)
        TradeStatus.ACTIVE -> Triple(ActiveBg, AmberPrimary, ActiveBorder)
        TradeStatus.WAITING,
        TradeStatus.MISSED,
        TradeStatus.UNKNOWN -> Triple(WaitingBg, TextMuted, LineBorder)
    }
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun SideTag(side: String, isSell: Boolean) {
    val bg = if (isSell) SellBg else BuyBg
    val fg = if (isSell) SellText else BuyText
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = bg
    ) {
        Text(
            text = side,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun PriceBox(
    label: String,
    value: String,
    valueColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val boxShape = RoundedCornerShape(11.dp)
    Column(
        modifier = modifier
            .clip(boxShape)
            .background(BoxBg)
            .border(1.dp, borderColor, boxShape)
            .padding(horizontal = 9.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TargetChip(
    index: Int,
    formattedPrice: String,
    isDone: Boolean
) {
    val bg = if (isDone) ChipDoneBg else ChipBg
    val border = if (isDone) GreenPrimary else ChipBorder
    val fg = if (isDone) Color.White else GreenPrimary
    val prefix = if (isDone) "✓ " else ""

    Surface(
        shape = RoundedCornerShape(9.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = "${prefix}T$index $formattedPrice",
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = fg,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun MetricLabel(
    name: String,
    value: String,
    valueColor: Color
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = TextMuted, fontWeight = FontWeight.Normal)) {
                append(name)
                append(" ")
            }
            withStyle(
                SpanStyle(
                    color = valueColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            ) {
                append(value)
            }
        },
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
}
