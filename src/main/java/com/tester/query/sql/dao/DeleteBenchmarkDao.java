package com.tester.query.sql.dao;

import com.tester.query.sql.util.DatabaseConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class DeleteBenchmarkDao implements AutoCloseable {
    private final Connection conn;

    public DeleteBenchmarkDao() {
        this.conn = DatabaseConnectionFactory.getConnection();
    }

    public String createTable() throws SQLException {
        String tableName = "delete_testing_table_" + UUID.randomUUID().toString().replace("-", "");
        String sql = isPostgreSQL() ? "CREATE TABLE " + tableName + " (id SERIAL PRIMARY KEY, data TEXT)"
                : "CREATE TABLE " + tableName + " (id INT AUTO_INCREMENT PRIMARY KEY, data TEXT)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        return tableName;
    }

    public void fillTable(String tableName, int count) throws SQLException {
        String sql = "INSERT INTO " + tableName + " (data) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                stmt.setString(1, generateRandomString(1024));
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public int deleteRow(String tableName, int id) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate();
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

    private String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * characters.length());
            sb.append(characters.charAt(index));
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
