package multibank.candle.aggregation.websocket

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class SubscriptionManager {

  private val subscriptions = ConcurrentHashMap.newKeySet<String>()

  fun add(symbol: String, interval: String) {
    subscriptions.add("kline.$interval.$symbol")
  }

  fun remove(symbol: String, interval: String) {
    subscriptions.remove("kline.$interval.$symbol")
  }

  fun all(): Set<String> = subscriptions.toSet()
}
