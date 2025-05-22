package com.tester.query.sql.dao;

import com.tester.query.sql.util.DatabaseConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

public class SelectJoinBenchmarkDao implements AutoCloseable {
    private final Connection conn;
    private final Random random = new Random();

    public SelectJoinBenchmarkDao() throws SQLException {
        this.conn = DatabaseConnectionFactory.getConnection();
    }

    public String createJoinTable(String tableName, boolean hasParent) throws SQLException {
        String sql = isPostgreSQL() ? hasParent ? "CREATE TABLE " + tableName + " (id SERIAL PRIMARY KEY, data TEXT, parent_id INT)"
                : "CREATE TABLE " + tableName + " (id SERIAL PRIMARY KEY, data TEXT)"
                : hasParent ? "CREATE TABLE " + tableName + " (id INT AUTO_INCREMENT PRIMARY KEY, data TEXT, parent_id INT)"
                : "CREATE TABLE " + tableName + " (id INT AUTO_INCREMENT PRIMARY KEY, data TEXT)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        return tableName;
    }

    public void insertRandomData(String tableName, int count, int dataLength, boolean hasParent, int parentCount)
            throws SQLException {
        String sql = hasParent ? "INSERT INTO " + tableName + " (data, parent_id) VALUES (?, ?)"
                : "INSERT INTO " + tableName + " (data) VALUES (?)";
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                preparedStatement.setString(1, generateRandomString(dataLength));
                if (hasParent) {
                    preparedStatement.setInt(2, random.nextInt(parentCount) + 1);
                }
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    public void selectJoinRandomChain(String[] tables) throws SQLException {
        int startIndex = random.nextInt(tables.length - 1);
        int maxChainLength = Math.min(5, tables.length - startIndex);
        int chainLength = random.nextInt(maxChainLength - 1) + 2;
        StringBuilder sql = new StringBuilder("SELECT ");

        for (int i = 0; i < chainLength; i++) {
            sql.append("t").append(i).append(".data");
            if (i < chainLength - 1) {
                sql.append(", ");
            }
        }

        sql.append(" FROM ").append(tables[startIndex]).append(" t0");
        for (int i = 1; i < chainLength; i++) {
            String joinType = randomJoinType();
            sql.append(" ").append(joinType).append(" ").append(tables[startIndex + i]).append(" t").append(i)
                    .append(" ON t").append(i).append(".parent_id = t").append(i - 1).append(".id");
        }

        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(sql.toString())) {
                while (rs.next()) {
                    rs.getString(1);
                }
            }
        }
    }

    public void dropTable(String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS " + tableName);
        }
    }

    private String randomJoinType() {
        String[] types = {"INNER JOIN", "LEFT JOIN", "RIGHT JOIN"};
        return types[random.nextInt(types.length)];
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
