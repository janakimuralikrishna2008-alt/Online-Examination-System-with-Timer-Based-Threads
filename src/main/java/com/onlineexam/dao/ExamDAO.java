package com.onlineexam.dao;

import com.onlineexam.config.DatabaseConnection;
import com.onlineexam.model.Exam;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Data Access Object for {@link Exam} entity operations.
 */
public class ExamDAO {

    private static final Logger LOGGER = Logger.getLogger(ExamDAO.class.getName());

    public Optional<Exam> findById(Long id) throws SQLException {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, subject_id, topic_id, title, description, duration_minutes, total_marks, " +
                     "passing_marks, assessment_type, is_active, created_at, updated_at FROM exams WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToExam(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Exam> findAll() throws SQLException {
        List<Exam> list = new ArrayList<>();
        String sql = "SELECT id, subject_id, topic_id, title, description, duration_minutes, total_marks, " +
                     "passing_marks, assessment_type, is_active, created_at, updated_at FROM exams ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToExam(rs));
            }
        }
        return list;
    }

    public List<Exam> findAllActive() throws SQLException {
        List<Exam> list = new ArrayList<>();
        String sql = "SELECT id, subject_id, topic_id, title, description, duration_minutes, total_marks, " +
                     "passing_marks, assessment_type, is_active, created_at, updated_at FROM exams " +
                     "WHERE is_active = TRUE ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToExam(rs));
            }
        }
        return list;
    }

    public List<Exam> findBySubjectId(Long subjectId) throws SQLException {
        List<Exam> list = new ArrayList<>();
        if (subjectId == null) return list;
        String sql = "SELECT id, subject_id, topic_id, title, description, duration_minutes, total_marks, " +
                     "passing_marks, assessment_type, is_active, created_at, updated_at FROM exams " +
                     "WHERE subject_id = ? ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToExam(rs));
                }
            }
        }
        return list;
    }

    public List<Exam> findByTopicId(Long topicId) throws SQLException {
        List<Exam> list = new ArrayList<>();
        if (topicId == null) return list;
        String sql = "SELECT id, subject_id, topic_id, title, description, duration_minutes, total_marks, " +
                     "passing_marks, assessment_type, is_active, created_at, updated_at FROM exams " +
                     "WHERE topic_id = ? ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, topicId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToExam(rs));
                }
            }
        }
        return list;
    }

    public List<Exam> findActiveByTopicId(Long topicId) throws SQLException {
        List<Exam> list = new ArrayList<>();
        if (topicId == null) return list;
        String sql = "SELECT id, subject_id, topic_id, title, description, duration_minutes, total_marks, " +
                     "passing_marks, assessment_type, is_active, created_at, updated_at FROM exams " +
                     "WHERE topic_id = ? AND is_active = TRUE ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, topicId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToExam(rs));
                }
            }
        }
        return list;
    }

    public List<Exam> findPracticeQuizzesByTopicId(Long topicId) throws SQLException {
        List<Exam> list = new ArrayList<>();
        if (topicId == null) return list;
        String sql = "SELECT id, subject_id, topic_id, title, description, duration_minutes, total_marks, " +
                     "passing_marks, assessment_type, is_active, created_at, updated_at FROM exams " +
                     "WHERE topic_id = ? AND assessment_type = 'PRACTICE_QUIZ' AND is_active = TRUE ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, topicId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToExam(rs));
                }
            }
        }
        return list;
    }

    public List<Exam> findFormalExamsByTopicId(Long topicId) throws SQLException {
        List<Exam> list = new ArrayList<>();
        if (topicId == null) return list;
        String sql = "SELECT id, subject_id, topic_id, title, description, duration_minutes, total_marks, " +
                     "passing_marks, assessment_type, is_active, created_at, updated_at FROM exams " +
                     "WHERE topic_id = ? AND assessment_type = 'FORMAL_EXAM' AND is_active = TRUE ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, topicId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToExam(rs));
                }
            }
        }
        return list;
    }

    public Exam create(Exam exam) throws SQLException {
        exam.validate();
        String sql = "INSERT INTO exams (subject_id, topic_id, title, description, duration_minutes, " +
                     "total_marks, passing_marks, assessment_type, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (exam.getSubjectId() != null) {
                stmt.setLong(1, exam.getSubjectId());
            } else {
                stmt.setNull(1, Types.BIGINT);
            }
            if (exam.getTopicId() != null) {
                stmt.setLong(2, exam.getTopicId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setString(3, exam.getTitle());
            stmt.setString(4, exam.getDescription());
            stmt.setInt(5, exam.getDurationMinutes());
            stmt.setInt(6, exam.getTotalMarks());
            stmt.setInt(7, exam.getPassingMarks());
            stmt.setString(8, exam.getAssessmentType() != null ? exam.getAssessmentType().name() : "FORMAL_EXAM");
            stmt.setBoolean(9, exam.isActive());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating exam failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    exam.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating exam failed, no ID obtained.");
                }
            }
            return exam;
        }
    }

    public boolean update(Exam exam) throws SQLException {
        exam.validate();
        if (exam.getId() == null) {
            throw new IllegalArgumentException("Exam ID is required for update.");
        }
        String sql = "UPDATE exams SET subject_id = ?, topic_id = ?, title = ?, description = ?, " +
                     "duration_minutes = ?, total_marks = ?, passing_marks = ?, assessment_type = ?, is_active = ? " +
                     "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (exam.getSubjectId() != null) {
                stmt.setLong(1, exam.getSubjectId());
            } else {
                stmt.setNull(1, Types.BIGINT);
            }
            if (exam.getTopicId() != null) {
                stmt.setLong(2, exam.getTopicId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setString(3, exam.getTitle());
            stmt.setString(4, exam.getDescription());
            stmt.setInt(5, exam.getDurationMinutes());
            stmt.setInt(6, exam.getTotalMarks());
            stmt.setInt(7, exam.getPassingMarks());
            stmt.setString(8, exam.getAssessmentType() != null ? exam.getAssessmentType().name() : "FORMAL_EXAM");
            stmt.setBoolean(9, exam.isActive());
            stmt.setLong(10, exam.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Counts student exam attempts (student_exams rows) for all exams of the given subject.
     * Used to block deletion when student history exists.
     */
    public int countAttemptsBySubjectId(Long subjectId) throws SQLException {
        if (subjectId == null) return 0;
        String sql = "SELECT COUNT(*) FROM student_exams se " +
                     "JOIN exams e ON se.exam_id = e.id " +
                     "WHERE e.subject_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Transactional overload: NULLs out subject_id and topic_id on exams belonging to this
     * subject using the supplied connection. We do NOT delete exams here because they may have
     * student_exams (attempt history). Instead we detach them from the subject so the subject
     * row can be removed without violating the SET NULL FK.
     */
    public int detachFromSubject(Connection conn, Long subjectId) throws SQLException {
        if (subjectId == null) return 0;
        String sql = "UPDATE exams SET subject_id = NULL, topic_id = NULL WHERE subject_id = ? OR topic_id IN (SELECT id FROM topics WHERE subject_id = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, subjectId);
            stmt.setLong(2, subjectId);
            return stmt.executeUpdate();
        }
    }

    public boolean delete(Long id) throws SQLException {
        if (id == null) return false;
        String sql = "DELETE FROM exams WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private Exam mapResultSetToExam(ResultSet rs) throws SQLException {
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        Timestamp updatedAtTs = rs.getTimestamp("updated_at");

        Long subjectId = rs.getObject("subject_id", Long.class);
        Long topicId = rs.getObject("topic_id", Long.class);
        String typeStr = rs.getString("assessment_type");
        Exam.AssessmentType type = Exam.AssessmentType.fromString(typeStr);

        return new Exam(
                rs.getLong("id"),
                subjectId,
                topicId,
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("duration_minutes"),
                rs.getInt("total_marks"),
                rs.getInt("passing_marks"),
                type,
                rs.getBoolean("is_active"),
                createdAtTs != null ? createdAtTs.toLocalDateTime() : null,
                updatedAtTs != null ? updatedAtTs.toLocalDateTime() : null
        );
    }
}
