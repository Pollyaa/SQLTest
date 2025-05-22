package com.tester.query.sql.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConnectionRequest {
    private String dbType;
    private String host;
    private String port;
    private String username;
    private String password;
    private String database;
}
