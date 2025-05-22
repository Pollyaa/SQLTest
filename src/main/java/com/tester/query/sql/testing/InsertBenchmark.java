package com.tester.query.sql.testing;

import com.tester.query.sql.dao.InsertBenchmarkDao;
import com.tester.query.sql.websocket.WebSocketSessionManager;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Random;

public class InsertBenchmark {
    private static final Random random = new Random();
    private static final int BATCH_SIZE = 100;
    private static final int TEST_DURATION_SECONDS = 30;
    private final InsertBenchmarkDao insertBenchmarkDao;

    public InsertBenchmark() throws SQLException {
        this.insertBenchmarkDao = new InsertBenchmarkDao();
    }

    public void runTesting() {
        WebSocketSessionManager.broadcast("{\"status\": \"running\", \"scenario\": \"Insert Performance Test\"}");
        String[] tables = new String[5];

        try {
            for (int i = 0; i < 5; i++) {
                tables[i] = insertBenchmarkDao.createTable();
            }
        } catch (SQLException e) {
            handleException(e, "Create Test Tables");
            return;
        }

        long totalRequests = 0;
        long failedRequests = 0;
        long minTimeMs = Long.MAX_VALUE;
        long maxTimeMs = 0;
        long totalTimeMs = 0;
        long requestCount = 0;
        long testStartTime = System.nanoTime();

        try {
            while ((System.nanoTime() - testStartTime) < TEST_DURATION_SECONDS * 1_000_000_000L) {
                String table = tables[random.nextInt(5)];
                long start = System.nanoTime();

                try {
                    insertBenchmarkDao.insertBatch(table, BATCH_SIZE);
                    long durationMs = (System.nanoTime() - start) / 1_000_000;

                    totalRequests += BATCH_SIZE;
                    minTimeMs = Math.min(minTimeMs, durationMs);
                    maxTimeMs = Math.max(maxTimeMs, durationMs);
                    totalTimeMs += durationMs;
                    requestCount++;

                    double queryTime = durationMs / (double) BATCH_SIZE;
                    WebSocketSessionManager.broadcast(String.format(Locale.US,
                            "{\"status\": \"progress\", \"total_requests\": %d, \"failed_requests\": %d, \"batch_time\": %d, \"query_time\": %.2f}",
                            totalRequests, failedRequests, durationMs, queryTime
                    ));
                } catch (SQLException e) {
                    failedRequests++;
                }
            }
        } finally {
            double avgTimeMs = requestCount > 0 ? totalTimeMs / (double) requestCount : 0;

            if (minTimeMs == Long.MAX_VALUE) minTimeMs = 0;
            WebSocketSessionManager.broadcast(String.format(Locale.US,
                    "{\"status\": \"completed\", \"total_requests\": %d, \"failed_requests\": %d, \"min_time\": %.2f, \"max_time\": %.2f, \"avg_time\": %.2f}",
                    totalRequests, failedRequests, minTimeMs / (double) BATCH_SIZE, maxTimeMs / (double) BATCH_SIZE, avgTimeMs / (double) BATCH_SIZE
            ));

            try {
                for (String table : tables) {
                    insertBenchmarkDao.dropTable(table);
                }
            } catch (SQLException e) {
                handleException(e, "Drop Test Tables");
            }
        }
    }

    private void handleException(Exception e, String context) {
        WebSocketSessionManager.broadcast("{\"status\": \"error\", \"context\": \"" + context + "\", \"message\": \"" + e.getMessage() + "\"}");
        e.printStackTrace();
    }
}
