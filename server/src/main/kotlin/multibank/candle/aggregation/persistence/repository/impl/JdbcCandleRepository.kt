package multibank.candle.aggregation.persistence.repository.impl

import multibank.candle.aggregation.persistence.entity.Candle
import multibank.candle.aggregation.persistence.entity.CandleInterval
import multibank.candle.aggregation.persistence.repository.CandleRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.Instant

@Repository
class JdbcCandleRepository(private val jdbc: NamedParameterJdbcTemplate) : CandleRepository {

  override fun saveBatch(interval: CandleInterval, candles: List<Candle>) {
    if (candles.isEmpty()) return

    val table = tableName(interval)

    val sql = """
            INSERT INTO $table
            (symbol, time, open, high, low, close, volume)
            VALUES (:symbol, :time, :open, :high, :low, :close, :volume)
            ON CONFLICT (symbol, time)
            DO UPDATE SET
                open = EXCLUDED.open,
                high = EXCLUDED.high,
                low = EXCLUDED.low,
                close = EXCLUDED.close,
                volume = EXCLUDED.volume
    """.trimIndent()

    val batch = candles.map {
      mapOf(
        "symbol" to it.symbol,
        "time" to Timestamp.from(it.time),
        "open" to it.open,
        "high" to it.high,
        "low" to it.low,
        "close" to it.close,
        "volume" to it.volume,
      )
    }.toTypedArray()

    jdbc.batchUpdate(sql, batch)
  }

  override fun findRange(symbol: String, interval: CandleInterval, from: Instant, to: Instant): List<Candle> {
    val table = tableName(interval)

    val sql = """
            SELECT symbol, time, open, high, low, close, volume
            FROM $table
            WHERE symbol = :symbol
              AND time BETWEEN :from AND :to
            ORDER BY time ASC
    """.trimIndent()

    val params = mapOf(
      "symbol" to symbol,
      "from" to Timestamp.from(from),
      "to" to Timestamp.from(to),
    )

    return jdbc.query(sql, params) { rs, _ ->
      Candle(
        symbol = rs.getString("symbol"),
        interval = interval,
        time = rs.getTimestamp("time").toInstant(),
        open = rs.getDouble("open"),
        high = rs.getDouble("high"),
        low = rs.getDouble("low"),
        close = rs.getDouble("close"),
        volume = rs.getLong("volume"),
      )
    }
  }

  override fun findLast(symbol: String, interval: CandleInterval): Candle? {
    val table = tableName(interval)

    val sql = """
            SELECT symbol, time, open, high, low, close, volume
            FROM $table
            WHERE symbol = :symbol
            ORDER BY time DESC
            LIMIT 1
    """.trimIndent()

    val params = mapOf("symbol" to symbol)

    return jdbc.query(sql, params) { rs, _ ->
      Candle(
        symbol = rs.getString("symbol"),
        interval = interval,
        time = rs.getTimestamp("time").toInstant(),
        open = rs.getDouble("open"),
        high = rs.getDouble("high"),
        low = rs.getDouble("low"),
        close = rs.getDouble("close"),
        volume = rs.getLong("volume"),
      )
    }.firstOrNull()
  }

  private fun tableName(interval: CandleInterval): String = when (interval) {
    CandleInterval.ONE_SECOND -> "candles_1s"
    CandleInterval.FIVE_SECONDS -> "candles_5s"
    CandleInterval.ONE_MINUTE -> "candles_1m"
    CandleInterval.FIVE_MINUTES -> "candles_5m"
    CandleInterval.FIFTEEN_MINUTES -> "candles_15m"
    CandleInterval.ONE_HOUR -> "candles_1h"
  }
}
