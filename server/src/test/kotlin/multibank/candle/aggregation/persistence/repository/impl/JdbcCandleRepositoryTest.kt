package multibank.candle.aggregation.persistence.repository.impl

import multibank.candle.aggregation.helper.DataTestProfile
import multibank.candle.aggregation.persistence.entity.Candle
import multibank.candle.aggregation.persistence.entity.CandleInterval
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class JdbcCandleRepositoryTest : DataTestProfile() {
	private val interval = CandleInterval.ONE_MINUTE

	@Test
	fun `saveBatch should insert candles`() {
		val t1 = Instant.parse("2024-01-01T00:00:00Z")
		val t2 = Instant.parse("2024-01-01T00:01:00Z")

		val candles = listOf(
			Candle("BTCUSDT", interval, t1, 10.0, 11.0, 9.0, 10.5, 100),
			Candle("BTCUSDT", interval, t2, 11.0, 12.0, 10.5, 11.5, 200),
		)

		repository.saveBatch(interval, candles)

		val rows = jdbcTemplate.queryForList(
			"SELECT source, symbol, open, high, low, close, start_time, timestamp FROM candle ORDER BY start_time",
			emptyMap<String, Any>(),
		)

		assertEquals(2, rows.size)

		assertEquals(10.0, (rows[0]["open"] as Number).toDouble())
		assertEquals(11.0, (rows[1]["open"] as Number).toDouble())

	}

	@Test
	fun `findRange should return candles in interval ordered asc`() {
		val base = Instant.parse("2024-01-01T00:00:00Z")
		val candles = (0L..4L).map { offset ->
			Candle(
				symbol = "ETHUSDT",
				interval = interval,
				time = base.plusSeconds(60 * offset),
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
			from = base.plusSeconds(60),
			to = base.plusSeconds(3 * 60),
		)

		assertEquals(3, result.size)
		assertEquals(base.plusSeconds(60), result.first().time)
		assertEquals(base.plusSeconds(180), result.last().time)
	}

	@Test
	fun `findLast should return latest candle or null`() {
		assertNull(repository.findLast("DOGEUSDT", interval))

		val t1 = Instant.parse("2024-01-01T00:00:00Z")
		val t2 = Instant.parse("2024-01-01T00:02:00Z")

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

