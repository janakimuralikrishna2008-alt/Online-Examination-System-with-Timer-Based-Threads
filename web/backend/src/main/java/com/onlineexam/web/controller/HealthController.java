package com.onlineexam.web.controller;

import com.onlineexam.config.DatabaseConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check endpoint exposing backend and database availability.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("service", "Online Examination System");

        // Safely check database connectivity without leaking credentials or stack traces
        try {
            DatabaseConnection.ConnectionTestResult dbTest = DatabaseConnection.testConnection();
            status.put("database", dbTest.successful() ? "UP" : "DOWN");
        } catch (Exception e) {
            status.put("database", "DOWN");
        }

        return ResponseEntity.ok(status);
    }
}
