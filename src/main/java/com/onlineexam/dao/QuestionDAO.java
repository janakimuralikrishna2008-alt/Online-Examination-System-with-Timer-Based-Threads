package com.onlineexam.dao;

import com.onlineexam.config.DatabaseConnection;
import com.onlineexam.model.Question;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Data Access Object for {@link Question} entity operations.
 */
public class QuestionDAO {

    private static final Logger LOGGER = Logger.getLogger(QuestionDAO.class.getName());

    public Optional<Question> findById(Long id) throws SQLException {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, exam_id, topic_id, question_text, option_a, option_b, option_c, option_d, " +
                     "correct_option, marks, explanation, is_published, created_at FROM questions WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToQuestion(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Question> findAll() throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT id, exam_id, topic_id, question_text, option_a, option_b, option_c, option_d, " +
                     "correct_option, marks, explanation, is_published, created_at FROM questions ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToQuestion(rs));
            }
        }
        return list;
    }

    public List<Question> findByExamId(Long examId) throws SQLException {
        List<Question> list = new ArrayList<>();
        if (examId == null) return list;
        String sql = "SELECT id, exam_id, topic_id, question_text, option_a, option_b, option_c, option_d, " +
                     "correct_option, marks, explanation, is_published, created_at FROM questions " +
                     "WHERE exam_id = ? ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToQuestion(rs));
                }
            }
        }
        return list;
    }

    public List<Question> findPublishedByExamId(Long examId) throws SQLException {
        List<Question> list = new ArrayList<>();
        if (examId == null) return list;
        String sql = "SELECT id, exam_id, topic_id, question_text, option_a, option_b, option_c, option_d, " +
                     "correct_option, marks, explanation, is_published, created_at FROM questions " +
                     "WHERE exam_id = ? AND is_published = TRUE ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToQuestion(rs));
                }
            }
        }
        return list;
    }

    public List<Question> findByTopicId(Long topicId) throws SQLException {
        List<Question> list = new ArrayList<>();
        if (topicId == null) return list;
        String sql = "SELECT id, exam_id, topic_id, question_text, option_a, option_b, option_c, option_d, " +
                     "correct_option, marks, explanation, is_published, created_at FROM questions " +
                     "WHERE topic_id = ? ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, topicId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToQuestion(rs));
                }
            }
        }
        return list;
    }

    public List<Question> findPublishedByTopicId(Long topicId) throws SQLException {
        List<Question> list = new ArrayList<>();
        if (topicId == null) return list;
        String sql = "SELECT id, exam_id, topic_id, question_text, option_a, option_b, option_c, option_d, " +
                     "correct_option, marks, explanation, is_published, created_at FROM questions " +
                     "WHERE topic_id = ? AND is_published = TRUE ORDER BY id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, topicId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToQuestion(rs));
                }
            }
        }
        return list;
    }

    public int countByExamId(Long examId) throws SQLException {
        if (examId == null) return 0;
        String sql = "SELECT COUNT(*) FROM questions WHERE exam_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, examId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public Question create(Question question) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return create(question, conn);
        }
    }

    public Question create(Question question, Connection conn) throws SQLException {
        question.validate();
        String sql = "INSERT INTO questions (exam_id, topic_id, question_text, option_a, option_b, option_c, " +
                     "option_d, correct_option, marks, explanation, is_published) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, question.getExamId());
            if (question.getTopicId() != null) {
                stmt.setLong(2, question.getTopicId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setString(3, question.getQuestionText());
            stmt.setString(4, question.getOptionA());
            stmt.setString(5, question.getOptionB());
            stmt.setString(6, question.getOptionC());
            stmt.setString(7, question.getOptionD());
            stmt.setString(8, question.getCorrectOption().name());
            stmt.setInt(9, question.getMarks());
            stmt.setString(10, question.getExplanation());
            stmt.setBoolean(11, question.isPublished());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating question failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    question.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating question failed, no ID obtained.");
                }
            }
            return question;
        }
    }

    public void createBatch(List<Question> questions, Connection conn) throws SQLException {
        if (questions == null || questions.isEmpty()) return;
        String sql = "INSERT INTO questions (exam_id, topic_id, question_text, option_a, option_b, option_c, " +
                     "option_d, correct_option, marks, explanation, is_published) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Question q : questions) {
                q.validate();
                stmt.setLong(1, q.getExamId());
                if (q.getTopicId() != null) {
                    stmt.setLong(2, q.getTopicId());
                } else {
                    stmt.setNull(2, Types.BIGINT);
                }
                stmt.setString(3, q.getQuestionText());
                stmt.setString(4, q.getOptionA());
                stmt.setString(5, q.getOptionB());
                stmt.setString(6, q.getOptionC());
                stmt.setString(7, q.getOptionD());
                stmt.setString(8, q.getCorrectOption().name());
                stmt.setInt(9, q.getMarks());
                stmt.setString(10, q.getExplanation());
                stmt.setBoolean(11, q.isPublished());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public boolean update(Question question) throws SQLException {
        question.validate();
        if (question.getId() == null) {
            throw new IllegalArgumentException("Question ID is required for update.");
        }
        String sql = "UPDATE questions SET exam_id = ?, topic_id = ?, question_text = ?, option_a = ?, " +
                     "option_b = ?, option_c = ?, option_d = ?, correct_option = ?, marks = ?, explanation = ?, " +
                     "is_published = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, question.getExamId());
            if (question.getTopicId() != null) {
                stmt.setLong(2, question.getTopicId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setString(3, question.getQuestionText());
            stmt.setString(4, question.getOptionA());
            stmt.setString(5, question.getOptionB());
            stmt.setString(6, question.getOptionC());
            stmt.setString(7, question.getOptionD());
            stmt.setString(8, question.getCorrectOption().name());
            stmt.setInt(9, question.getMarks());
            stmt.setString(10, question.getExplanation());
            stmt.setBoolean(11, question.isPublished());
            stmt.setLong(12, question.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(Long id) throws SQLException {
        if (id == null) return false;
        String sql = "DELETE FROM questions WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private Question mapResultSetToQuestion(ResultSet rs) throws SQLException {
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        Long topicId = rs.getObject("topic_id", Long.class);
        boolean published = rs.getBoolean("is_published");

        return new Question(
                rs.getLong("id"),
                rs.getLong("exam_id"),
                topicId,
                rs.getString("question_text"),
                rs.getString("option_a"),
                rs.getString("option_b"),
                rs.getString("option_c"),
                rs.getString("option_d"),
                Question.Option.fromString(rs.getString("correct_option")),
                rs.getInt("marks"),
                rs.getString("explanation"),
                published,
                createdAtTs != null ? createdAtTs.toLocalDateTime() : null
        );
    }
}
