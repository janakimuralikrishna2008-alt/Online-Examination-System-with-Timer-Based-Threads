package com.onlineexam.config;

/**
 * Command-line utility to test and troubleshoot database connectivity.
 * <p>
 * Run directly via:
 * {@code java -cp bin com.onlineexam.config.DatabaseConnectionTester}
 * </p>
 */
public class DatabaseConnectionTester {

    public static void main(String[] args) {
        System.out.println("===================================================================");
        System.out.println("  Online Examination System with Timer-Based Threads");
        System.out.println("  Database Connectivity Diagnostic Utility");
        System.out.println("===================================================================");

        System.out.println("JDBC Target URL : " + DatabaseConfig.getJdbcUrl());
        System.out.println("Configured User : " + DatabaseConfig.getUsername());
        System.out.println("Password Set    : " + (!DatabaseConfig.getPassword().isEmpty() ? "Yes (Masked)" : "No (Empty)"));
        System.out.println("-------------------------------------------------------------------");
        System.out.println("Attempting connection to MySQL server...");

        DatabaseConnection.ConnectionTestResult result = DatabaseConnection.testConnection();

        System.out.println("-------------------------------------------------------------------");
        if (result.successful()) {
            System.out.println("STATUS: SUCCESS");
            System.out.println("Message          : " + result.message());
            System.out.println("Database Server  : " + result.databaseProductName() + " " + result.databaseProductVersion());
            System.out.println("Catalog/Database : " + result.databaseName());
            System.out.println("Connected User   : " + result.connectedUser());
            System.out.println("Roundtrip Latency: " + result.latencyMs() + " ms");
            System.out.println("===================================================================");
            System.out.println("Database connectivity is fully operational.");
        } else {
            System.err.println("STATUS: FAILED");
            System.err.println("Error Message    : " + result.message());
            System.err.println("Roundtrip Latency: " + result.latencyMs() + " ms");
            System.err.println("-------------------------------------------------------------------");
            System.err.println("Troubleshooting Checklist:");
            System.err.println("  1. Verify MySQL Service is running: Get-Service *mysql*");
            System.err.println("  2. If Access Denied: Set DB_PASSWORD environment variable:");
            System.err.println("     $env:DB_PASSWORD=\"your_password\"");
            System.err.println("  3. If Unknown Database: Create it by executing database/schema.sql");
            System.err.println("===================================================================");
            System.exit(1);
        }
    }
}
