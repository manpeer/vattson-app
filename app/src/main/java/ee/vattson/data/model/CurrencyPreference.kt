package ee.vattson.data.model

enum class CurrencyPreference {
    SENT_KWH,
    EURO_MWH;

    fun getLabel(value: Double?): String {
        val isSingular = value == 1.0
        return when (this) {
            SENT_KWH -> if (isSingular) "Sent/kWh" else "Senti/kWh"
            EURO_MWH -> if (isSingular) "Euro/MWh" else "Eurot/MWh"
        }
    }

    val defaultLabel: String
        get() = when (this) {
            SENT_KWH -> "Senti/kWh"
            EURO_MWH -> "Eurot/MWh"
        }

    val shortLabel: String
        get() = when (this) {
            SENT_KWH -> "s/kWh"
            EURO_MWH -> "€/MWh"
        }
}
