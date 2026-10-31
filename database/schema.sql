-- ===================================================================
-- Online Examination System with Timer-Based Threads
-- Database Schema (MySQL 8.0+)
-- ===================================================================

CREATE DATABASE IF NOT EXISTS online_exam_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE online_exam_db;

-- -------------------------------------------------------------------
-- 1. Table: users
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_role (role),
    INDEX idx_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------------
-- 2. Table: subjects
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
-- 3. Table: topics
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
-- 4. Table: study_materials
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
-- 5. Table: student_material_progress
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
-- 6. Table: exams
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT NULL,
    topic_id BIGINT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    duration_minutes INT NOT NULL DEFAULT 15,
    total_marks INT NOT NULL DEFAULT 100,
    passing_marks INT NOT NULL DEFAULT 40,
    assessment_type ENUM('FORMAL_EXAM', 'PRACTICE_QUIZ') NOT NULL DEFAULT 'FORMAL_EXAM',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_exams_subject FOREIGN KEY (subject_id)
        REFERENCES subjects(id) ON DELETE SET NULL,
    CONSTRAINT fk_exams_topic FOREIGN KEY (topic_id)
        REFERENCES topics(id) ON DELETE SET NULL,
    INDEX idx_exam_active (is_active),
    INDEX idx_exams_subject (subject_id),
    INDEX idx_exams_topic (topic_id),
    INDEX idx_exams_assessment_type (assessment_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------------
-- 7. Table: questions
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    topic_id BIGINT NULL,
    question_text TEXT NOT NULL,
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255) NOT NULL,
    option_d VARCHAR(255) NOT NULL,
    correct_option ENUM('A', 'B', 'C', 'D') NOT NULL,
    marks INT NOT NULL DEFAULT 1,
    explanation TEXT,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_questions_exam FOREIGN KEY (exam_id)
        REFERENCES exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_questions_topic FOREIGN KEY (topic_id)
        REFERENCES topics(id) ON DELETE SET NULL,
    INDEX idx_question_exam (exam_id),
    INDEX idx_questions_topic (topic_id),
    INDEX idx_questions_published (is_published)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------------
-- 8. Table: student_exams (Exam Sessions)
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS student_exams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NULL,
    time_remaining_seconds INT NOT NULL,
    total_score INT DEFAULT 0,
    status ENUM('NOT_STARTED', 'IN_PROGRESS', 'SUBMITTED', 'AUTO_SUBMITTED') NOT NULL DEFAULT 'NOT_STARTED',
    submitted_at TIMESTAMP NULL,
    CONSTRAINT fk_session_student FOREIGN KEY (student_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_session_exam FOREIGN KEY (exam_id)
        REFERENCES exams(id) ON DELETE CASCADE,
    INDEX idx_session_status (status),
    INDEX idx_session_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------------
-- 9. Table: student_answers
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS student_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option ENUM('A', 'B', 'C', 'D') NULL,
    is_correct BOOLEAN DEFAULT FALSE,
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ans_session FOREIGN KEY (student_exam_id)
        REFERENCES student_exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_ans_question FOREIGN KEY (question_id)
        REFERENCES questions(id) ON DELETE CASCADE,
    UNIQUE KEY uq_session_question (student_exam_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
