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

  override fun saveBatch(candles: List<Candle>) {
    if (candles.isEmpty()) return

    val sql = """
            INSERT INTO candle
            (source, symbol, open, high, low, close, start_time, timestamp)
            VALUES (:source, :symbol, :open, :high, :low, :close, :start_time, :timestamp)
    """.trimIndent()

    val batch = candles.map {
      mapOf(
        "source" to "aggregation-service",
        "symbol" to it.symbol,
        "open" to it.open,
        "high" to it.high,
        "low" to it.low,
        "close" to it.close,
        "start_time" to Timestamp.from(it.time),
        "timestamp" to Timestamp.from(it.time),
      )
    }.toTypedArray()

    jdbc.batchUpdate(sql, batch)
  }

  override fun findRange(symbol: String, interval: CandleInterval, from: Instant, to: Instant): List<Candle> {
    val table = tableName(interval)

    val sql = """
            SELECT symbol AS symbol,
                   start_time AS time,
                   open,
                   high,
                   low,
                   close,
                   0 AS volume
            FROM $table
            WHERE symbol = :symbol
              AND start_time BETWEEN :from AND :to
            ORDER BY start_time ASC
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
        open = rs.getBigDecimal("open"),
        high = rs.getBigDecimal("high"),
        low = rs.getBigDecimal("low"),
        close = rs.getBigDecimal("close"),
        volume = rs.getBigDecimal("volume"),
      )
    }
  }

  override fun findLast(symbol: String, interval: CandleInterval): Candle? {
    val table = tableName(interval)

    val sql = """
            SELECT symbol AS symbol,
                   start_time AS time,
                   open,
                   high,
                   low,
                   close,
                   0 AS volume
            FROM $table
            WHERE symbol = :symbol
            ORDER BY start_time DESC
            LIMIT 1
    """.trimIndent()

    val params = mapOf("symbol" to symbol)

    return jdbc.query(sql, params) { rs, _ ->
      Candle(
        symbol = rs.getString("symbol"),
        interval = interval,
        time = rs.getTimestamp("time").toInstant(),
        open = rs.getBigDecimal("open"),
        high = rs.getBigDecimal("high"),
        low = rs.getBigDecimal("low"),
        close = rs.getBigDecimal("close"),
        volume = rs.getBigDecimal("volume"),
      )
    }.firstOrNull()
  }

  private fun tableName(interval: CandleInterval): String = "candle_info_${interval.postfix}"
}
