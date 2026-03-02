package multibank.candle.aggregation.persistence.repository

import multibank.candle.aggregation.persistence.entity.Candle
import multibank.candle.aggregation.persistence.entity.CandleInterval
import java.time.Instant

interface CandleRepository {

  fun saveBatch(interval: CandleInterval, candles: List<Candle>)

  fun findRange(symbol: String, interval: CandleInterval, from: Instant, to: Instant): List<Candle>

  fun findLast(symbol: String, interval: CandleInterval): Candle?
}
