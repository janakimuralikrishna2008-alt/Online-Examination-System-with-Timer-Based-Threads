package com.onlineexam.web;

import com.onlineexam.config.DatabaseConfig;
import com.onlineexam.config.DatabaseConnection;
import com.onlineexam.config.DatabaseMigration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spring Boot REST API Application for the Online Examination Platform.
 */
@SpringBootApplication
public class OnlineExamWebApplication {

    private static final Logger LOGGER = Logger.getLogger(OnlineExamWebApplication.class.getName());

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @Value("${spring.datasource.username:root}")
    private String dbUser;

    public static void main(String[] args) {
        SpringApplication.run(OnlineExamWebApplication.class, args);
    }

    @PostConstruct
    public void init() {
        LOGGER.info("Initializing Online Examination Web Backend...");
        if (dbPassword != null && !dbPassword.isBlank()) {
            DatabaseConfig.setRuntimeCredentials(null, dbUser, dbPassword);
        }
        try (Connection conn = DatabaseConnection.getConnection()) {
            LOGGER.info("Database connection established successfully: " + conn.getCatalog());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database connection check during startup: " + e.getMessage());
        }
    }

}
