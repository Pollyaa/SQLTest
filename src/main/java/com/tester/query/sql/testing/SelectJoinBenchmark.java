package com.tester.query.sql.testing;

import com.tester.query.sql.dao.SelectJoinBenchmarkDao;
import com.tester.query.sql.websocket.WebSocketSessionManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SelectJoinBenchmark {
    private static final int TABLE_COUNT = 50;
    private static final int ROWS_PER_TABLE = 1000;
    private static final int DATA_LENGTH = 200;
    private static final int TOTAL_QUERIES = 5000;
    private final Random random = new Random();

    public void runTesting() {
        WebSocketSessionManager.broadcast("{\"status\":\"running\",\"scenario\":\"Select Join Performance Test\"}");
        List<String> tables = new ArrayList<>();

        try (SelectJoinBenchmarkDao dao = new SelectJoinBenchmarkDao()) {
            WebSocketSessionManager.broadcast("{\"message\":\"\uD83C\uDFF3️ Створюються тестові таблиці...\"}");

            for (int i = 0; i < TABLE_COUNT; i++) {
                String tableName = "select_join_test_table_%d_%d".formatted(System.currentTimeMillis(), i);
                boolean hasParent = i > 0;
                dao.createJoinTable(tableName, hasParent);

                if (hasParent) {
                    dao.insertRandomData(tableName, ROWS_PER_TABLE, DATA_LENGTH, true, ROWS_PER_TABLE);
                } else {
                    dao.insertRandomData(tableName, ROWS_PER_TABLE, DATA_LENGTH, false, 0);
                }
                tables.add(tableName);
            }

            WebSocketSessionManager.broadcast("{\"message\":\"\uD83C\uDFC1 Тестові таблиці створені\"}");
            long totalQueriesExecuted = 0;
            long failedQueries = 0;
            long minTimeMs = Long.MAX_VALUE;
            long maxTimeMs = 0;
            long totalTimeMs = 0;

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
                        dao.selectJoinRandomChain(tableArray);
                        long duration = (System.nanoTime() - start) / 1000000;
                        minTimeMs = Math.min(minTimeMs, duration);
                        maxTimeMs = Math.max(maxTimeMs, duration);
                        totalTimeMs += duration;
                    } catch (SQLException e) {
                        failedQueries++;
                    }
                    totalQueriesExecuted++;
                }

                long batchDuration = (System.nanoTime() - batchStart) / 1000000;
                double avgQueryTime = (double) batchDuration / queriesInBatch;
                WebSocketSessionManager.broadcast(String.format(Locale.US,
                        "{\"status\":\"progress\",\"total_queries\":%d,\"failed_queries\":%d,\"batch_time\":%d,\"query_time\":%.2f}",
                        totalQueriesExecuted, failedQueries, batchDuration, avgQueryTime));
            }

            double avgTimeMs = totalQueriesExecuted > 0 ? totalTimeMs / (double) totalQueriesExecuted : 0;
            if (minTimeMs == Long.MAX_VALUE) {
                minTimeMs = 0;
            }

            WebSocketSessionManager.broadcast(String.format(Locale.US,
                    "{\"status\":\"completed\",\"total_queries\":%d,\"failed_queries\":%d,\"min_time\":%.2f,\"max_time\":%.2f,\"avg_time\":%.2f}",
                    totalQueriesExecuted, failedQueries, (double) minTimeMs, (double) maxTimeMs, avgTimeMs));
            for (String table : tables) {
                dao.dropTable(table);
            }
        } catch (SQLException e) {
            WebSocketSessionManager.broadcast("{\"status\":\"error\",\"message\":\"%s\"}".formatted(e.getMessage()));
        }
    }
}
