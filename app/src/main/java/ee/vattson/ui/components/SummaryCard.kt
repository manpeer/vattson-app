package ee.vattson.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ee.vattson.data.model.CurrencyPreference
import ee.vattson.ui.theme.displayFontFamily
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val estonianSymbols = DecimalFormatSymbols(Locale.forLanguageTag("et-EE")).apply {
    decimalSeparator = ','
}
private val summaryDecimalFormat = DecimalFormat("#,##0.00", estonianSymbols)

@Composable
fun SummaryItem(
    label: String,
    value: Double?,
    currencyPreference: CurrencyPreference,
    modifier: Modifier = Modifier,
    timePeriod: String? = null
) {
    Column(
        modifier = modifier
            .padding(6.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: Label
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 12.sp,
                maxFontSize = MaterialTheme.typography.titleSmall.fontSize
            )
        )
        Spacer(modifier = Modifier.height(1.dp))

        // Row 2: Value + Unit
        if (value != null) {
            val formattedValue = synchronized(summaryDecimalFormat) {
                summaryDecimalFormat.format(value)
            }
            Text(
                text = buildAnnotatedString {
                    append(formattedValue)
                    withStyle(
                        SpanStyle(
                            fontFamily = displayFontFamily,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.ExtraLight
                        )
                    ) {
                        append(currencyPreference.shortLabel)
                    }
                },
                fontFamily = displayFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = 12.sp,
                    maxFontSize = 19.sp,
                )
            )
        } else {
            Text(
                text = "--",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1
            )
        }

        // Row 3: Time Period
        if (!timePeriod.isNullOrEmpty()) {
            Text(
                text = timePeriod,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                )
        }
    }
}
