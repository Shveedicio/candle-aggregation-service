package multibank.candle.aggregation.helper

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
class IntegrationalTestProfile {
  @Autowired
  private lateinit var databaseCleaner: DatabaseCleaner

  @BeforeEach
  fun cleanDatabase() {
    databaseCleaner.clean()
  }
}
