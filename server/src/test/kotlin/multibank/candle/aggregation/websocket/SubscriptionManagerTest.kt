package multibank.candle.aggregation.websocket

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubscriptionManagerTest {

	private val subscriptionManager = SubscriptionManager()

	@Test
	fun `add should add formatted subscription key`() {
		val symbol = "BTCUSDT"
		val interval = "1m"

		subscriptionManager.add(symbol, interval)

		val allSubscriptions = subscriptionManager.all()
		assertEquals(1, allSubscriptions.size)
		assertTrue(allSubscriptions.contains("kline.$interval.$symbol"))
	}

	@Test
	fun `remove should remove formatted subscription key`() {
		val symbol = "ETHUSDT"
		val interval = "5m"
		subscriptionManager.add(symbol, interval)
		assertTrue(subscriptionManager.all().contains("kline.$interval.$symbol"))

		subscriptionManager.remove(symbol, interval)

		val allSubscriptions = subscriptionManager.all()
		assertFalse(allSubscriptions.contains("kline.$interval.$symbol"))
	}

	@Test
	fun `add should be idempotent for same key`() {
		val symbol = "BTCUSDT"
		val interval = "1m"

		subscriptionManager.add(symbol, interval)
		subscriptionManager.add(symbol, interval)

		val allSubscriptions = subscriptionManager.all()
		assertEquals(1, allSubscriptions.size)
		assertTrue(allSubscriptions.contains("kline.$interval.$symbol"))
	}

	@Test
	fun `remove should not fail for non existing key`() {
		val symbol = "NONEXISTENT"
		val interval = "1m"

		subscriptionManager.remove(symbol, interval)

		// ensure nothing was added accidentally
		assertTrue(subscriptionManager.all().isEmpty())
	}
}

