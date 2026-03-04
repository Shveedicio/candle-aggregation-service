## candle-aggregation-service

### 0. Application Boot
* Run docker containers using `docker compose up -d` in `/local`
* Use `SPRING_PROFILES_ACTIVE=local` to apply local environment
* Visit `localhost:8080/swagger-ui/index.html` to access api documentation

### 1. Persistence

**TimescaleDB layout**
The database is split into **hypertables** by timeframe: `1m`, `5m`, `15m`, `30m`, `1h`.
Each table has a primary key on `(symbol, time)` and an index on `(symbol, time desc)` to efficiently fetch the latest candles.

Chunk sizes and retention policies are chosen so that the number of rows per chunk (for a single symbol) is roughly comparable:
- **1m** – 1 day chunk (86400 records per 24 hours per symbol)
- **5m** – 5 days chunk (86400 / 5 * 5)
- **15m** – 15 days chunk
- **30m** – 30 days chunk
- **1h** – 60 days chunk

Aggregation works with scheduling by add_continuous_aggregate_policy. End border excludes current interval to avoid multiple aggregations.

### 2. Data layer

- **Entity**: `Candle` with fields `symbol`, `interval`, `time`, `open`, `high`, `low`, `close`, `volume`.
- **Repository**: `CandleRepository` interface and `JdbcCandleRepository` implementation using `NamedParameterJdbcTemplate`.

### 3. API

API interfaces and models are generated from the OpenAPI YAML specification (`api/src/main/resources/openapi/*.yaml`) using openapi-generator plugin.

### 4. Kafka
Topic `candle-data` receives candles collected by web-socket as a buffered storage. Messages are represented by protobuf schema `candle_data.proto` to decrease the size of each message. Schema-registry is used for schema versioning.
Message consumption happens in batch mode as well as database insert operations.

Using kafka allows to scale services that collect and provide data from different sources. It also provides consistency while reading data from persistent state. Ones the consumer bean is created - offset will be moved to a configurable time range. This approach prevents data loss when consumer service is being restarted.

`system.kafka.default.consumer.candle-data.topicOffsetMinutes` - where to move the offset before starting to read data after application restarts

### 5. Web socket (candle ingestion)
`BybitConnectionManager` is an entrypoint for candle upstream. We subscribe to Bybit SPOT market. Each consumed candle forwards to `candle-data` kafka topic and becomes part of persistence.
I also described back-pressure behavior inside `CandlesGapRecoveryService#recover`, but skipped the implementation not to exceed functional requirements. This method has to be executed before a websocket connection was established to fill the gap with candles that were not loaded due to application restart or unavailability.

`SubscriptionManager` is responsible for multiple symbol subscription management. List of symbols is configurable by property `system.websockets.symbols`.

### 5. Tests

Test classes are split into different layers: mock-testing using `Mockito` for service layer and integrational testing using `TestContainers` on service-layer.

### 5. Notes and Improvements
There might be some small mistakes that I haven't checked yet. I also skipped a few tests to keep the pace, so I mentioned some notes here that I would apply in a production code:

1. Use `MockMvc` for controller-layer testing
2. OnStartUp uploading service `CandlesGapRecoveryService`. Responsible for uploading candles that were missed during application restart/unavailability.
3. Candle background aggregation leads to small lag, which is related to delayed candles that usually appear with high market volatility.
4. Collecting, saving and aggregating candles seem to be different responsibilities. At least, I recommend not to use only one service for these purposes.
   1.1. Firstly, much better design approach would be to stand out a collector service, which would be responsible for consuming websocket data and delivering it to kafka.
   1.2. Secondly, separated consumer service should be writing this data to timescale-db. It will decrease CPU and RAM consumption in favor of db connections
   1.3. And the third microservice has to be responsible for data aggregation on API level by accessing DB in a read-only mode. It will spread database activity by two services and avoid our service to be overloaded by sql queries.
5. JDBC batch inserts should have limitation to prevent long-running execution
6. Using large amount of symbols in `system.websockets.symbols` (~> 100) may cause some CPU overloads. **Webstream sharding** with **horizontal scaling** will decrease the CPU usage.
7. Use `EmbeddedKafka` for integrational tests
8. Scheduling websocket connection should be implemented with persistence so it could be more scalable. Quartz jobs could be a nice substitution for Executors.
