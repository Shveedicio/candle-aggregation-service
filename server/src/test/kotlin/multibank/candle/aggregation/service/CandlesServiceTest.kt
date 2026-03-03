package multibank.candle.aggregation.service

import multibank.candle.aggregation.persistence.entity.Candle
import multibank.candle.aggregation.persistence.entity.CandleInterval
import multibank.candle.aggregation.persistence.repository.CandleRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import kotlin.toBigDecimal

class CandlesServiceTest {

	private val candleRepository: CandleRepository = mock()
	private val candlesService = CandlesService(candleRepository)

	@Test
	fun `getHistory returns HistoryResponse with candles from repository`() {
		val symbol = "BTCUSDT"
		val interval = "1m"
		val from = 1620000000L
		val to = 1620000060L

		val candles = listOf(
			Candle(
				symbol,
				CandleInterval.ONE_MINUTE,
				Instant.ofEpochSecond(1620000000),
				29500.5.toBigDecimal(),
				29510.toBigDecimal(),
				29490.toBigDecimal(),
				29505.toBigDecimal(),
				10.toBigDecimal()
			),
			Candle(
				symbol,
				CandleInterval.ONE_MINUTE,
				Instant.ofEpochSecond(1620000060),
				29501.toBigDecimal(),
				29505.toBigDecimal(),
				29500.toBigDecimal(),
				29502.toBigDecimal(),
				8.toBigDecimal()
			),
		)

		whenever(
			candleRepository.findRange(
				eq(symbol),
				eq(CandleInterval.ONE_MINUTE),
				eq(Instant.ofEpochSecond(from)),
				eq(Instant.ofEpochSecond(to)),
			),
		).thenReturn(candles)

		val result = candlesService.getHistory(symbol, interval, from, to)

		assertEquals(symbol, result.s)
		assertEquals(listOf(1620000000L, 1620000060L), result.t)
		assertEquals(listOf(29500.5.toBigDecimal(), 29501.toBigDecimal()), result.o)
		assertEquals(listOf(29510.toBigDecimal(), 29505.toBigDecimal()), result.h)
		assertEquals(listOf(29490.toBigDecimal(), 29500.toBigDecimal()), result.l)
		assertEquals(listOf(29505.toBigDecimal(), 29502.toBigDecimal()), result.c)
		assertEquals(listOf(10.toBigDecimal(), 8.toBigDecimal()), result.v)

		verify(candleRepository).findRange(
			eq(symbol),
			eq(CandleInterval.ONE_MINUTE),
			eq(Instant.ofEpochSecond(from)),
			eq(Instant.ofEpochSecond(to)),
		)
	}

	@Test
	fun `getHistory returns empty arrays when repository returns empty list`() {
		whenever(candleRepository.findRange(any(), any(), any(), any())).thenReturn(emptyList())

		val result = candlesService.getHistory("ETHUSDT", "5m", 1620000000L, 1620000300L)

		assertEquals("ETHUSDT", result.s)
		assertEquals(emptyList<Long>(), result.t)
		assertEquals(emptyList<BigDecimal>(), result.o)
		assertEquals(emptyList<BigDecimal>(), result.h)
		assertEquals(emptyList<BigDecimal>(), result.l)
		assertEquals(emptyList<BigDecimal>(), result.c)
		assertEquals(emptyList<BigDecimal>(), result.v)
	}

	@Test
	fun `getHistory maps interval to correct CandleInterval`() {
		whenever(candleRepository.findRange(any(), any(), any(), any())).thenReturn(emptyList())

		candlesService.getHistory("BTC-USD", "1m", 0L, 1L)
		verify(candleRepository).findRange(any(), eq(CandleInterval.ONE_MINUTE), any(), any())

		candlesService.getHistory("BTC-USD", "5m", 0L, 1L)
		verify(candleRepository).findRange(any(), eq(CandleInterval.FIVE_MINUTES), any(), any())

		candlesService.getHistory("BTC-USD", "15m", 0L, 1L)
		verify(candleRepository).findRange(any(), eq(CandleInterval.FIFTEEN_MINUTES), any(), any())

		candlesService.getHistory("BTC-USD", "30m", 0L, 1L)
		verify(candleRepository).findRange(any(), eq(CandleInterval.THIRTY_MINUTES), any(), any())

		candlesService.getHistory("BTC-USD", "1h", 0L, 1L)
		verify(candleRepository).findRange(any(), eq(CandleInterval.ONE_HOUR), any(), any())
	}

	@Test
	fun `getHistory throws for unsupported interval`() {
		assertThrows(IllegalStateException::class.java) {
			candlesService.getHistory("BTC-USD", "invalid", 0L, 1L)
		}
	}
}
