package multibank.candle.aggregation.helper

import org.junit.platform.commons.logging.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import kotlin.collections.forEach
import kotlin.jvm.java

@Component
class DatabaseCleaner {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private val selectTablesQuery = """
    select table_name
    from information_schema.tables
    where table_schema = 'public'
    order by table_name
  """

  private val excludedTables = listOf("databasechangelog", "databasechangeloglock")

  fun clean() {
    val tables = jdbcTemplate.queryForList(selectTablesQuery, String::class.java)
    tables.removeAll(excludedTables)
    tables.forEach {
      jdbcTemplate.update(
        "TRUNCATE $it CASCADE",
      )
      log.info { "Table $it has been removed" }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(DatabaseCleaner::class.java)
  }
}
