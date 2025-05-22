package com.tester.query.sql.testing;

import com.tester.query.sql.dao.UpdateBenchmarkDao;
import com.tester.query.sql.websocket.WebSocketSessionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class UpdateBenchmark {
    private static final int TABLE_COUNT = 5;
    private static final int ROWS_PER_TABLE = 1000;
    private static final int DATA_LENGTH = 200;
    private static final int TOTAL_QUERIES = 200;
    private final Random random = new Random();

    public void runTesting() {
        WebSocketSessionManager.broadcast("{\"status\":\"running\",\"scenario\":\"Update Benchmark\"}");
        List<String> tables = new ArrayList<>();

        try (UpdateBenchmarkDao dao = new UpdateBenchmarkDao()) {
            WebSocketSessionManager.broadcast("{\"message\":\"\uD83C\uDFF3 Створюються тестові таблиці...\"}");

            for (int i = 0; i < TABLE_COUNT; i++) {
                String tableName = "update_benchmark_test_table_%d_%d".formatted(System.currentTimeMillis(), i);
                dao.createUpdateTable(tableName);
                dao.insertRandomData(tableName, ROWS_PER_TABLE, DATA_LENGTH);
                tables.add(tableName);
            }

            WebSocketSessionManager.broadcast("{\"message\":\"\uD83C\uDFC1 Тестові таблиці створені\"}");
            long totalQueriesExecuted = 0;
            long failedQueries = 0;
            double minTimeSec = Double.MAX_VALUE;
            double maxTimeSec = 0;
            double totalTimeSec = 0;

            String[] tableArray = tables.toArray(new String[0]);
            while (totalQueriesExecuted < TOTAL_QUERIES) {
                int queriesInBatch = random.nextInt(4) + 2;
                if (totalQueriesExecuted + queriesInBatch > TOTAL_QUERIES) {
                    queriesInBatch = (int) (TOTAL_QUERIES - totalQueriesExecuted);
                }

                long batchStart = System.nanoTime();
                for (int i = 0; i < queriesInBatch; i++) {
                    long start = System.nanoTime();
                    try {
                        dao.updateRandomData(tableArray[random.nextInt(TABLE_COUNT)], ROWS_PER_TABLE);
                        double durationSec = (System.nanoTime() - start) / 1_000_000_000.0;
                        minTimeSec = Math.min(minTimeSec, durationSec);
                        maxTimeSec = Math.max(maxTimeSec, durationSec);
                        totalTimeSec += durationSec;
                    } catch (SQLException e) {
                        failedQueries++;
                    }
                    totalQueriesExecuted++;
                }

                double batchDurationSec = (System.nanoTime() - batchStart) / 1_000_000_000.0;
                double avgQueryTimeSec = batchDurationSec / queriesInBatch;
                WebSocketSessionManager.broadcast(String.format(Locale.US,
                        "{\"status\":\"progress\",\"total_queries\":%d,\"failed_queries\":%d,\"batch_time\":%.6f,\"query_time\":%.6f}",
                        totalQueriesExecuted, failedQueries, batchDurationSec, avgQueryTimeSec));
            }

            double avgTimeSec = totalQueriesExecuted > 0 ? totalTimeSec / totalQueriesExecuted : 0;
            if (minTimeSec == Double.MAX_VALUE) {
                minTimeSec = 0;
            }

            WebSocketSessionManager.broadcast(String.format(Locale.US,
                    "{\"status\":\"completed\",\"total_queries\":%d,\"failed_queries\":%d,\"min_time\":%.6f,\"max_time\":%.6f,\"avg_time\":%.6f}",
                    totalQueriesExecuted, failedQueries, minTimeSec, maxTimeSec, avgTimeSec));
            for (String table : tables) {
                dao.dropTable(table);
            }
        } catch (SQLException e) {
            WebSocketSessionManager.broadcast("{\"status\":\"error\",\"message\":\"%s\"}"
                    .formatted(e.getMessage().replaceAll("\"", "'")));
        }
    }
}
