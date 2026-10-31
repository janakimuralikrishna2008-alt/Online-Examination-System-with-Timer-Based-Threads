package com.onlineexam.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthControllerTest {

    @Test
    @DisplayName("Health endpoint returns status UP and service name")
    void testHealthCheckReturnsUp() {
        HealthController controller = new HealthController();
        ResponseEntity<Map<String, Object>> response = controller.healthCheck();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        assertEquals("Online Examination System", body.get("service"));
        assertTrue(body.containsKey("database"));
    }
}
