package com.onlineexam.dao;

import com.onlineexam.config.DatabaseConnection;
import com.onlineexam.model.StudentExam;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for {@link StudentExam} exam session entities.
 */
public class StudentExamDAO {

    private static final Logger LOGGER = Logger.getLogger(StudentExamDAO.class.getName());

    public Optional<StudentExam> findById(Long id) throws SQLException {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, student_id, exam_id, start_time, end_time, time_remaining_seconds, " +
                     "total_score, status, submitted_at FROM student_exams WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStudentExam(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<StudentExam> findByStudentAndExam(Long studentId, Long examId) throws SQLException {
        if (studentId == null || examId == null) return Optional.empty();
        String sql = "SELECT id, student_id, exam_id, start_time, end_time, time_remaining_seconds, " +
                     "total_score, status, submitted_at FROM student_exams " +
                     "WHERE student_id = ? AND exam_id = ? ORDER BY id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, studentId);
            stmt.setLong(2, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStudentExam(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<StudentExam> findByStudentId(Long studentId) throws SQLException {
        List<StudentExam> list = new ArrayList<>();
        if (studentId == null) return list;
        String sql = "SELECT id, student_id, exam_id, start_time, end_time, time_remaining_seconds, " +
                     "total_score, status, submitted_at FROM student_exams " +
                     "WHERE student_id = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToStudentExam(rs));
                }
            }
        }
        return list;
    }

    public List<StudentExam> findAll() throws SQLException {
        List<StudentExam> list = new ArrayList<>();
        String sql = "SELECT id, student_id, exam_id, start_time, end_time, time_remaining_seconds, " +
                     "total_score, status, submitted_at FROM student_exams ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToStudentExam(rs));
            }
        }
        return list;
    }

    public List<StudentExam> findByExamId(Long examId) throws SQLException {
        List<StudentExam> list = new ArrayList<>();
        if (examId == null) return list;
        String sql = "SELECT id, student_id, exam_id, start_time, end_time, time_remaining_seconds, " +
                     "total_score, status, submitted_at FROM student_exams " +
                     "WHERE exam_id = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToStudentExam(rs));
                }
            }
        }
        return list;
    }

    public StudentExam create(StudentExam studentExam) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return create(studentExam, conn);
        }
    }

    public StudentExam create(StudentExam studentExam, Connection conn) throws SQLException {
        studentExam.validate();
        String sql = "INSERT INTO student_exams (student_id, exam_id, time_remaining_seconds, total_score, status) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, studentExam.getStudentId());
            stmt.setLong(2, studentExam.getExamId());
            stmt.setInt(3, studentExam.getTimeRemainingSeconds());
            stmt.setInt(4, studentExam.getTotalScore());
            stmt.setString(5, studentExam.getStatus().name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating student exam failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    studentExam.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating student exam failed, no ID obtained.");
                }
            }
            return studentExam;
        }
    }

    /**
     * Updates the remaining time counter in the database. Called periodically by countdown timer thread.
     */
    public boolean updateTimer(Long studentExamId, int remainingSeconds) throws SQLException {
        if (studentExamId == null) return false;
        String sql = "UPDATE student_exams SET time_remaining_seconds = ? WHERE id = ? AND status = 'IN_PROGRESS'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, Math.max(0, remainingSeconds));
            stmt.setLong(2, studentExamId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(Long studentExamId, StudentExam.ExamStatus status) throws SQLException {
        if (studentExamId == null || status == null) return false;
        String sql = "UPDATE student_exams SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setLong(2, studentExamId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateScoreAndStatus(Long studentExamId, int totalScore, StudentExam.ExamStatus status) throws SQLException {
        if (studentExamId == null || status == null) return false;
        String sql = "UPDATE student_exams SET total_score = ?, status = ?, end_time = CURRENT_TIMESTAMP, " +
                     "submitted_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, totalScore);
            stmt.setString(2, status.name());
            stmt.setLong(3, studentExamId);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean submitExam(Long studentExamId, int totalScore, StudentExam.ExamStatus status, Connection conn) throws SQLException {
        if (studentExamId == null || status == null) return false;
        String sql = "UPDATE student_exams SET total_score = ?, status = ?, end_time = CURRENT_TIMESTAMP, " +
                     "submitted_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, totalScore);
            stmt.setString(2, status.name());
            stmt.setLong(3, studentExamId);

            return stmt.executeUpdate() > 0;
        }
    }

    private StudentExam mapResultSetToStudentExam(ResultSet rs) throws SQLException {
        Timestamp startTs = rs.getTimestamp("start_time");
        Timestamp endTs = rs.getTimestamp("end_time");
        Timestamp subTs = rs.getTimestamp("submitted_at");

        return new StudentExam(
                rs.getLong("id"),
                rs.getLong("student_id"),
                rs.getLong("exam_id"),
                startTs != null ? startTs.toLocalDateTime() : null,
                endTs != null ? endTs.toLocalDateTime() : null,
                rs.getInt("time_remaining_seconds"),
                rs.getInt("total_score"),
                StudentExam.ExamStatus.fromString(rs.getString("status")),
                subTs != null ? subTs.toLocalDateTime() : null
        );
    }
}
