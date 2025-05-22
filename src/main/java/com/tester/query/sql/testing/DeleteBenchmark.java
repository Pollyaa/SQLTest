package com.tester.query.sql.testing;

import com.tester.query.sql.dao.DeleteBenchmarkDao;
import com.tester.query.sql.websocket.WebSocketSessionManager;

import java.sql.SQLException;
import java.util.Locale;

public class DeleteBenchmark {
    private static final int NUM_TABLES = 10;
    private static final int ROWS_PER_TABLE = 500;
    private final DeleteBenchmarkDao deleteBenchmarkDao;

    public DeleteBenchmark() throws SQLException {
        this.deleteBenchmarkDao = new DeleteBenchmarkDao();
    }

    public void runTesting() {
        WebSocketSessionManager.broadcast("{\"status\": \"running\", \"scenario\": \"Delete Performance Test\"}");
        String[] tables = new String[NUM_TABLES];

        try {
            WebSocketSessionManager.broadcast("{\"message\":\"\uD83C\uDFF3️ Створюються тестові таблиці...\"}");
            for (int i = 0; i < NUM_TABLES; i++) {
                tables[i] = deleteBenchmarkDao.createTable();
            }
            for (String table : tables) {
                deleteBenchmarkDao.fillTable(table, ROWS_PER_TABLE);
            }
            WebSocketSessionManager.broadcast("{\"message\":\"\uD83C\uDFC1 Тестові таблиці створені\"}");
        } catch (SQLException e) {
            handleException(e, "Create and Fill Test Tables");
            return;
        }

        long totalRequests = 0;
        long failedRequests = 0;
        long requestCount = 0;
        double minTimeMs = Double.MAX_VALUE;
        double maxTimeMs = 0;
        double totalTimeMs = 0;

        for (String table : tables) {
            for (int id = 1; id <= ROWS_PER_TABLE; id++) {
                long start = System.nanoTime();
                try {
                    int deleted = deleteBenchmarkDao.deleteRow(table, id);
                    double durationMs = (System.nanoTime() - start) / 1_000_000.0;
                    if (deleted == 0) {
                        failedRequests++;
                    } else {
                        totalRequests++;
                        minTimeMs = Math.min(minTimeMs, durationMs);
                        maxTimeMs = Math.max(maxTimeMs, durationMs);
                        totalTimeMs += durationMs;
                        requestCount++;
                    }
                    WebSocketSessionManager.broadcast(String.format(Locale.US,
                            "{\"status\": \"progress\", \"total_requests\": %d, \"failed_requests\": %d, \"query_time\": %.2f}",
                            totalRequests, failedRequests, durationMs));
                } catch (SQLException e) {
                    failedRequests++;
                    WebSocketSessionManager.broadcast("{\"status\": \"error\", \"context\": \"Delete Query\", \"message\": \"" + e.getMessage() + "\"}");
                }
            }
        }

        double avgTimeMs = requestCount > 0 ? totalTimeMs / requestCount : 0;
        if (minTimeMs == Double.MAX_VALUE) {
            minTimeMs = 0;
        }

        WebSocketSessionManager.broadcast(String.format(Locale.US,
                "{\"status\": \"completed\", \"total_requests\": %d, \"failed_requests\": %d, \"min_time\": %.2f, \"max_time\": %.2f, \"avg_time\": %.2f}",
                totalRequests, failedRequests, minTimeMs, maxTimeMs, avgTimeMs));

        try {
            for (String table : tables) {
                deleteBenchmarkDao.dropTable(table);
            }
        } catch (SQLException e) {
            handleException(e, "Drop Test Tables");
        }
    }

    private void handleException(Exception e, String context) {
        WebSocketSessionManager.broadcast("{\"status\": \"error\", \"context\": \"" + context + "\", \"message\": \"" + e.getMessage() + "\"}");
        e.printStackTrace();
    }
}
