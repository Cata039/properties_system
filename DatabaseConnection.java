package org.example;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/property_management"; // Your database URL
    private static final String USER = "cata";  // Your MySQL username
    private static final String PASSWORD = "cata";  // Your MySQL password

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
