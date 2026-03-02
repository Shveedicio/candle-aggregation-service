package multibank.candle.aggregation.helper

import multibank.candle.aggregation.persistence.config.TestPostgresContainerConfig
import multibank.candle.aggregation.persistence.repository.impl.JdbcCandleRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcCandleRepository::class, TestPostgresContainerConfig::class)
@Testcontainers
open class DataTestProfile {

  @Autowired
  protected lateinit var repository: JdbcCandleRepository

  @Autowired
  protected lateinit var jdbcTemplate: NamedParameterJdbcTemplate
}
