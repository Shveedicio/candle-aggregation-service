package multibank.candle.aggregation.service

import multibank.candle.aggregation.persistence.entity.Candle
import multibank.candle.aggregation.persistence.entity.CandleInterval
import multibank.candle.aggregation.persistence.repository.CandleRepository
import multibank.kafka.CandleDataOuterClass.CandleData
import multibank.kafka.DecimalValueOuterClass
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

@Service
class CandleIngestionService(private val candleRepository: CandleRepository) {

  fun ingest(messages: List<CandleData>) {
    if (messages.isEmpty()) return

    val interval = CandleInterval.ONE_MINUTE

    val candles = messages.map { message ->
      Candle(
        symbol = message.base + message.quote,
        interval = interval,
        time = Instant.ofEpochMilli(message.startTime),
        open = message.open.toBigDecimal(),
        high = message.high.toBigDecimal(),
        low = message.low.toBigDecimal(),
        close = message.close.toBigDecimal(),
        volume = BigDecimal.ZERO,
      )
    }

    candleRepository.saveBatch(candles)
  }
}

fun DecimalValueOuterClass.DecimalValue.toBigDecimal(): BigDecimal = BigDecimal(
  BigInteger.valueOf(this.unscaledValue),
  this.scale,
)
