package com.tester.query.sql.dao;

import com.tester.query.sql.util.DatabaseConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

public class SelectBenchmarkDao implements AutoCloseable {
    private final Connection conn;
    private final Random random = new Random();

    public SelectBenchmarkDao() throws SQLException {
        this.conn = DatabaseConnectionFactory.getConnection();
    }

    public String createTable(String tableName, boolean includeRelated) throws SQLException {
        String sql;
        if (isPostgreSQL()) {
            sql = includeRelated ? "CREATE TABLE " + tableName + " (id SERIAL PRIMARY KEY, data TEXT, related_id INT)"
                    : "CREATE TABLE " + tableName + " (id SERIAL PRIMARY KEY, data TEXT)";
        } else {
            sql = includeRelated ? "CREATE TABLE " + tableName + " (id INT AUTO_INCREMENT PRIMARY KEY, data TEXT, related_id INT)"
                    : "CREATE TABLE " + tableName + " (id INT AUTO_INCREMENT PRIMARY KEY, data TEXT)";
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        return tableName;
    }

    public void insertRandomData(String tableName, int count, int dataLength, boolean includeRelated) throws SQLException {
        String sql = includeRelated ? "INSERT INTO " + tableName + " (data, related_id) VALUES (?, ?)"
                : "INSERT INTO " + tableName + " (data) VALUES (?)";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                preparedStatement.setString(1, generateRandomString(dataLength));
                if (includeRelated) {
                    preparedStatement.setInt(2, random.nextInt(count) + 1);
                }
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    public void selectRandomRow(String tableName, int count) throws SQLException {
        String sql = "SELECT data FROM " + tableName + " WHERE id = ?";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setInt(1, random.nextInt(count) + 1);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    rs.getString("data");
                }
            }
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
