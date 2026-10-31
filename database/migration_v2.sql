-- ===================================================================
-- Online Examination System with Timer-Based Threads
-- Database Migration V2: Learning Platform Schema Upgrade
-- ===================================================================

USE online_exam_db;

-- -------------------------------------------------------------------
-- 1. Table: subjects
-- -------------------------------------------------------------------
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

-- -------------------------------------------------------------------
-- 2. Table: topics
-- -------------------------------------------------------------------
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

-- -------------------------------------------------------------------
-- 3. Table: study_materials
-- -------------------------------------------------------------------
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

-- -------------------------------------------------------------------
-- 4. Table: student_material_progress
-- -------------------------------------------------------------------
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

-- -------------------------------------------------------------------
-- 5. Safe Column Upgrades for `exams`
-- -------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upgrade_exams_table;
DELIMITER $$
CREATE PROCEDURE upgrade_exams_table()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exams' AND COLUMN_NAME = 'subject_id'
    ) THEN
        ALTER TABLE exams ADD COLUMN subject_id BIGINT NULL;
        ALTER TABLE exams ADD CONSTRAINT fk_exams_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL;
        ALTER TABLE exams ADD INDEX idx_exams_subject (subject_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exams' AND COLUMN_NAME = 'topic_id'
    ) THEN
        ALTER TABLE exams ADD COLUMN topic_id BIGINT NULL;
        ALTER TABLE exams ADD CONSTRAINT fk_exams_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE SET NULL;
        ALTER TABLE exams ADD INDEX idx_exams_topic (topic_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exams' AND COLUMN_NAME = 'assessment_type'
    ) THEN
        ALTER TABLE exams ADD COLUMN assessment_type ENUM('FORMAL_EXAM', 'PRACTICE_QUIZ') NOT NULL DEFAULT 'FORMAL_EXAM';
        ALTER TABLE exams ADD INDEX idx_exams_assessment_type (assessment_type);
    END IF;
END $$
DELIMITER ;
CALL upgrade_exams_table();
DROP PROCEDURE IF EXISTS upgrade_exams_table;

-- -------------------------------------------------------------------
-- 6. Safe Column Upgrades for `questions`
-- -------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upgrade_questions_table;
DELIMITER $$
CREATE PROCEDURE upgrade_questions_table()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'questions' AND COLUMN_NAME = 'topic_id'
    ) THEN
        ALTER TABLE questions ADD COLUMN topic_id BIGINT NULL;
        ALTER TABLE questions ADD CONSTRAINT fk_questions_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE SET NULL;
        ALTER TABLE questions ADD INDEX idx_questions_topic (topic_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'questions' AND COLUMN_NAME = 'is_published'
    ) THEN
        ALTER TABLE questions ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT TRUE;
        ALTER TABLE questions ADD INDEX idx_questions_published (is_published);
    END IF;
END $$
DELIMITER ;
CALL upgrade_questions_table();
DROP PROCEDURE IF EXISTS upgrade_questions_table;

-- -------------------------------------------------------------------
-- 7. Seed Initial Learning Platform Content
-- -------------------------------------------------------------------
INSERT INTO subjects (id, name, description, is_published) 
VALUES (1, 'Computer Science & Software Engineering', 'Core disciplines covering programming, concurrency, data structures, and database engineering.', TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO topics (id, subject_id, name, description, display_order, is_published)
VALUES (1, 1, 'Java 21 Concurrency & Virtual Threads', 'Deep dive into Java 21 LTS virtual threads, ScheduledExecutorService, and thread synchronization.', 1, TRUE)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO study_materials (id, topic_id, title, content, resource_url, display_order, is_published)
VALUES 
(1, 1, 'Java Concurrency Architecture & ScheduledExecutorService', 
'# Java Concurrency Architecture\n\n## 1. ScheduledExecutorService\nFor periodic execution and countdown timers, prefer ScheduledExecutorService over java.util.Timer or raw threads.\n\n### Code Example:\n```java\nScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();\nscheduler.scheduleAtFixedRate(() -> {\n    // Tick countdown\n}, 0, 1, TimeUnit.SECONDS);\n```\n\n## 2. Event Dispatch Thread (EDT) Safety\nNever mutate Swing components from background threads. Always use:\n```java\nSwingUtilities.invokeLater(() -> label.setText("Updated"));\n```',
'https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ScheduledExecutorService.html',
1, TRUE),
(2, 1, 'Java 21 Virtual Threads & Scalability',
'# Java 21 Virtual Threads\n\nVirtual threads are lightweight threads introduced in Java 21 (Project Loom).\n\nUnlike traditional OS platform threads, millions of virtual threads can be created concurrently without depleting operating system handles or thread stack memory.\n\n### Creating a Virtual Thread:\n```java\nThread.startVirtualThread(() -> {\n    System.out.println("Running in virtual thread: " + Thread.currentThread());\n});\n```',
'https://openjdk.org/jeps/444',
2, TRUE)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- Connect existing Exam 1 and its questions to Subject 1 and Topic 1
UPDATE exams SET subject_id = 1, topic_id = 1, assessment_type = 'FORMAL_EXAM' WHERE id = 1 AND (subject_id IS NULL OR topic_id IS NULL);
UPDATE questions SET topic_id = 1 WHERE exam_id = 1 AND topic_id IS NULL;
