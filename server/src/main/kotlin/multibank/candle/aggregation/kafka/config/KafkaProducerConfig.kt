package multibank.candle.aggregation.kafka.config

import com.google.protobuf.Message
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.ssl.DefaultSslBundleRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate

@Configuration
class KafkaProducerConfig(
  private val kafkaProperties: KafkaProperties,
  @Value("\${spring.kafka.producer.properties.schema.registry.url}")
  private val schemaRegistryUrl: String,
) {

  @Bean
  fun commonKafkaTemplate(): KafkaTemplate<String, Message> {
    val protobufSerializer = KafkaProtobufSerializer<Message>(
      CachedSchemaRegistryClient(
        schemaRegistryUrl,
        100,
      ),
      kafkaProperties.buildProducerProperties(DefaultSslBundleRegistry()),
    )

    return KafkaTemplate(
      DefaultKafkaProducerFactory(
        kafkaProperties.buildProducerProperties(
          DefaultSslBundleRegistry(),
        ),
        StringSerializer(),
        protobufSerializer,
      ),
    )
  }
}
