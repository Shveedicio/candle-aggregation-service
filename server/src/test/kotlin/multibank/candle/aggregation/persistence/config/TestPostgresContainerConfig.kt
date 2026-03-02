package multibank.candle.aggregation.persistence.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@TestConfiguration
@Testcontainers
class TestPostgresContainerConfig {

  companion object {
    @Container
    @JvmStatic
    val postgres: PostgreSQLContainer<*> =
      PostgreSQLContainer(
        DockerImageName
          .parse("timescale/timescaledb:latest-pg17")
          .asCompatibleSubstituteFor("postgres"),
      ).apply {
        withDatabaseName("candle-aggregation-service")
        withUsername("candle-aggregation-service")
        withPassword("candle-aggregation-service")
      }

    @JvmStatic
    @DynamicPropertySource
    fun registerDataSourceProperties(registry: DynamicPropertyRegistry) {
      registry.add("spring.datasource.url") { postgres.jdbcUrl }
      registry.add("spring.datasource.username") { postgres.username }
      registry.add("spring.datasource.password") { postgres.password }

      registry.add("spring.liquibase.enabled") { false }
    }
  }
}

