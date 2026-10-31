package com.onlineexam.dao;

import com.onlineexam.config.DatabaseConnection;
import com.onlineexam.model.Topic;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Data Access Object for Topic entity operations.
 */
public class TopicDAO {

    private static final Logger LOGGER = Logger.getLogger(TopicDAO.class.getName());

    public Optional<Topic> findById(Long id) throws SQLException {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, subject_id, name, description, display_order, is_published, created_at, updated_at " +
                     "FROM topics WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTopic(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Topic> findBySubjectId(Long subjectId) throws SQLException {
        List<Topic> topics = new ArrayList<>();
        if (subjectId == null) return topics;
        String sql = "SELECT id, subject_id, name, description, display_order, is_published, created_at, updated_at " +
                     "FROM topics WHERE subject_id = ? ORDER BY display_order ASC, name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    topics.add(mapResultSetToTopic(rs));
                }
            }
        }
        return topics;
    }

    public List<Topic> findPublishedBySubjectId(Long subjectId) throws SQLException {
        List<Topic> topics = new ArrayList<>();
        if (subjectId == null) return topics;
        String sql = "SELECT id, subject_id, name, description, display_order, is_published, created_at, updated_at " +
                     "FROM topics WHERE subject_id = ? AND is_published = TRUE ORDER BY display_order ASC, name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    topics.add(mapResultSetToTopic(rs));
                }
            }
        }
        return topics;
    }

    public List<Topic> findAll() throws SQLException {
        List<Topic> topics = new ArrayList<>();
        String sql = "SELECT id, subject_id, name, description, display_order, is_published, created_at, updated_at " +
                     "FROM topics ORDER BY subject_id ASC, display_order ASC, name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                topics.add(mapResultSetToTopic(rs));
            }
        }
        return topics;
    }

    public Topic create(Topic topic) throws SQLException {
        if (topic == null) throw new IllegalArgumentException("Topic cannot be null.");
        topic.validate();

        String sql = "INSERT INTO topics (subject_id, name, description, display_order, is_published) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, topic.getSubjectId());
            stmt.setString(2, topic.getName());
            stmt.setString(3, topic.getDescription());
            stmt.setInt(4, topic.getDisplayOrder());
            stmt.setBoolean(5, topic.isPublished());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating topic failed, no rows affected.");
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    topic.setId(generatedKeys.getLong(1));
                }
            }
        }
        return topic;
    }

    public boolean update(Topic topic) throws SQLException {
        if (topic == null || topic.getId() == null) {
            throw new IllegalArgumentException("Topic and ID cannot be null.");
        }
        topic.validate();

        String sql = "UPDATE topics SET subject_id = ?, name = ?, description = ?, display_order = ?, " +
                     "is_published = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, topic.getSubjectId());
            stmt.setString(2, topic.getName());
            stmt.setString(3, topic.getDescription());
            stmt.setInt(4, topic.getDisplayOrder());
            stmt.setBoolean(5, topic.isPublished());
            stmt.setLong(6, topic.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Transactional overload: deletes all topics for a subject using the supplied connection.
     * Materials are already deleted before this is called (or will cascade via FK).
     */
    public int deleteBySubjectId(Connection conn, Long subjectId) throws SQLException {
        if (subjectId == null) return 0;
        String sql = "DELETE FROM topics WHERE subject_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, subjectId);
            return stmt.executeUpdate();
        }
    }

    public boolean delete(Long id) throws SQLException {
        if (id == null) return false;
        String sql = "DELETE FROM topics WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public int countMaterials(Long topicId) throws SQLException {
        if (topicId == null) return 0;
        String sql = "SELECT COUNT(*) FROM study_materials WHERE topic_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, topicId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int countExams(Long topicId) throws SQLException {
        if (topicId == null) return 0;
        String sql = "SELECT COUNT(*) FROM exams WHERE topic_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, topicId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private Topic mapResultSetToTopic(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long subjectId = rs.getLong("subject_id");
        String name = rs.getString("name");
        String desc = rs.getString("description");
        int order = rs.getInt("display_order");
        boolean published = rs.getBoolean("is_published");
        Timestamp cAt = rs.getTimestamp("created_at");
        Timestamp uAt = rs.getTimestamp("updated_at");

        LocalDateTime createdAt = (cAt != null) ? cAt.toLocalDateTime() : null;
        LocalDateTime updatedAt = (uAt != null) ? uAt.toLocalDateTime() : null;

        return new Topic(id, subjectId, name, desc, order, published, createdAt, updatedAt);
    }
}
