package com.mycompany.keycloak.authenticator.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBUtil {

    private static final String URL = "jdbc:postgresql://localhost:5432/keycloak_db";
    private static final String USER = "grootan";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}