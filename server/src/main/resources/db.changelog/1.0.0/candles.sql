--liquibase formatted sql

--changeset Shveedicio:persistence.1
create extension if not exists timescaledb cascade;

--changeset Shveedicio:persistence.2
create table if not exists candles_1s
(
    symbol     text             not null,
    time       timestamp        not null,
    open       double precision not null,
    high       double precision not null,
    low        double precision not null,
    close      double precision not null,
    volume     bigint           not null,
    created_at timestamp        not null default now(),
    primary key (symbol, time)
);

create table candles_5s (like candles_1s including all);
create table candles_1m (like candles_1s including all);
create table candles_5m (like candles_1s including all);
create table candles_15m (like candles_1s including all);
create table candles_1h (like candles_1s including all);

--changeset Shveedicio:persistence.3
select create_hypertable('candles_1s',  'time', chunk_time_interval => interval '1 day',  if_not_exists => true);
select create_hypertable('candles_5s',  'time', chunk_time_interval => interval '3 days', if_not_exists => true);
select create_hypertable('candles_1m',  'time', chunk_time_interval => interval '7 days', if_not_exists => true);
select create_hypertable('candles_5m',  'time', chunk_time_interval => interval '14 days', if_not_exists => true);
select create_hypertable('candles_15m', 'time', chunk_time_interval => interval '30 days', if_not_exists => true);
select create_hypertable('candles_1h',  'time', chunk_time_interval => interval '90 days', if_not_exists => true);

--changeset Shveedicio:persistence.4
create index if not exists idx_candles_1s_symbol_time_desc
    on candles_1s (symbol, time desc);

create index if not exists idx_candles_5s_symbol_time_desc
    on candles_5s (symbol, time desc);

create index if not exists idx_candles_1m_symbol_time_desc
    on candles_1m (symbol, time desc);

create index if not exists idx_candles_5m_symbol_time_desc
    on candles_5m (symbol, time desc);

create index if not exists idx_candles_15m_symbol_time_desc
    on candles_15m (symbol, time desc);

create index if not exists idx_candles_1h_symbol_time_desc
    on candles_1h (symbol, time desc);

--changeset Shveedicio:persistence.5
SELECT add_retention_policy('candles_1s',  INTERVAL '14 days');
SELECT add_retention_policy('candles_5s',  INTERVAL '30 days');
SELECT add_retention_policy('candles_1m',  INTERVAL '180 days');
SELECT add_retention_policy('candles_5m',  INTERVAL '365 days');
SELECT add_retention_policy('candles_15m', INTERVAL '2 years');
SELECT add_retention_policy('candles_1h',  INTERVAL '5 years');

--changeset Shveedicio:persistence.6
create view candles_all as
select '1s'  as interval, * from candles_1s union all
select '5s',  * from candles_5s union all
select '1m',  * from candles_1m union all
select '5m',  * from candles_5m union all
select '15m', * from candles_15m union all
select '1h',  * from candles_1h;

