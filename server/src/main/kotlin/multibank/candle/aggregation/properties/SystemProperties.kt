package multibank.candle.aggregation.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "system")
class SystemProperties(var kafka: Map<String, Cluster>) {
  data class Cluster(val consumer: Map<String, NodeConfig>, val producer: Map<String, NodeConfig>) {

    open class Common(
      val topic: String,
      val partitions: Int,
      val properties: Map<String, String>,
      var retentionMs: String,
      var topicOffsetMinutes: Long,
    )

    class NodeConfig(
      topic: String,
      partitions: Int = 1,
      properties: Map<String, String> = emptyMap(),
      retentionMs: String = "604800000",
      topicOffsetMinutes: Long = 0,
    ) : Common(
      topic,
      partitions,
      properties,
      retentionMs,
      topicOffsetMinutes,
    )
  }

  companion object {
    const val CANDLE_DATA_TOPIC = "candle-data"
  }
}
