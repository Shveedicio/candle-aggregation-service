package multibank.candle.aggregation.persistence.entity

import java.time.Instant

data class Candle(
  val symbol: String,
  val interval: CandleInterval,
  val time: Instant,
  val open: Double,
  val high: Double,
  val low: Double,
  val close: Double,
  val volume: Long,
)
