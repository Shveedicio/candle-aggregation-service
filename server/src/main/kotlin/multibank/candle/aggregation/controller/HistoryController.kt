package multibank.candle.aggregation.controller

import multibank.candle.aggregation.api.model.HistoryResponse
import multibank.candle.aggregation.api.v1.HistoryApi
import multibank.candle.aggregation.service.CandlesService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class HistoryController(private val candlesService: CandlesService) : HistoryApi {

  override fun historyGet(symbol: String, interval: String, from: Long, to: Long): ResponseEntity<HistoryResponse> {
    val response = candlesService.getHistory(symbol, interval, from, to)
    return ResponseEntity.ok(response)
  }
}
