package ee.vattson.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.vattson.R
import ee.vattson.data.model.CurrencyPreference
import ee.vattson.data.model.ElectricityPrice
import ee.vattson.data.model.PriceResolution
import ee.vattson.ui.theme.bodyFontFamily
import ee.vattson.ui.theme.displayFontFamily
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun PriceCard(
    price: ElectricityPrice?,
    nextPrice: ElectricityPrice?,
    currencyPreference: CurrencyPreference,
    showVat: Boolean,
    onCurrencyChange: (CurrencyPreference) -> Unit,
    onResolutionChange: (PriceResolution) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var resolutionExpanded by rememberSaveable { mutableStateOf(false) }

    val estonianSymbols = remember {
        DecimalFormatSymbols(Locale.forLanguageTag("et-EE")).apply {
            decimalSeparator = ','
        }
    }
    val priceFormatter = remember(estonianSymbols) { DecimalFormat("#,##0.000", estonianSymbols) }
    val percentageFormatter = remember(estonianSymbols) { DecimalFormat("0.0%", estonianSymbols) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val shape = RoundedCornerShape(12.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp)
            .then(
                Modifier
                    .shadow(
                        elevation = 12.dp,
                        shape = shape,
                        spotColor = Color(0xFF2E7D32),
                        ambientColor = Color(0xFF2E7D32)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (isDark) Color(0xFF2E7D32).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        shape = shape
                    )
            ),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                )
        ) {
            val priceValue = remember(price, currencyPreference, showVat) {
                price?.resolvedPrice(currencyPreference, showVat)
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
                val startTimeStr = if (price != null) timeFormatter.format(price.localDateTime) else ""
                val endTimeStr = if (price != null) {
                    val end = if (price.resolution == PriceResolution.HOUR_1) {
                        price.localDateTime.plusHours(1)
                    } else {
                        price.localDateTime.plusMinutes(15)
                    }
                    timeFormatter.format(end)
                } else ""

                Text(
                    text = "Hind hetkel",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                
                if (startTimeStr.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .choiceBoxClickable { resolutionExpanded = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$startTimeStr–$endTimeStr",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_drop_down),
                                contentDescription = "Muuda resolutsiooni",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = resolutionExpanded,
                            onDismissRequest = { resolutionExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            PriceResolution.entries.forEach { resolution ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (resolution == PriceResolution.HOUR_1) "1 tund" else "15 minutit",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    onClick = {
                                        onResolutionChange(resolution)
                                        resolutionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val formattedPriceValue = remember(priceValue, priceFormatter) {
                    priceValue?.let { priceFormatter.format(it) }
                }
                if (formattedPriceValue != null) {
                    AnimatedContent(
                        targetState = formattedPriceValue,
                        transitionSpec = {
                            slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                        },
                        label = "PriceCardValueAnimation"
                    ) { priceText ->
                        Text(
                            text = priceText,
                            fontFamily = displayFontFamily,
                            fontSize = MaterialTheme.typography.displayMedium.fontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                } else {
                    Text(
                        text = "--",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    Row(
                        modifier = Modifier
                            .background(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .choiceBoxClickable { expanded = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currencyPreference.getLabel(priceValue),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_drop_down),
                            contentDescription = "Muuda ühikut",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        CurrencyPreference.entries.forEach { preference ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        preference.defaultLabel,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                },
                                onClick = {
                                    onCurrencyChange(preference)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (nextPrice != null) {
                val nextPriceValue = remember(nextPrice, currencyPreference, showVat) {
                    nextPrice.resolvedPrice(currencyPreference, showVat)
                }

                val diff = remember(priceValue, nextPriceValue) {
                    if (priceValue != null && priceValue != 0.0) {
                        (nextPriceValue - priceValue) / abs(priceValue)
                    } else null
                }

                val trendIconId = "trendIcon"
                val inlineContent = remember(diff) {
                    if (diff != null && abs(diff) > 0.0001) {
                        mapOf(
                            trendIconId to InlineTextContent(
                                Placeholder(
                                    width = 16.sp,
                                    height = 16.sp,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                                )
                            ) {
                                Icon(
                                    imageVector = if (diff > 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (diff > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    } else emptyMap()
                }

                val nextPriceFormatted = remember(nextPriceValue, priceFormatter) {
                    priceFormatter.format(nextPriceValue)
                }
                
                val percentageFormattedText = remember(diff, percentageFormatter) {
                    diff?.let { percentageFormatter.format(abs(it)) }
                }

                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = bodyFontFamily
                            )
                        ) {
                            append("Järgmine hind → ")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = displayFontFamily,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize
                            )
                        ) {
                            append(nextPriceFormatted)
                        }
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.ExtraLight,
                                fontFamily = displayFontFamily,
                                fontSize = MaterialTheme.typography.labelSmall.fontSize
                            )
                        ) {
                            append(currencyPreference.shortLabel)
                        }
                        if (diff != null && abs(diff) > 0.0001 && percentageFormattedText != null) {
                            append("  ")
                            appendInlineContent(trendIconId)
                            withStyle(
                                style = MaterialTheme.typography.titleSmall.toSpanStyle().copy(
                                    fontWeight = FontWeight.Light,
                                    color = if (diff > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                                )
                            ) {
                                append(" $percentageFormattedText")
                            }
                        }
                    },
                    inlineContent = inlineContent,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 12.sp,
                        maxFontSize = MaterialTheme.typography.titleSmall.fontSize
                    )
                )
            }
        }
    }
}
}
