package com.onlineexam;

import com.onlineexam.config.DatabaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic sanity and configuration tests for Online Examination System.
 */
public class AppTest {

    @Test
    @DisplayName("Application title should match official project name")
    void testApplicationName() {
        String expectedName = "Online Examination System with Timer-Based Threads";
        assertEquals(expectedName, DatabaseConfig.getApplicationName());
    }

    @Test
    @DisplayName("Default JDBC URL should target online_exam_db")
    void testDefaultJdbcUrl() {
        String url = DatabaseConfig.getJdbcUrl();
        assertNotNull(url);
        assertTrue(url.contains("online_exam_db"), "JDBC URL should point to online_exam_db");
    }

    @Test
    @DisplayName("Database username should not be null")
    void testDatabaseUsername() {
        String username = DatabaseConfig.getUsername();
        assertNotNull(username);
        assertFalse(username.isBlank());
    }
}
