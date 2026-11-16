package ee.vattson.data.model

import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class EleringPriceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: EleringData
)

data class EleringData(
    @SerializedName("ee") val ee: List<EleringPriceItem> = emptyList(),
    @SerializedName("fi") val fi: List<EleringPriceItem> = emptyList(),
    @SerializedName("lv") val lv: List<EleringPriceItem> = emptyList(),
    @SerializedName("lt") val lt: List<EleringPriceItem> = emptyList()
)

data class EleringPriceItem(
    @SerializedName("timestamp") val timestamp: Long, // Epoch seconds
    @SerializedName("price") val price: Double // €/MWh (VAT excluded)
)

enum class PriceResolution {
    MINUTE_15,
    HOUR_1
}

data class ElectricityPrice(
    val timestamp: Instant,
    val resolution: PriceResolution,
    val priceEurMwh: Double,
    val priceWithVatEurMwh: Double,
    val priceCentsKwh: Double,
    val priceWithVatCentsKwh: Double
) {
    val localDateTime: LocalDateTime
        get() = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())

    fun resolvedPrice(currencyPreference: CurrencyPreference, showVat: Boolean): Double =
        if (currencyPreference == CurrencyPreference.SENT_KWH) {
            if (showVat) priceWithVatCentsKwh else priceCentsKwh
        } else {
            if (showVat) priceWithVatEurMwh else priceEurMwh
        }

    companion object {
        private const val VAT_RATE = 1.24

        fun fromEleringItem(item: EleringPriceItem, resolution: PriceResolution): ElectricityPrice {
            val priceEurMwh = item.price
            val priceWithVatEurMwh = priceEurMwh * VAT_RATE
            val priceCentsKwh = priceEurMwh / 10.0
            val priceWithVatCentsKwh = priceCentsKwh * VAT_RATE

            return ElectricityPrice(
                timestamp = Instant.ofEpochSecond(item.timestamp),
                resolution = resolution,
                priceEurMwh = priceEurMwh,
                priceWithVatEurMwh = priceWithVatEurMwh,
                priceCentsKwh = priceCentsKwh,
                priceWithVatCentsKwh = priceWithVatCentsKwh
            )
        }
    }
}
