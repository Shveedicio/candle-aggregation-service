package multibank.candle.aggregation.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CandleAggregationServiceApplication

fun main(args: Array<String>) {
  runApplication<CandleAggregationServiceApplication>(*args)
}
