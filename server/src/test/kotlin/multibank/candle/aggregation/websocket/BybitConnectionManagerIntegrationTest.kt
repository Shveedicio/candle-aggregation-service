package multibank.candle.aggregation.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import multibank.candle.aggregation.kafka.producer.CandleDataProducer
import multibank.candle.aggregation.properties.SystemProperties
import multibank.candle.aggregation.service.CandlesGapRecoveryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class BybitConnectionManagerIntegrationTest {

  private val objectMapper = ObjectMapper()
  private val candleDataProducer: CandleDataProducer = mock()
  private val candlesGapRecoveryService: CandlesGapRecoveryService = mock()
  private val subscriptionManager: SubscriptionManager = mock()

  private fun createSystemProperties(symbols: List<String>): SystemProperties {
    return SystemProperties(
      kafka = emptyMap(),
      websockets = SystemProperties.WebsocketsBlock(symbols),
    )
  }

  @Test
  fun `start should connect and subscribe all configured symbols`() {
    val symbols = listOf("BTCUSDT", "ETHUSDT")
    val systemProperties = createSystemProperties(symbols)

    val manager = BybitConnectionManager(
      objectMapper = objectMapper,
      candleDataProducer = candleDataProducer,
      candlesGapRecoveryService = candlesGapRecoveryService,
      subscriptionManager = subscriptionManager,
      systemProperties = systemProperties,
    )

    val clientMock: StandardWebSocketClient = mock()
    val future = CompletableFuture<WebSocketSession>()

    org.mockito.kotlin.whenever(clientMock.execute(any(), any())).thenReturn(future)

    val clientField = BybitConnectionManager::class.java.getDeclaredField("client")
    clientField.isAccessible = true
    clientField.set(manager, clientMock)

    manager.start()

    symbols.forEach {
      verify(subscriptionManager).add(eq(it), eq("1"))
    }

    verify(clientMock).execute(any(), eq("wss://stream.bybit.com/v5/public/spot"))
  }

  @Test
  fun `onConnected should recover gaps and set state LIVE`() {
    val systemProperties = createSystemProperties(emptyList())

    val manager = BybitConnectionManager(
      objectMapper = objectMapper,
      candleDataProducer = candleDataProducer,
      candlesGapRecoveryService = candlesGapRecoveryService,
      subscriptionManager = subscriptionManager,
      systemProperties = systemProperties,
    )

    val schedulerMock: ScheduledExecutorService = mock()

    org.mockito.kotlin.whenever(
      schedulerMock.schedule<Runnable>(any(), any(), any()),
    ).thenReturn(mock<ScheduledFuture<Runnable>>())

    org.mockito.kotlin.whenever(schedulerMock.execute(any())).thenAnswer { invocation ->
      (invocation.arguments[0] as Runnable).run()
      null
    }

    val schedulerField = BybitConnectionManager::class.java.getDeclaredField("scheduler")
    schedulerField.isAccessible = true
    schedulerField.set(manager, schedulerMock)

    val onConnectedMethod = BybitConnectionManager::class.java.getDeclaredMethod("onConnected")
    onConnectedMethod.isAccessible = true
    onConnectedMethod.invoke(manager)

    verify(candlesGapRecoveryService).recover()

    val stateField = BybitConnectionManager::class.java.getDeclaredField("state")
    stateField.isAccessible = true
    val stateRef = stateField.get(manager) as AtomicReference<ConnectionState>
    assertEquals(ConnectionState.LIVE, stateRef.get())

    val backoffField = BybitConnectionManager::class.java.getDeclaredField("backoffSeconds")
    backoffField.isAccessible = true
    assertEquals(1L, backoffField.get(manager) as Long)
  }

  @Test
  fun `scheduleReconnect should set disconnected state and schedule reconnect`() {
    val systemProperties = createSystemProperties(emptyList())

    val manager = BybitConnectionManager(
      objectMapper = objectMapper,
      candleDataProducer = candleDataProducer,
      candlesGapRecoveryService = candlesGapRecoveryService,
      subscriptionManager = subscriptionManager,
      systemProperties = systemProperties,
    )

    val schedulerMock: ScheduledExecutorService = mock()

    org.mockito.kotlin.whenever(
      schedulerMock.schedule<Runnable>(any(), any(), any()),
    ).thenReturn(mock<ScheduledFuture<Runnable>>())

    val schedulerField = BybitConnectionManager::class.java.getDeclaredField("scheduler")
    schedulerField.isAccessible = true
    schedulerField.set(manager, schedulerMock)

    val stateField = BybitConnectionManager::class.java.getDeclaredField("state")
    stateField.isAccessible = true
    val stateRef = stateField.get(manager) as AtomicReference<ConnectionState>
    stateRef.set(ConnectionState.LIVE)

    val scheduleReconnectMethod = BybitConnectionManager::class.java.getDeclaredMethod("scheduleReconnect")
    scheduleReconnectMethod.isAccessible = true
    scheduleReconnectMethod.invoke(manager)

    verify(schedulerMock).schedule(any<Runnable>(), eq(1L), eq(TimeUnit.SECONDS))
    assertEquals(ConnectionState.DISCONNECTED, stateRef.get())

    val backoffField = BybitConnectionManager::class.java.getDeclaredField("backoffSeconds")
    backoffField.isAccessible = true
    assertEquals(2L, backoffField.get(manager) as Long)
  }
}

