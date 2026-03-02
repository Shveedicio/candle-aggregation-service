## candle-aggregation-service

### 0. Application Startup
* Run docker containers using `docker compose up -d` in `/local`
* Use `SPRING_PROFILES_ACTIVE=local` to apply local environment
* Visit `localhost:8080/swagger-ui/index.html` to access api documentation

### 1. Persistence

**TimescaleDB layout**
The database is split into **hypertables** by timeframe: `1s`, `5s`, `1m`, `5m`, `15m`, `1h`.
Each table has a primary key on `(symbol, time)` and an index on `(symbol, time desc)` to efficiently fetch the latest candles.

Chunk sizes and retention policies are chosen so that the number of rows per chunk (for a single symbol) is roughly comparable:
- **1s** – 1 day chunk (86400 records per 24 hours per symbol)
- **5s** – 3 days chunk (86400 / 5 * 3)
- **1m** – 7 days chunk
- **5m** – 14 days chunk
- **15m** – 30 days chunk
- **1h** – 90 days chunk

### 2. Data layer

- **Entity**: `Candle` with fields `symbol`, `interval`, `time`, `open`, `high`, `low`, `close`, `volume`.
- **Repository**: `CandleRepository` interface and `JdbcCandleRepository` implementation using `NamedParameterJdbcTemplate`.
- **Behavior**:
  - `saveBatch` uses `INSERT ... ON CONFLICT (symbol, time) DO UPDATE` for idempotent candle writes.
  - `findRange` selects candles by `symbol` and time range \([from, to]\) ordered by `time ASC`.
  - `findLast` selects the latest candle for a `symbol` (`ORDER BY time DESC LIMIT 1`).

### 3. API

API interfaces and models are generated from the OpenAPI YAML specification (`api/src/main/resources/openapi/*.yaml`) using openapi-generator plugin.

### 4. Tests

- Tests use a simple DDL for `candles_1s` without Timescale-specific functions (hypertable/retention), which is enough to validate repository SQL logic.
- Test classes are split into different layers: mock-testing using `Mockito` for service layer and integrational testing using `TestContainers` on service-layer

### 5. Remarks & hacks

### 6. ToDo's and Improvements
1. Use `MockMvc` for controller-layer testing
