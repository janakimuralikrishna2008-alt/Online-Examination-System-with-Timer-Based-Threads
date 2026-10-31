package com.onlineexam.dao;

import com.onlineexam.config.DatabaseConnection;
import com.onlineexam.model.Subject;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for Subject entity operations.
 */
public class SubjectDAO {

    private static final Logger LOGGER = Logger.getLogger(SubjectDAO.class.getName());

    public Optional<Subject> findById(Long id) throws SQLException {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, name, description, is_published, created_by, created_at, updated_at " +
                     "FROM subjects WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSubject(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Subject> findByName(String name) throws SQLException {
        if (name == null || name.isBlank()) return Optional.empty();
        String sql = "SELECT id, name, description, is_published, created_by, created_at, updated_at " +
                     "FROM subjects WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToSubject(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Subject> findAll() throws SQLException {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT id, name, description, is_published, created_by, created_at, updated_at " +
                     "FROM subjects ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                subjects.add(mapResultSetToSubject(rs));
            }
        }
        return subjects;
    }

    public List<Subject> findAllPublished() throws SQLException {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT id, name, description, is_published, created_by, created_at, updated_at " +
                     "FROM subjects WHERE is_published = TRUE ORDER BY name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                subjects.add(mapResultSetToSubject(rs));
            }
        }
        return subjects;
    }

    public Subject create(Subject subject) throws SQLException {
        if (subject == null) throw new IllegalArgumentException("Subject cannot be null.");
        subject.validate();

        String sql = "INSERT INTO subjects (name, description, is_published, created_by) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, subject.getName());
            stmt.setString(2, subject.getDescription());
            stmt.setBoolean(3, subject.isPublished());
            if (subject.getCreatedBy() != null) {
                stmt.setLong(4, subject.getCreatedBy());
            } else {
                stmt.setNull(4, Types.BIGINT);
            }

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating subject failed, no rows affected.");
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    subject.setId(generatedKeys.getLong(1));
                }
            }
        }
        return subject;
    }

    public boolean update(Subject subject) throws SQLException {
        if (subject == null || subject.getId() == null) {
            throw new IllegalArgumentException("Subject and ID cannot be null.");
        }
        subject.validate();

        String sql = "UPDATE subjects SET name = ?, description = ?, is_published = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, subject.getName());
            stmt.setString(2, subject.getDescription());
            stmt.setBoolean(3, subject.isPublished());
            stmt.setLong(4, subject.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(Long id) throws SQLException {
        if (id == null) return false;
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Counts the total number of student exam attempts (student_exams rows) for all exams
     * belonging to the given subject directly or through its topics. If this count is > 0,
     * deletion must be blocked because deleting the exam rows would cascade and destroy
     * irreplaceable student history.
     */
    public int countStudentAttemptsBySubject(Long subjectId) throws SQLException {
        if (subjectId == null) return 0;
        try (Connection conn = DatabaseConnection.getConnection()) {
            return countStudentAttemptsBySubject(conn, subjectId);
        }
    }

    public int countStudentAttemptsBySubject(Connection conn, Long subjectId) throws SQLException {
        if (subjectId == null) return 0;
        String sql = "SELECT COUNT(*) FROM student_exams se " +
                     "JOIN exams e ON se.exam_id = e.id " +
                     "WHERE e.subject_id = ? OR e.topic_id IN (SELECT id FROM topics WHERE subject_id = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, subjectId);
            stmt.setLong(2, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Returns the list of topics belonging to the given subject (used in transactional cascade).
     */
    public List<com.onlineexam.model.Topic> findTopicsBySubject(Long subjectId) throws SQLException {
        if (subjectId == null) return new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findTopicsBySubject(conn, subjectId);
        }
    }

    public List<com.onlineexam.model.Topic> findTopicsBySubject(Connection conn, Long subjectId) throws SQLException {
        List<com.onlineexam.model.Topic> topics = new ArrayList<>();
        if (subjectId == null) return topics;
        String sql = "SELECT id, subject_id, name, description, display_order, is_published, created_at, updated_at " +
                     "FROM topics WHERE subject_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    topics.add(mapResultSetToTopic(rs));
                }
            }
        }
        return topics;
    }

    /**
     * Transactional overload of delete – uses the provided connection so it participates
     * in the caller's transaction instead of opening a second connection.
     */
    public boolean delete(Connection conn, Long id) throws SQLException {
        if (id == null) return false;
        String sql = "DELETE FROM subjects WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private com.onlineexam.model.Topic mapResultSetToTopic(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long subjectId = rs.getLong("subject_id");
        String name = rs.getString("name");
        String desc = rs.getString("description");
        int order = rs.getInt("display_order");
        boolean published = rs.getBoolean("is_published");
        java.sql.Timestamp cAt = rs.getTimestamp("created_at");
        java.sql.Timestamp uAt = rs.getTimestamp("updated_at");
        java.time.LocalDateTime createdAt = (cAt != null) ? cAt.toLocalDateTime() : null;
        java.time.LocalDateTime updatedAt = (uAt != null) ? uAt.toLocalDateTime() : null;
        return new com.onlineexam.model.Topic(id, subjectId, name, desc, order, published, createdAt, updatedAt);
    }

    public int countTopics(Long subjectId) throws SQLException {
        if (subjectId == null) return 0;
        String sql = "SELECT COUNT(*) FROM topics WHERE subject_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int countExams(Long subjectId) throws SQLException {
        if (subjectId == null) return 0;
        String sql = "SELECT COUNT(*) FROM exams WHERE subject_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private Subject mapResultSetToSubject(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String name = rs.getString("name");
        String desc = rs.getString("description");
        boolean published = rs.getBoolean("is_published");
        Long createdBy = rs.getObject("created_by", Long.class);
        Timestamp cAt = rs.getTimestamp("created_at");
        Timestamp uAt = rs.getTimestamp("updated_at");

        LocalDateTime createdAt = (cAt != null) ? cAt.toLocalDateTime() : null;
        LocalDateTime updatedAt = (uAt != null) ? uAt.toLocalDateTime() : null;

        return new Subject(id, name, desc, published, createdBy, createdAt, updatedAt);
    }
}
