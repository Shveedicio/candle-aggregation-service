package multibank.candle.aggregation.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import multibank.candle.aggregation.kafka.producer.CandleDataProducer
import multibank.candle.aggregation.websocket.model.BybitKline
import multibank.candle.aggregation.websocket.model.BybitKlineMessage
import multibank.kafka.CandleDataOuterClass
import multibank.kafka.DecimalValueOuterClass
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference

private val log = KotlinLogging.logger {}

class BybitWebSocketHandler(
  private val objectMapper: ObjectMapper,
  private val stateRef: AtomicReference<ConnectionState>,
  private val onConnected: () -> Unit,
  private val onDisconnected: () -> Unit,
  private val producer: CandleDataProducer,
) : TextWebSocketHandler() {

  override fun afterConnectionEstablished(session: WebSocketSession) {
    log.info { "WebSocket connected" }

    val subscribe = """
            {
              "op": "subscribe",
              "args": ["kline.1.BTCUSDT"]
            }
    """.trimIndent()

    session.sendMessage(TextMessage(subscribe))
    onConnected()
  }

  override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
    if (stateRef.get() != ConnectionState.LIVE) return

    log.info { "KLine Message: ${message.payload}" }

    val parsed = objectMapper.readValue(
      message.payload,
      BybitKlineMessage::class.java,
    )

    val symbol = parsed.topic?.split(".")?.last() ?: return

    parsed.data.forEach {
      producer.produce(
        mapToCandleData(it, symbol),
      )
    }
  }

  override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
    log.info { "Transport error: ${exception.message}" }
    onDisconnected()
  }

  override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
    log.info { "WebSocket closed: ${status.reason}" }
    onDisconnected()
  }

  private fun mapToCandleData(candle: BybitKline, symbol: String) = CandleDataOuterClass.CandleData.newBuilder()
    .setSymbol(symbol)
    .setOpen(candle.open.toDecimalValue())
    .setHigh(candle.high.toDecimalValue())
    .setClose(candle.close.toDecimalValue())
    .setLow(candle.low.toDecimalValue())
    .setStartTime(candle.start)
    .setTimestamp(candle.timestamp)
    .setVolume(candle.volume.toDecimalValue())
    .build()

  private fun BigDecimal.toDecimalValue() = DecimalValueOuterClass.DecimalValue.newBuilder()
    .setUnscaledValue(this.unscaledValue().toLong())
    .setScale(this.scale())
    .build()
}
