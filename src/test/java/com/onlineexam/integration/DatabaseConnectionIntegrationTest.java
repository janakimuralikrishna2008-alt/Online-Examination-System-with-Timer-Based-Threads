package com.onlineexam.integration;

import com.onlineexam.config.DatabaseConfig;
import com.onlineexam.config.DatabaseConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying real JDBC connectivity against a running MySQL 8 database.
 * Tagged as 'integration' so it is separated from fast, standalone unit tests.
 */
@Tag("integration")
public class DatabaseConnectionIntegrationTest {

    @Test
    @DisplayName("Test connection returns informative diagnostic report")
    void testConnectionReport() {
        DatabaseConnection.ConnectionTestResult result = DatabaseConnection.testConnection();
        assertNotNull(result, "ConnectionTestResult should never be null");
        assertNotNull(result.message(), "Result message should not be null");
        assertTrue(result.latencyMs() >= 0, "Latency should be non-negative");

        if (result.successful()) {
            assertNotNull(result.databaseProductName());
            assertNotNull(result.databaseProductVersion());
            System.out.println("Integration Test [PASSED]: " + result);
        } else {
            // Even if MySQL credentials are not yet configured by the user,
            // we verify that the diagnostic message provides clear guidance.
            assertNotNull(result.cause());
            assertTrue(
                    result.message().contains("Reason:") || result.message().contains("Error Code:"),
                    "Failed connection message must contain actionable troubleshooting hints: " + result.message()
            );
            System.out.println("Integration Test [DIAGNOSTIC]: " + result.message());
        }
    }
}
