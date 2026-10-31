-- ===================================================================
-- Online Examination System with Timer-Based Threads
-- Sample Seed Data (MySQL 8.0+)
-- ===================================================================

USE online_exam_db;

-- -------------------------------------------------------------------
-- 1. Seed Users
-- Default passwords for testing:
-- admin / admin123
-- student1 / student123
-- student2 / student123
-- -------------------------------------------------------------------
INSERT INTO users (username, password_hash, full_name, email, role) VALUES
('admin', '$2a$10$kiVLO6wlUvcWkZNyaMLk1O5147RAZt2eVWI6klxVEkSNDBQX8Grfe', 'System Administrator', 'admin@onlineexam.com', 'ADMIN'),
('student1', '$2a$10$Negaj2NhFsVeYJBHpa80R.tGuN1uII/cV4.8Ut5BNYos4y38SU5.a', 'Alice Johnson', 'alice.student@onlineexam.com', 'STUDENT'),
('student2', '$2a$10$Negaj2NhFsVeYJBHpa80R.tGuN1uII/cV4.8Ut5BNYos4y38SU5.a', 'Bob Smith', 'bob.student@onlineexam.com', 'STUDENT')
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), password_hash = VALUES(password_hash);

-- -------------------------------------------------------------------
-- 2. Seed Sample Exam
-- -------------------------------------------------------------------
INSERT INTO exams (id, title, description, duration_minutes, total_marks, passing_marks, is_active) VALUES
(1, 'Java 21 & Concurrency Fundamentals', 'Comprehensive assessment evaluating knowledge of Java 21, multi-threading, ScheduledExecutorService, and JDBC persistence.', 10, 4, 3, TRUE)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- -------------------------------------------------------------------
-- 3. Seed Questions for Exam 1
-- -------------------------------------------------------------------
INSERT INTO questions (exam_id, question_text, option_a, option_b, option_c, option_d, correct_option, marks, explanation) VALUES
(1, 'Which Java class is best suited for executing periodic countdown timer tasks with precise intervals?',
 'java.lang.Thread',
 'java.util.concurrent.ScheduledExecutorService',
 'java.util.Vector',
 'java.lang.ProcessBuilder',
 'B', 1, 'ScheduledExecutorService provides scheduleAtFixedRate and scheduleWithFixedDelay for robust recurring and delayed tasks.'),

(1, 'In Java Swing, all UI component updates must occur on which thread?',
 'Main Thread',
 'Garbage Collector Thread',
 'Event Dispatch Thread (EDT)',
 'Worker Daemon Thread',
 'C', 1, 'Swing components are not thread-safe and must be mutated only on the Event Dispatch Thread via SwingUtilities.invokeLater().'),

(1, 'What is the primary benefit of using Virtual Threads introduced in Java 21?',
 'They replace the garbage collector entirely.',
 'They are lightweight threads managed by the JVM that reduce the cost of blocking I/O.',
 'They run only on GPU cores.',
 'They eliminate the need for JDBC connections.',
 'B', 1, 'Virtual threads are lightweight threads that preserve the synchronous programming style while scaling to high throughput.'),

(1, 'Which JDBC statement type should be used to protect database queries against SQL Injection attacks?',
 'Statement',
 'CallableStatement',
 'PreparedStatement',
 'ResultSet',
 'C', 1, 'PreparedStatement pre-compiles SQL and parameterizes input data safely, preventing SQL injection vulnerabilities.')
ON DUPLICATE KEY UPDATE question_text = VALUES(question_text);
