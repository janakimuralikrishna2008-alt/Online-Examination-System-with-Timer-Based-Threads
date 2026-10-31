package com.onlineexam.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages JDBC connectivity to MySQL 8+.
 * Uses {@link DatabaseConfig} to retrieve connection credentials securely.
 */
public final class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";

    static {
        try {
            Class.forName(MYSQL_DRIVER);
            LOGGER.info("MySQL JDBC Driver registered successfully: " + MYSQL_DRIVER);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "MySQL JDBC Driver not found on classpath: " + MYSQL_DRIVER, e);
        }
    }

    private DatabaseConnection() {
        // Utility class
    }

    /**
     * Obtains a new JDBC connection using configured parameters.
     * Callers are responsible for closing the connection (e.g. via try-with-resources).
     *
     * @return a live {@link Connection} instance
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        String url = DatabaseConfig.getJdbcUrl();
        String user = DatabaseConfig.getUsername();
        String password = DatabaseConfig.getPassword();

        Connection conn = DriverManager.getConnection(url, user, password);
        DatabaseMigration.ensureLatestSchema(conn);
        return conn;
    }

    /**
     * Tests the database connection and returns diagnostic result information.
     *
     * @return {@link ConnectionTestResult} containing status, metadata, and error details if any
     */
    public static ConnectionTestResult testConnection() {
        long startTime = System.currentTimeMillis();
        String url = DatabaseConfig.getJdbcUrl();
        String user = DatabaseConfig.getUsername();

        try (Connection conn = getConnection()) {
            boolean valid = conn.isValid(3); // 3-second timeout
            long latency = System.currentTimeMillis() - startTime;

            if (valid) {
                DatabaseMetaData metaData = conn.getMetaData();
                return new ConnectionTestResult(
                        true,
                        "Successfully connected to MySQL database: " + conn.getCatalog(),
                        metaData.getDatabaseProductName(),
                        metaData.getDatabaseProductVersion(),
                        conn.getCatalog(),
                        user,
                        latency,
                        null
                );
            } else {
                return new ConnectionTestResult(
                        false,
                        "Connection established but failed validity check (timeout).",
                        null, null, null, user, latency, null
                );
            }
        } catch (SQLException e) {
            long latency = System.currentTimeMillis() - startTime;
            String diagnosticMessage = buildDiagnosticMessage(e, url, user);
            LOGGER.log(Level.WARNING, diagnosticMessage, e);
            return new ConnectionTestResult(
                    false,
                    diagnosticMessage,
                    null, null, null, user, latency, e
            );
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return new ConnectionTestResult(
                    false,
                    "Unexpected error during connection test: " + e.getMessage(),
                    null, null, null, user, latency, e
            );
        }
    }

    private static String buildDiagnosticMessage(SQLException e, String url, String user) {
        int errorCode = e.getErrorCode();
        String sqlState = e.getSQLState();
        String rawMsg = e.getMessage();

        StringBuilder sb = new StringBuilder();
        sb.append("Database connection failed. URL='").append(url)
          .append("', User='").append(user).append("'. ");

        if (errorCode == 1045) { // ER_ACCESS_DENIED_ERROR
            sb.append("Reason: Access denied (Authentication failed). Please check your MySQL username and password (e.g., set DB_PASSWORD environment variable).");
        } else if (errorCode == 1049) { // ER_BAD_DB_ERROR
            sb.append("Reason: Unknown database 'online_exam_db'. Please create the database using database/schema.sql.");
        } else if ("08S01".equals(sqlState) || rawMsg.contains("Communications link failure")) {
            sb.append("Reason: Communications link failure. Ensure MySQL Server is running and listening on the configured port.");
        } else {
            sb.append("Error Code: ").append(errorCode)
              .append(", SQLState: ").append(sqlState)
              .append(", Message: ").append(rawMsg);
        }
        return sb.toString();
    }

    /**
     * Diagnostic report holding connection test metrics and metadata.
     */
    public record ConnectionTestResult(
            boolean successful,
            String message,
            String databaseProductName,
            String databaseProductVersion,
            String databaseName,
            String connectedUser,
            long latencyMs,
            Throwable cause
    ) {
        @Override
        public String toString() {
            if (successful) {
                return String.format(
                        "CONNECTED [Success] %s | Server: %s %s | Database: %s | User: %s | Latency: %d ms",
                        message, databaseProductName, databaseProductVersion, databaseName, connectedUser, latencyMs
                );
            } else {
                return String.format(
                        "DISCONNECTED [Failed] %s | User: %s | Latency: %d ms",
                        message, connectedUser, latencyMs
                );
            }
        }
    }
}
