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
			Candle(
				"BTCUSDT",
				interval,
				t1,
				10.0.toBigDecimal(),
				11.0.toBigDecimal(),
				9.0.toBigDecimal(),
				10.5.toBigDecimal(),
				100.toBigDecimal()
			),
			Candle(
				"BTCUSDT",
				interval,
				t2,
				11.0.toBigDecimal(),
				12.0.toBigDecimal(),
				10.5.toBigDecimal(),
				11.5.toBigDecimal(),
				200.toBigDecimal()
			),
		)

		repository.saveBatch(candles)

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
				open = (100.0 + offset).toBigDecimal(),
				high = (101.0 + offset).toBigDecimal(),
				low = (99.0 + offset).toBigDecimal(),
				close = (100.5 + offset).toBigDecimal(),
				volume = (10L * (offset + 1)).toBigDecimal(),
			)
		}

		repository.saveBatch(candles)

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
			listOf(
				Candle(
					"DOGEUSDT",
					interval,
					t1,
					1.0.toBigDecimal(),
					1.1.toBigDecimal(),
					0.9.toBigDecimal(),
					1.05.toBigDecimal(),
					100.toBigDecimal()
				),
				Candle(
					"DOGEUSDT",
					interval,
					t2,
					2.0.toBigDecimal(),
					2.1.toBigDecimal(),
					1.9.toBigDecimal(),
					2.05.toBigDecimal(),
					200.toBigDecimal()
				),
			),
		)

		val last = repository.findLast("DOGEUSDT", interval)

		assertEquals(t2, last?.time)
		assertEquals(2.0.toBigDecimal(), last?.open)
	}
}

