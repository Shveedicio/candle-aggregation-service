package multibank.candle.aggregation.persistence.entity

import java.math.BigDecimal
import java.time.Instant

data class Candle(
  val symbol: String,
  val interval: CandleInterval,
  val time: Instant,
  val open: BigDecimal,
  val high: BigDecimal,
  val low: BigDecimal,
  val close: BigDecimal,
  val volume: BigDecimal,
)
