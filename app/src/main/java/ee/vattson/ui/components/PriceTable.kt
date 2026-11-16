package ee.vattson.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ee.vattson.data.model.CurrencyPreference
import ee.vattson.data.model.ElectricityPrice
import ee.vattson.data.model.PriceResolution
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val tableDateFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.forLanguageTag("et-EE"))

@Composable
fun PriceTableHeader(
    currencyPreference: CurrencyPreference,
    showVat: Boolean,
    date: java.time.LocalDate? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (date != null) tableDateFormatter.format(date) else "Aeg",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Hind (${currencyPreference.shortLabel}${if (showVat) " +KM" else ""})",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TableDateHeader(
    date: java.time.LocalDate,
    showPriceHeader: Boolean = false,
    currencyPreference: CurrencyPreference = CurrencyPreference.SENT_KWH,
    showVat: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tableDateFormatter.format(date),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        AnimatedVisibility(
            visible = showPriceHeader,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Hind (${currencyPreference.shortLabel}${if (showVat) " +KM" else ""})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

@Composable
fun PriceTableRow(
    p: ElectricityPrice,
    currencyPreference: CurrencyPreference,
    showVat: Boolean,
    currentPrice: ElectricityPrice?,
    previousPrice: ElectricityPrice?,
    minPrice: Double?,
    maxPrice: Double?,
    isInCheapestWindow: Boolean = false,
    isCheapestPrice: Boolean = false,
    formatter: DecimalFormat,
    timeFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier
) {
    val isCurrent = currentPrice?.timestamp == p.timestamp
    val priceValue = p.resolvedPrice(currencyPreference, showVat)

    val fraction = run {
        if (minPrice != null && maxPrice != null && maxPrice - minPrice > 0.0) {
            ((priceValue - minPrice) / (maxPrice - minPrice)).toFloat().coerceIn(0f, 1f)
        } else {
            0.5f
        }
    }

    val baseBg = MaterialTheme.colorScheme.surface
    val cheapColor = MaterialTheme.colorScheme.tertiaryContainer
    val expensiveColor = MaterialTheme.colorScheme.errorContainer
    val tintedBg = Color(
        red = baseBg.red + (cheapColor.red - baseBg.red) * (1f - fraction) * 0.35f + (expensiveColor.red - baseBg.red) * fraction * 0.35f,
        green = baseBg.green + (cheapColor.green - baseBg.green) * (1f - fraction) * 0.35f + (expensiveColor.green - baseBg.green) * fraction * 0.35f,
        blue = baseBg.blue + (cheapColor.blue - baseBg.blue) * (1f - fraction) * 0.35f + (expensiveColor.blue - baseBg.blue) * fraction * 0.35f,
        alpha = baseBg.alpha
    )

    val backgroundColor = if (isInCheapestWindow) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
    } else {
        tintedBg
    }

    val timeRangeText = remember(p, timeFormatter) {
        val start = p.localDateTime
        val end = if (p.resolution == PriceResolution.HOUR_1) start.plusHours(1) else start.plusMinutes(15)
        "${timeFormatter.format(start)} - ${timeFormatter.format(end)}"
    }
    val formattedPrice = remember(priceValue, formatter) { formatter.format(priceValue) }
    val decimals = if (currencyPreference == CurrencyPreference.SENT_KWH) 3 else 2

    val previousResolved = previousPrice?.resolvedPrice(currencyPreference, showVat)
    val deltaText = if (previousResolved != null) {
        val delta = priceValue - previousResolved
        if (abs(delta) < 0.001) null else String.format(Locale.US, "%+.${decimals}f", delta).replace(".", ",")
    } else {
        null
    }

    val desc = buildString {
        append(timeRangeText)
        append(", ")
        append(formattedPrice)
        if (isCurrent) append(", praegune tund")
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics {
                contentDescription = desc
                selected = isCurrent
                stateDescription = if (isCurrent) "Praegune" else ""
            },
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.fillMaxWidth().background(backgroundColor)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    TimeRangeText(text = timeRangeText, isCurrent = isCurrent)
                    if (isCurrent) {
                        CurrentBadge()
                    }
                    if (isCheapestPrice && !isCurrent) {
                        StarBadge()
                    } else if (isInCheapestWindow && !isCurrent) {
                        EcoBadge()
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    PriceColumn(text = formattedPrice, isCurrent = isCurrent)
                    if (deltaText != null && previousResolved != null) {
                        TrendIndicator(
                            deltaText = deltaText,
                            priceValue = priceValue,
                            prev = previousResolved
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun TimeRangeText(text: String, isCurrent: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
private fun PriceColumn(text: String, isCurrent: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun TrendIndicator(deltaText: String, priceValue: Double, prev: Double) {
    val isUp = priceValue > prev
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (isUp) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            null,
            Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = deltaText,
            style = MaterialTheme.typography.labelSmall,
            color = if (isUp) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun CurrentBadge() {
    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = "Nüüd",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun EcoBadge() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Eco,
            null,
            Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            "Soodsaim periood",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StarBadge() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Star,
            null,
            Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            "Soodsaim hind",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}