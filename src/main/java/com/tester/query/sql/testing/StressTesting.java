package com.tester.query.sql.testing;

import com.tester.query.sql.dao.StressTestingDao;
import com.tester.query.sql.util.DatabaseConnectionFactory;
import com.tester.query.sql.websocket.WebSocketSessionManager;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StressTesting {
    private static final int BATCH_SIZE = 10;
    private static final int MAX_CONNECTIONS_ERROR = -1;
    private static final int BASIC_TABLES_COUNT = 5;
    private static final int CONNECTION_COUNT = 10;
    private static final long TEST_DURATION_MS = 30000L;
    private static final int SELECT_LIMIT = 10;
    private static final int SCHEDULER_THREAD_COUNT = 1;
    private static final int SCHEDULER_INITIAL_DELAY = 1;
    private static final int SCHEDULER_PERIOD = 1;
    private static final long NANOSECONDS_IN_MILLISECOND = 1000000L;

    private static final Random random = new Random();

    private StressTestingDao stressTestingDao;

    public void runTesting() {
        stressTestingDao = new StressTestingDao();
        TableSetupResult tables = setupTables();
        if (tables == null) return;
        int maxConn = determineMaxConnections();
        if (maxConn == MAX_CONNECTIONS_ERROR) return;
        executeStressTest(tables);
    }

    private int determineMaxConnections() {
        try {
            WebSocketSessionManager.broadcast(new JSONObject().put("status", "max_connections_test_started").toString());
            int maxConnections = DatabaseConnectionFactory.determineMaxConnections();
            WebSocketSessionManager.broadcast(new JSONObject().put("status", "max_connections_test_completed").toString());
            JSONObject maxMsg = new JSONObject();
            maxMsg.put("status", "max_connections_determined");
            maxMsg.put("max_connections", maxConnections);
            WebSocketSessionManager.broadcast(maxMsg.toString());
            return maxConnections;
        } catch (SQLException e) {
            WebSocketSessionManager.broadcast("{\"status\":\"error\",\"code\":\"error_max_connections\"}");
            return MAX_CONNECTIONS_ERROR;
        }
    }

    private TableSetupResult setupTables() {
        TableSetupResult result = new TableSetupResult();
        result.basicTables = new String[BASIC_TABLES_COUNT];
        try {
            for (int i = 0; i < BASIC_TABLES_COUNT; i++) {
                result.basicTables[i] = stressTestingDao.createTable();
                stressTestingDao.insertBatch(result.basicTables[i], BATCH_SIZE);
            }
            result.parentTable = stressTestingDao.createParentTable();
            result.childTable = stressTestingDao.createChildTable(result.parentTable);
            stressTestingDao.insertWithForeignKey(result.parentTable, result.childTable, BATCH_SIZE);
            return result;
        } catch (SQLException e) {
            WebSocketSessionManager.broadcast("{\"status\":\"error\",\"code\":\"error_setup_tables\"}");
            return null;
        }
    }

    private void executeStressTest(TableSetupResult tables) {
        String[] basicTables = tables.basicTables;
        String parentTable = tables.parentTable;
        String childTable = tables.childTable;
        WebSocketSessionManager.broadcast(new JSONObject().put("status", "db_failure_detecting").toString());

        for (Connection conn : DatabaseConnectionFactory.getStoredConnections()) {
            try {
                conn.close();
            } catch (SQLException e) {
                WebSocketSessionManager.broadcast("{\"status\":\"error\",\"code\":\"error_closing_connection\"}");
            }
        }

        boolean allClosed = true;
        for (Connection conn : DatabaseConnectionFactory.getStoredConnections()) {
            try {
                if (!conn.isClosed()) {
                    allClosed = false;
                    break;
                }
            } catch (SQLException e) {
                allClosed = false;
                break;
            }
        }

        if (!allClosed) {
            WebSocketSessionManager.broadcast("{\"status\":\"error\",\"code\":\"error_connections_not_closed\"}");
            return;
        }

        DatabaseConnectionFactory.getStoredConnections().clear();
        LoadTestResult result = runDbLoadTest(basicTables, parentTable, childTable);
        WebSocketSessionManager.broadcast(new JSONObject().put("status", "db_failure_found").toString());
        JSONObject completedMsg = new JSONObject();
        completedMsg.put("status", "completed");
        completedMsg.put("total_requests", result.totalRequests);
        completedMsg.put("max_qps", result.maxQps);
        completedMsg.put("max_time", result.maxTime);
        completedMsg.put("min_time", result.minTime);
        completedMsg.put("avg_time", result.avgTime);
        WebSocketSessionManager.broadcast(completedMsg.toString());
    }

    private LoadTestResult runDbLoadTest(String[] basicTables, String parentTable, String childTable) {
        LoadTestResult loadTestResult = new LoadTestResult();
        try {
            int connectionCount = CONNECTION_COUNT;
            StressTestingDao[] daos = new StressTestingDao[connectionCount];
            Thread[] threads = new Thread[connectionCount];

            for (int i = 0; i < connectionCount; i++) {
                Connection conn = DatabaseConnectionFactory.createNewConnection();
                DatabaseConnectionFactory.getStoredConnections().add(conn);
                daos[i] = new StressTestingDao(conn);
            }

            AtomicLong globalCounter = new AtomicLong(0);
            AtomicLong maxGlobal = new AtomicLong(0);
            AtomicLong totalRequests = new AtomicLong(0);
            AtomicLong totalQueryTime = new AtomicLong(0);
            AtomicLong minQueryTime = new AtomicLong(Long.MAX_VALUE);
            AtomicLong maxQueryTime = new AtomicLong(0);
            AtomicLong intervalQueryTime = new AtomicLong(0);
            AtomicLong intervalQueryCount = new AtomicLong(0);
            long startTime = System.currentTimeMillis();
            Runnable[] tasks = new Runnable[connectionCount];

            for (int i = 0; i < connectionCount; i++) {
                int index = i;
                tasks[i] = () -> {
                    while (System.currentTimeMillis() - startTime < TEST_DURATION_MS) {
                        globalCounter.incrementAndGet();
                        long queryStart = System.nanoTime();
                        if (random.nextBoolean()) {
                            try {
                                daos[index].selectJoin(parentTable, childTable);
                            } catch (SQLException e) {
                                WebSocketSessionManager.broadcast("{\"status\":\"error\",\"code\":\"error_query_join\"}");
                            }
                        } else {
                            try {
                                daos[index].selectData(basicTables[random.nextInt(basicTables.length)], SELECT_LIMIT);
                            } catch (SQLException e) {
                                WebSocketSessionManager.broadcast("{\"status\":\"error\",\"code\":\"error_query_select\"}");
                            }
                        }
                        long queryEnd = System.nanoTime();
                        long elapsed = (queryEnd - queryStart) / NANOSECONDS_IN_MILLISECOND;
                        totalQueryTime.addAndGet(elapsed);
                        minQueryTime.updateAndGet(curr -> Math.min(curr, elapsed));
                        maxQueryTime.updateAndGet(curr -> Math.max(curr, elapsed));
                        intervalQueryTime.addAndGet(elapsed);
                        intervalQueryCount.incrementAndGet();
                    }
                };
            }

            for (int i = 0; i < connectionCount; i++) {
                threads[i] = new Thread(tasks[i]);
                threads[i].start();
            }

            AtomicLong intervalCount = new AtomicLong(0);
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SCHEDULER_THREAD_COUNT);
            scheduler.scheduleAtFixedRate(() -> {
                long count = globalCounter.getAndSet(0);
                totalRequests.addAndGet(count);
                intervalCount.incrementAndGet();
                long intervalSum = intervalQueryTime.getAndSet(0);
                long intervalCountQueries = intervalQueryCount.getAndSet(0);
                double avgQueryTime = intervalCountQueries > 0 ? (double) intervalSum / intervalCountQueries : 0;
                JSONObject progressMsg = new JSONObject();
                progressMsg.put("status", "progress");
                progressMsg.put("total_requests", totalRequests.get());
                progressMsg.put("query_time", avgQueryTime);
                WebSocketSessionManager.broadcast(progressMsg.toString());
            }, SCHEDULER_INITIAL_DELAY, SCHEDULER_PERIOD, TimeUnit.SECONDS);

            for (int i = 0; i < connectionCount; i++) {
                threads[i].join();
            }
            scheduler.shutdownNow();
            loadTestResult.maxQps = (int) (totalRequests.get() / intervalCount.get());
            loadTestResult.totalRequests = totalRequests.get();
            loadTestResult.avgTime = loadTestResult.totalRequests > 0 ? totalQueryTime.get() / (double) loadTestResult.totalRequests : 0;
            loadTestResult.maxTime = maxQueryTime.get();
            loadTestResult.minTime = minQueryTime.get() == Long.MAX_VALUE ? 0 : minQueryTime.get();
            return loadTestResult;
        } catch (Exception e) {
            WebSocketSessionManager.broadcast("{\"status\":\"error\",\"code\":\"error_creating_connection\"}");
            return loadTestResult;
        }
    }

    private static class TableSetupResult {
        String[] basicTables;
        String parentTable;
        String childTable;
    }

    private static class LoadTestResult {
        int maxQps;
        long totalRequests;
        double avgTime;
        long maxTime;
        long minTime;
    }
}
