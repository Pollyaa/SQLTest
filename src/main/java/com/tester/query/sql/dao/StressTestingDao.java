package com.tester.query.sql.dao;

import com.tester.query.sql.util.DatabaseConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class StressTestingDao {
    private final Connection connection;

    public StressTestingDao() {
        this.connection = DatabaseConnectionFactory.getConnection();
    }

    public StressTestingDao(Connection connection) {
        this.connection = connection;
    }

    public String createTable() throws SQLException {
        String tableName = "testing_table_" + UUID.randomUUID().toString().replace("-", "");
        String query = connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                ? "CREATE TABLE %s (id SERIAL PRIMARY KEY, data VARCHAR(255))"
                : "CREATE TABLE %s (id INT AUTO_INCREMENT PRIMARY KEY, data VARCHAR(255))";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(query, tableName));
        }
        return tableName;
    }

    public void insertBatch(String tableName, int count) throws SQLException {
        String query = connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                ? "INSERT INTO " + tableName + " (data) VALUES (?)"
                : "INSERT INTO " + tableName + " (data) VALUES (?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int i = 0; i < count; i++) {
                stmt.setString(1, "Batch Data " + i);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public String createParentTable() throws SQLException {
        String tableName = "testing_table_" + UUID.randomUUID().toString().replace("-", "");
        String query = connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                ? "CREATE TABLE %s (id SERIAL PRIMARY KEY, data VARCHAR(255))"
                : "CREATE TABLE %s (id INT AUTO_INCREMENT PRIMARY KEY, data VARCHAR(255))";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(query, tableName));
        }
        return tableName;
    }

    public String createChildTable(String parentTable) throws SQLException {
        String tableName = "testing_table_" + UUID.randomUUID().toString().replace("-", "");
        String query = connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                ? "CREATE TABLE %s (id SERIAL PRIMARY KEY, parent_id INT, data VARCHAR(255), FOREIGN KEY (parent_id) REFERENCES %s(id))"
                : "CREATE TABLE %s (id INT AUTO_INCREMENT PRIMARY KEY, parent_id INT, data VARCHAR(255), FOREIGN KEY (parent_id) REFERENCES %s(id))";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(query, tableName, parentTable));
        }
        return tableName;
    }

    public void insertWithForeignKey(String parentTable, String childTable, int count) throws SQLException {
        String parentQuery = connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                ? "INSERT INTO " + parentTable + " (data) VALUES (?)"
                : "INSERT INTO " + parentTable + " (data) VALUES (?)";
        String childQuery = connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                ? "INSERT INTO " + childTable + " (parent_id, data) VALUES (?, ?)"
                : "INSERT INTO " + childTable + " (parent_id, data) VALUES (?, ?)";
        try (PreparedStatement parentStmt = connection.prepareStatement(parentQuery, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement childStmt = connection.prepareStatement(childQuery)) {
            for (int i = 0; i < count; i++) {
                parentStmt.setString(1, "Parent Data " + i);
                parentStmt.executeUpdate();
                try (ResultSet rs = parentStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int parentId = rs.getInt(1);
                        childStmt.setInt(1, parentId);
                        childStmt.setString(2, "Child Data " + i);
                        childStmt.executeUpdate();
                    }
                }
            }
        }
    }

    public void selectData(String tableName, int limit) throws SQLException {
        String query = connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                ? "SELECT * FROM " + tableName + " LIMIT ?"
                : "SELECT * FROM " + tableName + " LIMIT ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, limit);
            stmt.executeQuery();
        }
    }

    public void selectJoin(String parentTable, String childTable) throws SQLException {
        String query = connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                ? "SELECT p.id, p.data, c.data FROM " + parentTable + " p JOIN " + childTable + " c ON p.id = c.parent_id LIMIT 10"
                : "SELECT p.id, p.data, c.data FROM " + parentTable + " p JOIN " + childTable + " c ON p.id = c.parent_id LIMIT 10";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.executeQuery();
        }
    }

    public int getDatabaseLoad() throws SQLException {
        String query = connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                ? "SELECT count(*) FROM pg_stat_activity"
                : "SHOW STATUS LIKE 'Threads_connected'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                        ? rs.getInt(1)
                        : Integer.parseInt(rs.getString("Value"));
            }
        }
        return 0;
    }

    public int getCurrentMaxConnections() throws SQLException {
        String query = connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                ? "SHOW max_connections"
                : "SHOW VARIABLES LIKE 'max_connections'";

        try (Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery(query)) {
            if (resultSet.next()) {
                return connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                        ? resultSet.getInt("max_connections")
                        : Integer.parseInt(resultSet.getString("Value"));
            }
        }
        return 0;
    }

    public void setDBMaxConnections(int newMax) throws SQLException {
        String query = connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                ? "ALTER SYSTEM SET max_connections = " + newMax
                : "SET GLOBAL max_connections = " + newMax;
        try (Statement stmt = DatabaseConnectionFactory.getConnection().createStatement()) {
            stmt.execute(query);
        }
    }
}
