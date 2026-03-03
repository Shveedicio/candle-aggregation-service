--liquibase formatted sql

--changeset Shveedicio:persistence.1 runInTransaction:false splitStatements:false
DO $$
    DECLARE
        intervals           TEXT[]      := ARRAY['1m',         '5m',         '15m',        '30m',        '1h'];
        buckets             TEXT[]      := ARRAY['1 minute',   '5 minutes',  '15 minutes', '30 minutes', '1 hour'];
        start_offsets       TEXT[]      := ARRAY['5 minutes',  '15 minutes', '45 minutes', '2 hours',    '4 hours'];
        end_offsets         TEXT[]      := ARRAY['1 minute',   '5 minutes',  '15 minutes', '30 minutes', '1 hour'];
        schedule_intervals  TEXT[]      := ARRAY['30 seconds', '3 minutes',  '15 minutes', '30 minutes', '1 hour'];
        deactivate_realtime BOOLEAN[]   := ARRAY[false,        true,         true,         true,         true];
        chunk_intervals     TEXT[]      := ARRAY['24 hours',   '5 days',     '15 days',    '30 days',    '60 days'];
        data_source         TEXT[]      := ARRAY['candle',     '1m',         '5m',         '15m',        '30m'];

        view_name TEXT;
        index_name TEXT;
        time_agg_column TEXT;
        source_name TEXT;
    BEGIN
        FOR i IN 1..array_length(intervals, 1) LOOP
                index_name := 'idx_candle_info_' || intervals[i] || '_time_symbol_source';
                view_name := 'candle_info_' || intervals[i];

                IF i = 1 THEN
                    time_agg_column := 'timestamp';
                ELSE
                    time_agg_column := 'start_time';
                END IF;

                IF i = 1 THEN
                    source_name := 'candle';
                ELSE
                    source_name := 'candle_info_' || data_source[i];
                END IF;

                EXECUTE format('DROP MATERIALIZED VIEW IF EXISTS %I CASCADE;', view_name);

                EXECUTE format('
            CREATE MATERIALIZED VIEW %I
            WITH (timescaledb.continuous, timescaledb.materialized_only = %I)
            AS SELECT
                time_bucket(%L, start_time) as start_time,
                source,
                symbol,
                FIRST(open, %I) AS open,
                MAX(high) AS high,
                MIN(low) AS low,
                LAST(close, %I) AS close
            FROM %I
            GROUP BY 1, 2, 3
            WITH NO DATA;
        ', view_name, deactivate_realtime[i], buckets[i], time_agg_column, time_agg_column, source_name);

                -- index to optimize source + symbol lookups
                EXECUTE format('create index if not exists %I ON %I(symbol, source, start_time) INCLUDE (low, high);', index_name, view_name);

                -- chunk time interval
                EXECUTE format('select set_chunk_time_interval(%L, INTERVAL %L);', view_name, chunk_intervals[i]);

                -- raw data range to lookup all the time
                -- [Most recent data]...[end_offset]...[start_offset]...[Oldest data]
                EXECUTE format('
            SELECT add_continuous_aggregate_policy(
              %L,
              start_offset => INTERVAL %L,
              end_offset => INTERVAL %L,
              schedule_interval => INTERVAL %L
            )', view_name, start_offsets[i], end_offsets[i], schedule_intervals[i]);

                -- retention
                EXECUTE format('SELECT add_retention_policy(%L, INTERVAL ''1 month'')', view_name);
            END LOOP;
    END $$;

