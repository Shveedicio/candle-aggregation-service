package multibank.candle.aggregation.websocket.model

import java.math.BigDecimal

data class BybitKlineMessage(val topic: String? = null, val data: List<BybitKline> = emptyList())

data class BybitKline(
  val start: Long,
  val end: Long,
  val open: BigDecimal,
  val close: BigDecimal,
  val high: BigDecimal,
  val low: BigDecimal,
  val volume: BigDecimal,
  val confirm: Boolean,
  val timestamp: Long,
)
