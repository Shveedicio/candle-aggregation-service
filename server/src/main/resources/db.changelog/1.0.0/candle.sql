--liquibase formatted sql

--changeset Shveedicio:persistence.1
create table if not exists candle
(
    source     text      null,
    symbol     text      not null,
    open       decimal   not null,
    high       decimal   not null,
    low        decimal   not null,
    close      decimal   not null,
    volume      decimal   not null,
    start_time timestamp not null,
    timestamp  timestamp not null
);

select create_hypertable('candle', by_range('start_time', interval '12 hours'));

create index if not exists ix_candle_time_symbol_source_symbol on candle (symbol, source, start_time);

-- compress data older than 1 day
alter table candle
    set (
        timescaledb.compress,
        timescaledb.compress_orderby = 'start_time',
        timescaledb.compress_segmentby = 'symbol, source'
        );
select add_compression_policy('candle', interval '12 hours');

-- drop raw data older than 1 month
select add_retention_policy('candle', interval '1 month');
