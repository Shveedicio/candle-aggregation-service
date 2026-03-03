package multibank.candle.aggregation.kafka.config

import multibank.candle.aggregation.properties.SystemProperties
import multibank.candle.aggregation.properties.SystemProperties.Companion.CANDLE_DATA_TOPIC
import multibank.kafka.CandleDataOuterClass.CandleData
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.ssl.DefaultSslBundleRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff
import kotlin.apply
import kotlin.collections.toMutableMap

@Configuration
class KafkaConsumerConfig(private val kafkaProperties: KafkaProperties, systemProperties: SystemProperties) {

  val defaultKafkaClusterConsumer = systemProperties.kafka["default"]!!.consumer

  // Candle Data
  @Bean
  fun candleDataKafkaListenerContainerFactory() = typedKafkaListenerFactory<CandleData>(CANDLE_DATA_TOPIC)

  private inline fun <reified T : Any> typedKafkaListenerFactory(consumerName: String): ConcurrentKafkaListenerContainerFactory<String, T> {
    val props = kafkaProperties.buildConsumerProperties(DefaultSslBundleRegistry()).toMutableMap().apply {
      putAll(
        defaultKafkaClusterConsumer[consumerName]!!.properties,
      )
    }

    return ConcurrentKafkaListenerContainerFactory<String, T>().apply {
      consumerFactory = DefaultKafkaConsumerFactory(props)
      setCommonErrorHandler(
        DefaultErrorHandler(
          DeadLetterPublishingRecoverer(
            KafkaTemplate(
              DefaultKafkaProducerFactory(
                kafkaProperties.buildProducerProperties(
                  DefaultSslBundleRegistry(),
                ),
              ),
            ),
          ),
          FixedBackOff(1000L, 5),
        ),
      )
    }
  }
}
