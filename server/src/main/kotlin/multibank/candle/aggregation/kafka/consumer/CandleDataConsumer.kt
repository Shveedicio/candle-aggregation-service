package multibank.candle.aggregation.kafka.consumer

import multibank.candle.aggregation.properties.SystemProperties
import multibank.candle.aggregation.properties.SystemProperties.Companion.CANDLE_DATA_TOPIC
import multibank.candle.aggregation.service.CandleIngestionService
import multibank.candle.aggregation.utils.toLocalDateTime
import multibank.kafka.CandleDataOuterClass
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.AbstractConsumerSeekAware
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class CandleDataConsumer(properties: SystemProperties, private val candleIngestionService: CandleIngestionService) :
  AbstractConsumerSeekAware() {
  private val topicOffsetMinutes = properties.kafka["default"]!!.consumer[CANDLE_DATA_TOPIC]!!.topicOffsetMinutes

  @KafkaListener(
    topics = ["\${system.kafka.default.consumer.candle-data.topic}"],
    groupId = "\${spring.kafka.consumer.group-id}",
    containerFactory = "candleDataKafkaListenerContainerFactory",
    batch = "true",
  )
  fun consume(@Payload messages: List<CandleDataOuterClass.CandleData>) {
    candleIngestionService.ingest(messages)
  }

  override fun onPartitionsAssigned(assignments: Map<TopicPartition, Long>, callback: ConsumerSeekCallback) {
    // Here we manually move the offset to avoid missing data on service restart
    // Property topicOffsetMinutes is extracted from configuration so it can be changed depending on restart time
    val timestamp = System.currentTimeMillis() - topicOffsetMinutes * 60 * 1000
    log.info("Search for candles older than {}", timestamp.toLocalDateTime())
    callback.seekToTimestamp(assignments.keys, timestamp)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
