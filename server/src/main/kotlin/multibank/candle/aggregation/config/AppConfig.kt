package multibank.candle.aggregation.config

import multibank.candle.aggregation.properties.SystemProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@EnableConfigurationProperties(SystemProperties::class)
@Configuration
class AppConfig
