package com.onlineexam.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database configuration manager.
 * <p>
 * Securely loads database connection parameters using a precedence hierarchy:
 * 1. Environment variables (DB_URL, DB_USER, DB_PASSWORD)
 * 2. application.properties file located in classpath
 * </p>
 * Passwords are never hard-coded in code or committed to repositories.
 */
public final class DatabaseConfig {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    private static final String CONFIG_FILE = "application.properties";

    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private DatabaseConfig() {
        // Utility class, prevent instantiation
    }

    private static void loadProperties() {
        properties.clear();
        // 1. Load from classpath resources
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Failed to load classpath database properties", e);
        }

        // 2. Overlay with local file from user directory if present (e.g. workspace edits)
        java.io.File localFile = new java.io.File(System.getProperty("user.dir"), CONFIG_FILE);
        if (!localFile.exists()) {
            localFile = new java.io.File(System.getProperty("user.dir"), "src/main/resources/" + CONFIG_FILE);
        }
        if (localFile.exists() && localFile.isFile()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(localFile)) {
                properties.load(fis);
                LOGGER.info("Overlayed database configuration from: " + localFile.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load local properties from: " + localFile.getAbsolutePath(), e);
            }
        }
    }

    public static void reloadProperties() {
        loadProperties();
    }

    private static volatile String runtimeUrl = null;
    private static volatile String runtimeUser = null;
    private static volatile String runtimePassword = null;

    /**
     * Sets runtime credentials override, useful for interactive configuration in the UI.
     */
    public static void setRuntimeCredentials(String url, String user, String password) {
        runtimeUrl = (url != null && !url.isBlank()) ? url.trim() : null;
        runtimeUser = (user != null && !user.isBlank()) ? user.trim() : null;
        runtimePassword = password;
        DatabaseMigration.resetMigrationFlag();
    }

    public static String getJdbcUrl() {
        if (runtimeUrl != null && !runtimeUrl.isBlank()) {
            return runtimeUrl;
        }
        // Support standard cloud provider environment variables
        for (String varName : new String[]{"DB_URL", "DATABASE_URL", "SPRING_DATASOURCE_URL", "MYSQL_URL"}) {
            String val = System.getenv(varName);
            if (val != null && !val.isBlank()) {
                val = val.trim();
                if (val.startsWith("mysql://")) {
                    val = "jdbc:" + val;
                }
                // Ensure cloud connections enforce secure TLS/SSL and proper timezone handling
                if (val.startsWith("jdbc:mysql://") && !val.contains("sslMode=") && !val.contains("useSSL=")) {
                    String separator = val.contains("?") ? "&" : "?";
                    val = val + separator + "sslMode=REQUIRED&serverTimezone=UTC";
                }
                return val;
            }
        }
        return properties.getProperty("db.url", "jdbc:mysql://localhost:3306/online_exam_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    }

    public static String getUsername() {
        if (runtimeUser != null && !runtimeUser.isBlank()) {
            return runtimeUser;
        }
        for (String varName : new String[]{"DB_USER", "DATABASE_USERNAME", "MYSQL_USER", "SPRING_DATASOURCE_USERNAME"}) {
            String val = System.getenv(varName);
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        return properties.getProperty("db.user", "root");
    }

    public static String getPassword() {
        if (runtimePassword != null) {
            return runtimePassword;
        }
        for (String varName : new String[]{"DB_PASSWORD", "DATABASE_PASSWORD", "MYSQL_PASSWORD", "SPRING_DATASOURCE_PASSWORD"}) {
            String val = System.getenv(varName);
            if (val != null) {
                return val;
            }
        }
        return properties.getProperty("db.password", "");
    }

    public static String getApplicationName() {
        return properties.getProperty("app.name", "Online Examination System with Timer-Based Threads");
    }
}
