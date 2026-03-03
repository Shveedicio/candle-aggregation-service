package multibank.candle.aggregation.websocket

enum class ConnectionState {
  DISCONNECTED,
  RECOVERING,
  LIVE,
}
