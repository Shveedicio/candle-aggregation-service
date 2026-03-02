package multibank.candle.aggregation.persistence.entity

enum class CandleInterval(val seconds: Long) {
  ONE_SECOND(1),
  FIVE_SECONDS(5),
  ONE_MINUTE(60),
  FIVE_MINUTES(300),
  FIFTEEN_MINUTES(900),
  ONE_HOUR(3600),
  ;

  companion object {
    fun fromApi(value: String): CandleInterval = when (value) {
      "1s" -> ONE_SECOND
      "5s" -> FIVE_SECONDS
      "1m" -> ONE_MINUTE
      "5m" -> FIVE_MINUTES
      "15m" -> FIFTEEN_MINUTES
      "1h" -> ONE_HOUR
      else -> error("Unsupported interval: $value")
    }
  }
}
