package it.unipi.dii.msss.polluted.classifier

enum class AirQuality(val value: Int) {
    GOOD(0),
    MODERATE(1),
    UNHEALTHY_FOR_SENSITIVE_GROUPS(2),
    UNHEALTHY(3),
    VERY_UNHEALTHY(4),
    SEVERE(5);

    companion object {
        infix fun from(value: Int): AirQuality? = AirQuality.values().firstOrNull { it.value == value }
    }
}
