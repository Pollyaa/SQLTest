package com.tester.query.sql.util;

import com.tester.query.sql.model.ConnectionRequest;
import com.tester.query.sql.dao.StressTestingDao;
import lombok.Getter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnectionFactory {

    @Getter
    private static final List<Connection> storedConnections = new ArrayList<>();
    private static Connection connection;
    private static String connectionUrl;
    private static String dbUsername;
    private static String dbPassword;

    @Getter
    private static int currentMaxConnections;

    private DatabaseConnectionFactory() {
        // Private constructor to prevent instantiation
    }

    public static void initializeConnection(ConnectionRequest request) throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:%s://%s:%s/%s".formatted(request.getDbType().toLowerCase(),
                    request.getHost(), request.getPort(), request.getDatabase());
            connection = DriverManager.getConnection(url, request.getUsername(), request.getPassword());
            connectionUrl = url;
            dbUsername = request.getUsername();
            dbPassword = request.getPassword();
            currentMaxConnections = new StressTestingDao().getCurrentMaxConnections();
        }
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                throw new RuntimeException("Database connection is not initialized.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public static Connection createNewConnection() throws SQLException {
        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(connectionUrl, dbUsername, dbPassword);
    }

    public static int determineMaxConnections() throws SQLException {
        int initialActiveConnections = new StressTestingDao().getDatabaseLoad() - 1;
        if (connection != null && !storedConnections.contains(connection)) {
            storedConnections.add(connection);
        }

        while (true) {
            try {
                Connection newConnection = createNewConnection();
                storedConnections.add(newConnection);
            } catch (SQLException e) {
                String msg = e.getMessage().toLowerCase();
                if (e.getErrorCode() == 1040 || "53300".equals(e.getSQLState()) || msg.contains("too many connections")
                        || msg.contains("max_connections") || msg.contains("too many clients")) {
                    try {
                        int newMax = currentMaxConnections * 2;
                        new StressTestingDao().setDBMaxConnections(newMax);
                        currentMaxConnections = newMax;
                    } catch (SQLException ex) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return initialActiveConnections + storedConnections.size();
    }
}
