package com.tester.query.sql.service;

import com.tester.query.sql.util.DatabaseConnectionFactory;
import com.tester.query.sql.model.ConnectionRequest;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestService {
    private Connection connection;
    private ConnectionRequest lastRequest;

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void connect(ConnectionRequest request) throws SQLException {
        DatabaseConnectionFactory.initializeConnection(request);
        this.connection = DatabaseConnectionFactory.getConnection();
        this.lastRequest = request;
    }

    public Map<String, String> getConnectionDetails() {
        if (lastRequest == null || !isConnected()) {
            return null;
        }

        Map<String, String> details = new HashMap<>();
        details.put("dbType", lastRequest.getDbType());
        details.put("host", lastRequest.getHost());
        details.put("port", lastRequest.getPort());
        details.put("username", lastRequest.getUsername());
        details.put("database", lastRequest.getDatabase());
        return details;
    }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        connection = null;
        lastRequest = null;
    }

    public Map<String, Object> getDatabaseStructure() throws SQLException {
        if (!isConnected()) {
            throw new SQLException("No active database connection.");
        }

        Map<String, Object> databaseStructure = new HashMap<>();
        List<Map<String, Object>> tables = new ArrayList<>();

        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tablesResultSet = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tablesResultSet.next()) {
                String tableName = tablesResultSet.getString("TABLE_NAME");

                List<Map<String, String>> columns = new ArrayList<>();
                try (ResultSet columnsResultSet = metaData.getColumns(null, null, tableName, "%")) {
                    while (columnsResultSet.next()) {
                        Map<String, String> columnInfo = new HashMap<>();
                        columnInfo.put("name", columnsResultSet.getString("COLUMN_NAME"));
                        columnInfo.put("type", columnsResultSet.getString("TYPE_NAME"));
                        columnInfo.put("constraints", columnsResultSet.getString("IS_NULLABLE").equals("NO") ? "NOT NULL" : "");

                        columns.add(columnInfo);
                    }
                }

                Map<String, Object> table = new HashMap<>();
                table.put("name", tableName);
                table.put("columns", columns);
                tables.add(table);
            }
        }

        databaseStructure.put("tables", tables);
        return databaseStructure;
    }
}
