package com.onlineexam.dao;

import com.onlineexam.config.DatabaseConnection;
import com.onlineexam.model.StudyMaterial;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * Data Access Object for StudyMaterial and student progress operations.
 */
public class StudyMaterialDAO {

    private static final Logger LOGGER = Logger.getLogger(StudyMaterialDAO.class.getName());

    public Optional<StudyMaterial> findById(Long id) throws SQLException {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, topic_id, title, content, resource_url, attachment_path, attachment_type, " +
                     "display_order, is_published, created_by, created_at, updated_at FROM study_materials WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStudyMaterial(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<StudyMaterial> findByTopicId(Long topicId) throws SQLException {
        List<StudyMaterial> list = new ArrayList<>();
        if (topicId == null) return list;
        String sql = "SELECT id, topic_id, title, content, resource_url, attachment_path, attachment_type, " +
                     "display_order, is_published, created_by, created_at, updated_at " +
                     "FROM study_materials WHERE topic_id = ? ORDER BY display_order ASC, id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, topicId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToStudyMaterial(rs));
                }
            }
        }
        return list;
    }

    public List<StudyMaterial> findPublishedByTopicId(Long topicId) throws SQLException {
        List<StudyMaterial> list = new ArrayList<>();
        if (topicId == null) return list;
        String sql = "SELECT id, topic_id, title, content, resource_url, attachment_path, attachment_type, " +
                     "display_order, is_published, created_by, created_at, updated_at " +
                     "FROM study_materials WHERE topic_id = ? AND is_published = TRUE ORDER BY display_order ASC, id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, topicId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToStudyMaterial(rs));
                }
            }
        }
        return list;
    }

    public StudyMaterial create(StudyMaterial material) throws SQLException {
        if (material == null) throw new IllegalArgumentException("Study material cannot be null.");
        material.validate();

        String sql = "INSERT INTO study_materials (topic_id, title, content, resource_url, attachment_path, " +
                     "attachment_type, display_order, is_published, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, material.getTopicId());
            stmt.setString(2, material.getTitle());
            stmt.setString(3, material.getContent());
            stmt.setString(4, material.getResourceUrl());
            stmt.setString(5, material.getAttachmentPath());
            stmt.setString(6, material.getAttachmentType());
            stmt.setInt(7, material.getDisplayOrder());
            stmt.setBoolean(8, material.isPublished());
            if (material.getCreatedBy() != null) {
                stmt.setLong(9, material.getCreatedBy());
            } else {
                stmt.setNull(9, Types.BIGINT);
            }

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating study material failed, no rows affected.");
            }
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    material.setId(keys.getLong(1));
                }
            }
        }
        return material;
    }

    public boolean update(StudyMaterial material) throws SQLException {
        if (material == null || material.getId() == null) {
            throw new IllegalArgumentException("Study material and ID cannot be null.");
        }
        material.validate();

        String sql = "UPDATE study_materials SET topic_id = ?, title = ?, content = ?, resource_url = ?, " +
                     "attachment_path = ?, attachment_type = ?, display_order = ?, is_published = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, material.getTopicId());
            stmt.setString(2, material.getTitle());
            stmt.setString(3, material.getContent());
            stmt.setString(4, material.getResourceUrl());
            stmt.setString(5, material.getAttachmentPath());
            stmt.setString(6, material.getAttachmentType());
            stmt.setInt(7, material.getDisplayOrder());
            stmt.setBoolean(8, material.isPublished());
            stmt.setLong(9, material.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Transactional overload: deletes all study materials for the given topic using
     * the supplied connection (so it participates in the caller's transaction).
     */
    public int deleteByTopicId(Connection conn, Long topicId) throws SQLException {
        if (topicId == null) return 0;
        String sql = "DELETE FROM study_materials WHERE topic_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, topicId);
            return stmt.executeUpdate();
        }
    }

    public boolean delete(Long id) throws SQLException {
        if (id == null) return false;
        String sql = "DELETE FROM study_materials WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean markCompleted(Long studentId, Long materialId) throws SQLException {
        return setCompleted(studentId, materialId, true);
    }

    public boolean setCompleted(Long studentId, Long materialId, boolean isCompleted) throws SQLException {
        if (studentId == null || materialId == null) return false;
        String sql = "INSERT INTO student_material_progress (student_id, material_id, is_completed) " +
                     "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE is_completed = ?, completed_at = CURRENT_TIMESTAMP";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, studentId);
            stmt.setLong(2, materialId);
            stmt.setBoolean(3, isCompleted);
            stmt.setBoolean(4, isCompleted);
            return stmt.executeUpdate() > 0;
        }
    }

    public int countCompletedByStudentId(Long studentId) throws SQLException {
        if (studentId == null) return 0;
        String sql = "SELECT COUNT(*) FROM student_material_progress WHERE student_id = ? AND is_completed = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public boolean isCompleted(Long studentId, Long materialId) throws SQLException {
        if (studentId == null || materialId == null) return false;
        String sql = "SELECT is_completed FROM student_material_progress WHERE student_id = ? AND material_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, studentId);
            stmt.setLong(2, materialId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        return false;
    }

    public Set<Long> getCompletedMaterialIds(Long studentId, Long topicId) throws SQLException {
        Set<Long> set = new HashSet<>();
        if (studentId == null || topicId == null) return set;
        String sql = "SELECT smp.material_id FROM student_material_progress smp " +
                     "JOIN study_materials sm ON smp.material_id = sm.id " +
                     "WHERE smp.student_id = ? AND sm.topic_id = ? AND smp.is_completed = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, studentId);
            stmt.setLong(2, topicId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getLong(1));
                }
            }
        }
        return set;
    }

    private StudyMaterial mapResultSetToStudyMaterial(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        Long topicId = rs.getLong("topic_id");
        String title = rs.getString("title");
        String content = rs.getString("content");
        String resourceUrl = rs.getString("resource_url");
        String attachPath = rs.getString("attachment_path");
        String attachType = rs.getString("attachment_type");
        int order = rs.getInt("display_order");
        boolean published = rs.getBoolean("is_published");
        Long createdBy = rs.getObject("created_by", Long.class);
        Timestamp cAt = rs.getTimestamp("created_at");
        Timestamp uAt = rs.getTimestamp("updated_at");

        LocalDateTime createdAt = (cAt != null) ? cAt.toLocalDateTime() : null;
        LocalDateTime updatedAt = (uAt != null) ? uAt.toLocalDateTime() : null;

        return new StudyMaterial(id, topicId, title, content, resourceUrl, attachPath, attachType,
                order, published, createdBy, createdAt, updatedAt);
    }
}
