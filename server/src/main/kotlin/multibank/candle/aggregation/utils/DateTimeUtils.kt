package multibank.candle.aggregation.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun Long.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)
