package multibank.candle.aggregation.listeners

import multibank.candle.aggregation.websocket.BybitConnectionManager
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class StartupListener(private val manager: BybitConnectionManager) {

  @EventListener(ApplicationReadyEvent::class)
  fun onStartup() {
    manager.start()
  }
}
