package com.onlineexam.dao;

import com.onlineexam.config.DatabaseConnection;
import com.onlineexam.model.Question;
import com.onlineexam.model.StudentAnswer;
import com.onlineexam.model.StudentExam;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object for {@link StudentAnswer} entity operations.
 * Includes transaction-safe multi-step exam submission workflows.
 */
public class StudentAnswerDAO {

    private static final Logger LOGGER = Logger.getLogger(StudentAnswerDAO.class.getName());

    public List<StudentAnswer> findByStudentExamId(Long studentExamId) throws SQLException {
        List<StudentAnswer> list = new ArrayList<>();
        if (studentExamId == null) return list;
        String sql = "SELECT id, student_exam_id, question_id, selected_option, is_correct, answered_at " +
                     "FROM student_answers WHERE student_exam_id = ? ORDER BY question_id ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, studentExamId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToStudentAnswer(rs));
                }
            }
        }
        return list;
    }

    public Optional<StudentAnswer> findByStudentExamAndQuestion(Long studentExamId, Long questionId) throws SQLException {
        if (studentExamId == null || questionId == null) return Optional.empty();
        String sql = "SELECT id, student_exam_id, question_id, selected_option, is_correct, answered_at " +
                     "FROM student_answers WHERE student_exam_id = ? AND question_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, studentExamId);
            stmt.setLong(2, questionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToStudentAnswer(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean saveOrUpdate(StudentAnswer answer) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return saveOrUpdate(answer, conn);
        }
    }

    public boolean saveOrUpdate(StudentAnswer answer, Connection conn) throws SQLException {
        answer.validate();
        String sql = "INSERT INTO student_answers (student_exam_id, question_id, selected_option, is_correct) " +
                     "VALUES (?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE selected_option = VALUES(selected_option), is_correct = VALUES(is_correct)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, answer.getStudentExamId());
            stmt.setLong(2, answer.getQuestionId());
            if (answer.getSelectedOption() != null) {
                stmt.setString(3, answer.getSelectedOption().name());
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }
            stmt.setBoolean(4, answer.isCorrect());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Executes a complete transactional exam submission:
     * 1. Evaluates all answers against exam question key.
     * 2. Atomically persists student responses via PreparedStatement batch.
     * 3. Calculates total accumulated score.
     * 4. Updates student exam session with end_time, total_score, and final status (SUBMITTED / AUTO_SUBMITTED).
     * Rolls back all changes if any error occurs.
     *
     * @param studentExamId the active student exam session ID
     * @param answers list of student chosen options
     * @param questions the reference questions for scoring
     * @param finalStatus either SUBMITTED or AUTO_SUBMITTED
     * @return the calculated total score
     * @throws SQLException if transaction fails and is rolled back
     */
    public int submitExamSessionTransactional(Long studentExamId,
                                              List<StudentAnswer> answers,
                                              List<Question> questions,
                                              StudentExam.ExamStatus finalStatus) throws SQLException {

        if (studentExamId == null) {
            throw new IllegalArgumentException("StudentExamId cannot be null.");
        }

        Map<Long, Question> questionMap = new HashMap<>();
        for (Question q : questions) {
            questionMap.put(q.getId(), q);
        }

        int calculatedScore = 0;
        for (StudentAnswer ans : answers) {
            Question q = questionMap.get(ans.getQuestionId());
            if (q != null && ans.getSelectedOption() != null) {
                boolean correct = q.isCorrect(ans.getSelectedOption());
                ans.setCorrect(correct);
                if (correct) {
                    calculatedScore += q.getMarks();
                }
            } else {
                ans.setCorrect(false);
            }
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // 1. Batch upsert answers
            String answerSql = "INSERT INTO student_answers (student_exam_id, question_id, selected_option, is_correct) " +
                               "VALUES (?, ?, ?, ?) " +
                               "ON DUPLICATE KEY UPDATE selected_option = VALUES(selected_option), is_correct = VALUES(is_correct)";

            try (PreparedStatement stmt = conn.prepareStatement(answerSql)) {
                for (StudentAnswer ans : answers) {
                    stmt.setLong(1, studentExamId);
                    stmt.setLong(2, ans.getQuestionId());
                    if (ans.getSelectedOption() != null) {
                        stmt.setString(3, ans.getSelectedOption().name());
                    } else {
                        stmt.setNull(3, Types.VARCHAR);
                    }
                    stmt.setBoolean(4, ans.isCorrect());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            // 2. Finalize exam session record atomically
            String sessionSql = "UPDATE student_exams SET total_score = ?, status = ?, time_remaining_seconds = 0, " +
                                "end_time = CURRENT_TIMESTAMP, submitted_at = CURRENT_TIMESTAMP " +
                                "WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sessionSql)) {
                stmt.setInt(1, calculatedScore);
                stmt.setString(2, finalStatus != null ? finalStatus.name() : StudentExam.ExamStatus.SUBMITTED.name());
                stmt.setLong(3, studentExamId);
                stmt.executeUpdate();
            }

            conn.commit(); // Commit Transaction
            LOGGER.info(String.format("Exam session %d submitted successfully with score %d", studentExamId, calculatedScore));
            return calculatedScore;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on failure
                    LOGGER.log(Level.WARNING, "Transaction rolled back for exam session: " + studentExamId, e);
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "Rollback failed", ex);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.FINE, "Error closing connection", ex);
                }
            }
        }
    }

    private StudentAnswer mapResultSetToStudentAnswer(ResultSet rs) throws SQLException {
        Timestamp answeredTs = rs.getTimestamp("answered_at");

        return new StudentAnswer(
                rs.getLong("id"),
                rs.getLong("student_exam_id"),
                rs.getLong("question_id"),
                Question.Option.fromString(rs.getString("selected_option")),
                rs.getBoolean("is_correct"),
                answeredTs != null ? answeredTs.toLocalDateTime() : null
        );
    }
}
