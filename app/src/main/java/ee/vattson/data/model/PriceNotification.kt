package ee.vattson.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class PriceNotification(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("threshold") val threshold: Double,
    @SerializedName("type") val type: NotificationType,
    @SerializedName("unit") val unit: CurrencyPreference = CurrencyPreference.SENT_KWH
)

enum class NotificationType {
    ABOVE, BELOW
}