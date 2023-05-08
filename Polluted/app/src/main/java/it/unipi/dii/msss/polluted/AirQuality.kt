package it.unipi.dii.msss.polluted

enum class AirQuality(val value: Int) {
    GOOD(0),
    MODERATE(1),
    UNHEALTHY_FOR_SENSITIVE_GROUPS(4),
    UNHEALTHY(3),
    VERY_UNHEALTHY(5),
    SEVERE(2)
}
