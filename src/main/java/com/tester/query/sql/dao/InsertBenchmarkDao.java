package com.tester.query.sql.dao;

import com.tester.query.sql.util.DatabaseConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class InsertBenchmarkDao implements AutoCloseable {
    private final Connection conn;

    public InsertBenchmarkDao() {
        this.conn = DatabaseConnectionFactory.getConnection();
    }

    public String createTable() throws SQLException {
        String tableName = "testing_table_" + UUID.randomUUID().toString().replace("-", "");
        String sql = isPostgreSQL()
                ? "CREATE TABLE " + tableName + " (id SERIAL PRIMARY KEY, data TEXT)"
                : "CREATE TABLE " + tableName + " (id INT AUTO_INCREMENT PRIMARY KEY, data VARCHAR(255))";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        return tableName;
    }

    public void insertBatch(String tableName, int count) throws SQLException {
        String sql = "INSERT INTO " + tableName + " (data) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                stmt.setString(1, "Batch Data " + i);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public void dropTable(String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS " + tableName);
        }
    }

    private boolean isPostgreSQL() throws SQLException {
        return conn.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
    }

    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
}
