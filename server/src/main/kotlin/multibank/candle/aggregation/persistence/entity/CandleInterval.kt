package multibank.candle.aggregation.persistence.entity

enum class CandleInterval(val seconds: Long) {
  ONE_MINUTE(60),
  FIVE_MINUTES(300),
  FIFTEEN_MINUTES(900),
  THIRTY_MINUTES(1800),
  ONE_HOUR(3600),
  ;

  companion object {
    fun fromApi(value: String): CandleInterval = when (value) {
      "1m" -> ONE_MINUTE
      "5m" -> FIVE_MINUTES
      "15m" -> FIFTEEN_MINUTES
      "30m" -> THIRTY_MINUTES
      "1h" -> ONE_HOUR
      else -> error("Unsupported interval: $value")
    }
  }
}
