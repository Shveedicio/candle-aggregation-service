package multibank.candle.aggregation.kafka.producer

import com.google.protobuf.Message
import multibank.candle.aggregation.properties.SystemProperties
import multibank.candle.aggregation.properties.SystemProperties.Companion.CANDLE_DATA_TOPIC
import multibank.kafka.CandleDataOuterClass
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CandleDataProducer(private val kafkaTemplate: KafkaTemplate<String, Message>, private val systemProperties: SystemProperties) {
  fun produce(candleData: CandleDataOuterClass.CandleData) {
    kafkaTemplate.send(
      systemProperties.kafka["default"]!!.producer[CANDLE_DATA_TOPIC]!!.topic,
      UUID.randomUUID().toString(),
      candleData,
    )
  }
}
