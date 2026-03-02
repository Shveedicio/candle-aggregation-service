\c "postgres"
CREATE USER "candle-aggregation-service" WITH PASSWORD 'candle-aggregation-service';

CREATE DATABASE "candle-aggregation-service" OWNER "candle-aggregation-service";

ALTER SCHEMA public OWNER TO "candle-aggregation-service";

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "candle-aggregation-service";

ALTER ROLE "candle-aggregation-service" SET search_path = candle_aggregation_service, public;

CREATE SCHEMA candle_aggregation_service AUTHORIZATION "candle-aggregation-service";

\c "candle-aggregation-service"
CREATE EXTENSION IF NOT EXISTS timescaledb;
