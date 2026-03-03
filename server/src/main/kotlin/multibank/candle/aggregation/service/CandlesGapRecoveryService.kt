package multibank.candle.aggregation.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CandlesGapRecoveryService {

  @Transactional
  fun recover() {
    // 1. Get last closed candle for each required symbol from DB. openTime required for that
    // 2. Send request to BybitHttpClient to fetch missing candles. Execute in multiple threads
    // 3. Insert batches of each symbol
  }
}
