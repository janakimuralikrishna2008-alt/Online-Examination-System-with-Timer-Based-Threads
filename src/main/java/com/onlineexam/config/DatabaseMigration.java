package com.onlineexam.config;

import java.sql.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Autonomous and idempotent database schema migration manager.
 * Ensures all Learning Platform tables and columns are created cleanly without data loss.
 */
public final class DatabaseMigration {

    private static final Logger LOGGER = Logger.getLogger(DatabaseMigration.class.getName());
    private static final AtomicBoolean MIGRATED = new AtomicBoolean(false);

    private DatabaseMigration() {}

    /**
     * Runs idempotent schema migrations if not already run in this JVM process.
     *
     * @param conn active database connection
     */
    public static void ensureLatestSchema(Connection conn) {
        if (MIGRATED.get()) {
            return;
        }
        synchronized (MIGRATED) {
            if (MIGRATED.get()) {
                return;
            }
            try {
                applyMigrations(conn);
                MIGRATED.set(true);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Automatic schema migration encountered warning: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Resets migration flag (e.g. after database reconfiguration in DB Settings dialog).
     */
    public static void resetMigrationFlag() {
        MIGRATED.set(false);
    }

    private static void applyMigrations(Connection conn) throws SQLException {
        LOGGER.info("Verifying Learning Platform database schema...");
        try (Statement stmt = conn.createStatement()) {
            // 1. subjects table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS subjects (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(150) NOT NULL UNIQUE,
                    description TEXT,
                    is_published BOOLEAN NOT NULL DEFAULT TRUE,
                    created_by BIGINT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    CONSTRAINT fk_subjects_creator FOREIGN KEY (created_by)
                        REFERENCES users(id) ON DELETE SET NULL,
                    INDEX idx_subject_published (is_published)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // 2. topics table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS topics (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    subject_id BIGINT NOT NULL,
                    name VARCHAR(150) NOT NULL,
                    description TEXT,
                    display_order INT NOT NULL DEFAULT 1,
                    is_published BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    CONSTRAINT fk_topics_subject FOREIGN KEY (subject_id)
                        REFERENCES subjects(id) ON DELETE CASCADE,
                    INDEX idx_topic_subject (subject_id),
                    INDEX idx_topic_published (is_published)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // 3. study_materials table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS study_materials (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    topic_id BIGINT NOT NULL,
                    title VARCHAR(200) NOT NULL,
                    content LONGTEXT,
                    resource_url VARCHAR(500) NULL,
                    attachment_path VARCHAR(500) NULL,
                    attachment_type VARCHAR(50) NULL,
                    display_order INT NOT NULL DEFAULT 1,
                    is_published BOOLEAN NOT NULL DEFAULT TRUE,
                    created_by BIGINT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    CONSTRAINT fk_material_topic FOREIGN KEY (topic_id)
                        REFERENCES topics(id) ON DELETE CASCADE,
                    CONSTRAINT fk_material_creator FOREIGN KEY (created_by)
                        REFERENCES users(id) ON DELETE SET NULL,
                    INDEX idx_material_topic (topic_id),
                    INDEX idx_material_published (is_published)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // 4. student_material_progress table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS student_material_progress (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    student_id BIGINT NOT NULL,
                    material_id BIGINT NOT NULL,
                    is_completed BOOLEAN NOT NULL DEFAULT TRUE,
                    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    CONSTRAINT fk_prog_student FOREIGN KEY (student_id)
                        REFERENCES users(id) ON DELETE CASCADE,
                    CONSTRAINT fk_prog_material FOREIGN KEY (material_id)
                        REFERENCES study_materials(id) ON DELETE CASCADE,
                    UNIQUE KEY uq_student_material (student_id, material_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);
        }

        // 5. Upgrade exams table columns if missing
        DatabaseMetaData meta = conn.getMetaData();
        ensureColumnExists(conn, meta, "exams", "subject_id", "BIGINT NULL");
        ensureColumnExists(conn, meta, "exams", "topic_id", "BIGINT NULL");
        ensureColumnExists(conn, meta, "exams", "assessment_type", "ENUM('FORMAL_EXAM', 'PRACTICE_QUIZ') NOT NULL DEFAULT 'FORMAL_EXAM'");

        // 6. Upgrade questions table columns if missing
        ensureColumnExists(conn, meta, "questions", "topic_id", "BIGINT NULL");
        ensureColumnExists(conn, meta, "questions", "is_published", "BOOLEAN NOT NULL DEFAULT TRUE");

        // 7. Seed initial learning materials if subjects table is empty
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM subjects")) {
            if (rs.next() && rs.getInt(1) == 0) {
                LOGGER.info("Seeding initial subjects and study materials...");
                try (Statement seedStmt = conn.createStatement()) {
                    seedStmt.executeUpdate("""
                        INSERT INTO subjects (id, name, description, is_published) 
                        VALUES (1, 'Computer Science & Software Engineering', 'Core disciplines covering programming, concurrency, data structures, and database engineering.', TRUE)
                    """);
                    seedStmt.executeUpdate("""
                        INSERT INTO topics (id, subject_id, name, description, display_order, is_published)
                        VALUES (1, 1, 'Java 21 Concurrency & Virtual Threads', 'Deep dive into Java 21 LTS virtual threads, ScheduledExecutorService, and thread synchronization.', 1, TRUE)
                    """);
                    seedStmt.executeUpdate("""
                        INSERT INTO study_materials (id, topic_id, title, content, resource_url, display_order, is_published)
                        VALUES 
                        (1, 1, 'Java Concurrency Architecture & ScheduledExecutorService', 
                        '# Java Concurrency Architecture\\n\\n## 1. ScheduledExecutorService\\nFor periodic execution and countdown timers, prefer ScheduledExecutorService over java.util.Timer or raw threads.\\n\\n### Code Example:\\n```java\\nScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();\\nscheduler.scheduleAtFixedRate(() -> {\\n    // Tick countdown\\n}, 0, 1, TimeUnit.SECONDS);\\n```\\n\\n## 2. Event Dispatch Thread (EDT) Safety\\nNever mutate Swing components from background threads. Always use:\\n```java\\nSwingUtilities.invokeLater(() -> label.setText("Updated"));\\n```',
                        'https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ScheduledExecutorService.html',
                        1, TRUE),
                        (2, 1, 'Java 21 Virtual Threads & Scalability',
                        '# Java 21 Virtual Threads\\n\\nVirtual threads are lightweight threads introduced in Java 21 (Project Loom).\\n\\nUnlike traditional OS platform threads, millions of virtual threads can be created concurrently without depleting operating system handles or thread stack memory.\\n\\n### Creating a Virtual Thread:\\n```java\\nThread.startVirtualThread(() -> {\\n    System.out.println("Running in virtual thread: " + Thread.currentThread());\\n});\\n```',
                        'https://openjdk.org/jeps/444',
                        2, TRUE)
                    """);
                    seedStmt.executeUpdate("UPDATE exams SET subject_id = 1, topic_id = 1, assessment_type = 'FORMAL_EXAM' WHERE id = 1 AND subject_id IS NULL");
                    seedStmt.executeUpdate("UPDATE questions SET topic_id = 1 WHERE exam_id = 1 AND topic_id IS NULL");
                }
            }
        }
        LOGGER.info("Learning Platform database schema verification completed successfully.");
    }

    private static void ensureColumnExists(Connection conn, DatabaseMetaData meta, String tableName, String columnName, String columnDefinition) {
        try {
            boolean exists = false;
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tableName, columnName)) {
                if (rs.next()) {
                    exists = true;
                }
            }
            if (!exists) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
                    LOGGER.info("Added missing column: " + tableName + "." + columnName);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.FINE, "Column check/add notice for " + tableName + "." + columnName + ": " + e.getMessage());
        }
    }
}
