package multibank.candle.aggregation.kafka.config

import multibank.candle.aggregation.properties.SystemProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaAdmin.NewTopics
import kotlin.collections.distinctBy
import kotlin.collections.map
import kotlin.collections.toMutableList
import kotlin.collections.toTypedArray

@Configuration
@ConditionalOnProperty(
  value = ["kafka.admin.enabled"],
  havingValue = "true",
  matchIfMissing = false,
)
class KafkaAdminConfig(private val systemProperties: SystemProperties) {

  @Bean
  fun kafkaTopics(): NewTopics {
    val defaultKafkaCluster = systemProperties.kafka["default"]!!
    val commonProperties = defaultKafkaCluster.producer.values
      // Cast needed
      .map {
        @Suppress("USELESS_CAST")
        it as SystemProperties.Cluster.Common
      }
      .toMutableList()
      .distinctBy { it.topic }

    val kafkaTopics = commonProperties.map { properties ->
      TopicBuilder
        .name(properties.topic)
        .partitions(properties.partitions)
        .config("retention.ms", properties.retentionMs)
        .build()
    }

    return NewTopics(*kafkaTopics.toTypedArray())
  }
}
