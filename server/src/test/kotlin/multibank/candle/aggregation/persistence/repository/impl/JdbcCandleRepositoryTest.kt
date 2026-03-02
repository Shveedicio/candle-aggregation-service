package multibank.candle.aggregation.persistence.repository.impl

import multibank.candle.aggregation.helper.DataTestProfile
import multibank.candle.aggregation.persistence.entity.Candle
import multibank.candle.aggregation.persistence.entity.CandleInterval
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class JdbcCandleRepositoryTest : DataTestProfile() {
	private val interval = CandleInterval.ONE_SECOND

	@Test
	fun `saveBatch should insert and update candles`() {
		val t1 = Instant.parse("2024-01-01T00:00:00Z")
		val t2 = Instant.parse("2024-01-01T00:00:01Z")

		val candles = listOf(
			Candle("BTCUSDT", interval, t1, 10.0, 11.0, 9.0, 10.5, 100),
			Candle("BTCUSDT", interval, t2, 11.0, 12.0, 10.5, 11.5, 200),
		)

		repository.saveBatch(interval, candles)

		// update same times with new data
		val updated = listOf(
			Candle("BTCUSDT", interval, t1, 20.0, 21.0, 19.0, 20.5, 300),
			Candle("BTCUSDT", interval, t2, 21.0, 22.0, 20.5, 21.5, 400),
		)

		repository.saveBatch(interval, updated)

		val rows = jdbcTemplate.queryForList(
			"SELECT symbol, time, open, high, low, close, volume FROM candles_1s ORDER BY time",
			emptyMap<String, Any>(),
		)

		assertEquals(2, rows.size)
		assertEquals(20.0, (rows[0]["open"] as Number).toDouble())
		assertEquals(21.5, (rows[1]["close"] as Number).toDouble())
	}

	@Test
	fun `findRange should return candles in interval ordered asc`() {
		val base = Instant.parse("2024-01-01T00:00:00Z")
		val candles = (0L..4L).map { offset ->
			Candle(
				symbol = "ETHUSDT",
				interval = interval,
				time = base.plusSeconds(offset),
				open = 100.0 + offset,
				high = 101.0 + offset,
				low = 99.0 + offset,
				close = 100.5 + offset,
				volume = 10L * (offset + 1),
			)
		}

		repository.saveBatch(interval, candles)

		val result = repository.findRange(
			symbol = "ETHUSDT",
			interval = interval,
			from = base.plusSeconds(1),
			to = base.plusSeconds(3),
		)

		assertEquals(3, result.size)
		assertEquals(base.plusSeconds(1), result.first().time)
		assertEquals(base.plusSeconds(3), result.last().time)
	}

	@Test
	fun `findLast should return latest candle or null`() {
		assertNull(repository.findLast("DOGEUSDT", interval))

		val t1 = Instant.parse("2024-01-01T00:00:00Z")
		val t2 = Instant.parse("2024-01-01T00:00:02Z")

		repository.saveBatch(
			interval,
			listOf(
				Candle("DOGEUSDT", interval, t1, 1.0, 1.1, 0.9, 1.05, 100),
				Candle("DOGEUSDT", interval, t2, 2.0, 2.1, 1.9, 2.05, 200),
			),
		)

		val last = repository.findLast("DOGEUSDT", interval)

		assertEquals(t2, last?.time)
		assertEquals(2.0, last?.open)
	}
}

