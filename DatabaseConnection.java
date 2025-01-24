package org.example;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/property_management"; // Your database URL
    private static final String ADMIN_USER = "admin_user";  // Admin MySQL username
    private static final String ADMIN_PASSWORD = "admin_password";  // Admin password
    private static final String TENANT_USER = "tenant_user";  // Tenant MySQL username
    private static final String TENANT_PASSWORD = "tenant_password";  // Tenant password

    // Admin connection
    public static Connection getAdminConnection() throws Exception {
        return DriverManager.getConnection(URL, ADMIN_USER, ADMIN_PASSWORD);
    }

    // Tenant connection
    public static Connection getTenantConnection() throws Exception {
        return DriverManager.getConnection(URL, TENANT_USER, TENANT_PASSWORD);
    }

    // Generic connection (if needed for future compatibility)
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, ADMIN_USER, ADMIN_PASSWORD);
    }
}
