package multibank.candle.aggregation.service

import multibank.candle.aggregation.helper.IntegrationalTestProfile
import multibank.candle.aggregation.persistence.entity.Candle
import multibank.candle.aggregation.persistence.entity.CandleInterval
import multibank.candle.aggregation.persistence.repository.CandleRepository
import multibank.kafka.CandleDataOuterClass
import multibank.kafka.CandleDataOuterClass.CandleData
import multibank.kafka.DecimalValueOuterClass.DecimalValue
import multibank.kafka.decimalValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant

class CandleIngestionServiceTest: IntegrationalTestProfile() {

	@Autowired
	private lateinit var candleRepository: CandleRepository

	@Autowired
	private lateinit var service: CandleIngestionService

	@Test
	fun `ingest should convert messages to candles and call saveBatch`() {
		val startTime = 1_710_000_000_000L

		val message = CandleData.newBuilder()
			.setOpen(decimalValue {
				scale = 3
				unscaledValue = 12_345L
			})
			.setClose(decimalValue {
				scale = 3
				unscaledValue = 12_350L
			})
			.setHigh(decimalValue {
				scale = 3
				unscaledValue = 12_400L
			})
			.setLow(decimalValue {
				scale = 3
				unscaledValue = 12_300L
			})
			.setBase("BTC")
			.setQuote("USDT")
			.setStartTime(startTime)
			.build()

		service.ingest(listOf(message))

		val candle = candleRepository.findLast("BTCUSDT", CandleInterval.ONE_MINUTE)

		assertEquals(candle?.symbol, "BTCUSDT")
		assertEquals(candle?.interval, CandleInterval.ONE_MINUTE)
		assertEquals(candle?.time, Instant.ofEpochMilli(startTime))
		assertEquals(candle?.open, message.open.toBigDecimal())
		assertEquals(candle?.high, message.high.toBigDecimal())
		assertEquals(candle?.low, message.low.toBigDecimal())
		assertEquals(candle?.close, message.close.toBigDecimal())
		assertEquals(candle?.volume, BigDecimal.ZERO)
	}
}

