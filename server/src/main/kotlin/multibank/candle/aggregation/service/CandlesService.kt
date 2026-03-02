package multibank.candle.aggregation.service

import multibank.candle.aggregation.api.model.HistoryResponse
import multibank.candle.aggregation.persistence.entity.CandleInterval
import multibank.candle.aggregation.persistence.repository.CandleRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CandlesService(private val candleRepository: CandleRepository) {

  fun getHistory(symbol: String, interval: String, from: Long, to: Long): HistoryResponse {
    val candleInterval = CandleInterval.fromApi(interval)
    val fromInstant = Instant.ofEpochSecond(from)
    val toInstant = Instant.ofEpochSecond(to)

    val candles = candleRepository.findRange(symbol, candleInterval, fromInstant, toInstant)

    return HistoryResponse(
      s = "ok",
      t = candles.map { it.time.epochSecond },
      o = candles.map { it.open },
      h = candles.map { it.high },
      l = candles.map { it.low },
      c = candles.map { it.close },
      v = candles.map { it.volume.toDouble() },
    )
  }
}
