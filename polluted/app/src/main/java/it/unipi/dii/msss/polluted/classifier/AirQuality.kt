package it.unipi.dii.msss.polluted.classifier

enum class AirQuality(val value: Int) {
    GOOD(0),
    UNHEALTHY(1),
    SEVERE(2);

    companion object {
        infix fun from(value: Int): AirQuality? = AirQuality.values().firstOrNull { it.value == value }
    }
}
