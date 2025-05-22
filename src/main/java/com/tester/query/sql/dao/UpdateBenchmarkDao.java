package com.tester.query.sql.dao;

import com.tester.query.sql.util.DatabaseConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

public class UpdateBenchmarkDao implements AutoCloseable {
    private final Connection conn;
    private final Random random = new Random();

    public UpdateBenchmarkDao() throws SQLException {
        this.conn = DatabaseConnectionFactory.getConnection();
    }

    public String createUpdateTable(String tableName) throws SQLException {
        String sql = "CREATE TABLE " + tableName + " (id SERIAL PRIMARY KEY, data TEXT)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        return tableName;
    }

    public void insertRandomData(String tableName, int count, int dataLength) throws SQLException {
        String sql = "INSERT INTO " + tableName + " (data) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                stmt.setString(1, generateRandomString(dataLength));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public void updateRandomData(String tableName, int rowsPerTable) throws SQLException {
        String sql = "UPDATE " + tableName + " SET data = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < rowsPerTable; i++) {
                stmt.setString(1, generateRandomString(200));
                stmt.setInt(2, i + 1);
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

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
}
