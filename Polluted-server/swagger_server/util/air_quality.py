from enum import Enum

class AirQuality(Enum):
    GOOD = 0
    MODERATE = 1
    UNHEALTHY_FOR_SENSITIVE_GROUPS = 2
    UNHEALTHY = 3
    VERY_UNHEALTHY = 4
    SEVERE = 5