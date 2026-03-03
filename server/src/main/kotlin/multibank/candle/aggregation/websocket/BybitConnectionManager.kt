package multibank.candle.aggregation.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import multibank.candle.aggregation.kafka.producer.CandleDataProducer
import multibank.candle.aggregation.service.CandlesGapRecoveryService
import org.springframework.stereotype.Component
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

private val log = KotlinLogging.logger {}

@Component
class BybitConnectionManager(
  private val objectMapper: ObjectMapper,
  private val candleDataProducer: CandleDataProducer,
  private val candlesGapRecoveryService: CandlesGapRecoveryService,
) {
  private val client = StandardWebSocketClient()
  private val uri = "wss://stream.bybit.com/v5/public/spot"

  private val scheduler = Executors.newSingleThreadScheduledExecutor()

  private val state = AtomicReference(ConnectionState.DISCONNECTED)

  private var backoffSeconds = 1L

  fun start() {
    connect()
  }

  private fun connect() {
    val handler = BybitWebSocketHandler(
      objectMapper = objectMapper,
      stateRef = state,
      onConnected = { onConnected() },
      onDisconnected = { scheduleReconnect() },
      producer = candleDataProducer,
    )

    log.info { "Connecting to Bybit..." }

    client.execute(handler, uri)
      .whenComplete { _, throwable ->
        if (throwable != null) {
          log.info { "Handshake failed: ${throwable.message}" }
          scheduleReconnect()
        }
      }
  }

  private fun onConnected() {
    scheduler.execute {
      try {
        log.info { "Recovering gaps..." }

        state.set(ConnectionState.RECOVERING)

        // Not implemented
        candlesGapRecoveryService.recover()

        state.set(ConnectionState.LIVE)
        backoffSeconds = 1

        log.info { "Connection is LIVE" }
      } catch (ex: Exception) {
        log.error(ex) { "Recovery failed: ${ex.message}" }
        scheduleReconnect()
      }
    }
  }

  private fun scheduleReconnect() {
    if (state.get() == ConnectionState.DISCONNECTED) return

    state.set(ConnectionState.DISCONNECTED)

    val delay = backoffSeconds.coerceAtMost(30)

    log.info { "Reconnecting in $delay seconds" }

    scheduler.schedule({ connect() }, delay, TimeUnit.SECONDS)

    backoffSeconds = (backoffSeconds * 2).coerceAtMost(30)
  }
}
