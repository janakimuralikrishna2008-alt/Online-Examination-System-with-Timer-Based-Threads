package com.onlineexam.service;

import com.onlineexam.dao.ExamDAO;
import com.onlineexam.dao.QuestionDAO;
import com.onlineexam.model.Exam;
import com.onlineexam.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ExamService} testing exam cataloging, eligibility, and configuration validation.
 */
public class ExamServiceTest {

    private ExamService examService;
    private StubExamDAO stubExamDAO;
    private StubQuestionDAO stubQuestionDAO;

    @BeforeEach
    void setUp() {
        stubExamDAO = new StubExamDAO();
        stubQuestionDAO = new StubQuestionDAO();
        examService = new ExamService(stubExamDAO, stubQuestionDAO);
    }

    @Test
    @DisplayName("getAvailableExams returns only active examinations")
    void testGetAvailableExams() throws Exception {
        Exam active1 = new Exam(1L, "Active Exam 1", "Desc", 10, 50, 20, true, null, null);
        Exam active2 = new Exam(2L, "Active Exam 2", "Desc", 15, 60, 25, true, null, null);
        Exam inactive = new Exam(3L, "Archived Exam", "Desc", 20, 100, 40, false, null, null);

        stubExamDAO.add(active1);
        stubExamDAO.add(active2);
        stubExamDAO.add(inactive);

        List<Exam> available = examService.getAvailableExams();
        assertEquals(2, available.size());
        assertTrue(available.contains(active1));
        assertTrue(available.contains(active2));
        assertFalse(available.contains(inactive));
    }

    @Test
    @DisplayName("getAvailableExams returns empty list when catalog is genuinely empty")
    void testEmptyCatalogReturnsEmptyList() throws Exception {
        List<Exam> available = examService.getAvailableExams();
        assertNotNull(available);
        assertTrue(available.isEmpty());
    }

    @Test
    @DisplayName("getAvailableExams propagates SQLException when database fails")
    void testGetAvailableExamsPropagatesSQLException() {
        stubExamDAO.setSimulateDatabaseFailure(true);
        assertThrows(java.sql.SQLException.class, () -> examService.getAvailableExams());
    }

    @Test
    @DisplayName("assertExamEligible succeeds for active exam with configured questions")
    void testEligibleExamSuccess() {
        Exam active = new Exam(10L, "Java 21 Fundamentals", "Desc", 10, 50, 20, true, null, null);
        stubExamDAO.add(active);

        Question q = new Question(1L, 10L, "Q1?", "A", "B", "C", "D", Question.Option.A, 1, null, null);
        stubQuestionDAO.add(q);

        Exam verified = assertDoesNotThrow(() -> examService.assertExamEligibleForAttempt(10L));
        assertEquals(active, verified);
    }

    @Test
    @DisplayName("assertExamEligible throws IllegalArgumentException when exam does not exist")
    void testExamNotFound() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                examService.assertExamEligibleForAttempt(999L));
        assertTrue(ex.getMessage().contains("Exam not found"));
    }

    @Test
    @DisplayName("assertExamEligible throws IllegalStateException when exam is inactive")
    void testInactiveExamThrows() {
        Exam inactive = new Exam(20L, "Draft Exam", "Desc", 10, 50, 20, false, null, null);
        stubExamDAO.add(inactive);

        Exception ex = assertThrows(IllegalStateException.class, () ->
                examService.assertExamEligibleForAttempt(20L));
        assertTrue(ex.getMessage().contains("inactive"));
    }

    @Test
    @DisplayName("assertExamEligible throws IllegalStateException when exam has 0 questions")
    void testExamWithoutQuestionsThrows() {
        Exam activeNoQuestions = new Exam(30L, "Empty Exam", "Desc", 10, 50, 20, true, null, null);
        stubExamDAO.add(activeNoQuestions);
        // Note: no questions added to stubQuestionDAO for exam 30

        Exception ex = assertThrows(IllegalStateException.class, () ->
                examService.assertExamEligibleForAttempt(30L));
        assertTrue(ex.getMessage().contains("no questions configured"));
    }

    @Test
    @DisplayName("validateExamConfiguration catches invalid durations and passing marks")
    void testValidateExamConfiguration() {
        Exam invalidDuration = new Exam("Bad", "Desc", 0, 100, 40, true);
        assertThrows(IllegalArgumentException.class, () ->
                examService.validateExamConfiguration(invalidDuration));

        Exam invalidPassingMarks = new Exam("Bad", "Desc", 15, 100, 150, true);
        assertThrows(IllegalArgumentException.class, () ->
                examService.validateExamConfiguration(invalidPassingMarks));
    }

    // In-memory test stubs
    private static class StubExamDAO extends ExamDAO {
        private final Map<Long, Exam> store = new HashMap<>();
        private boolean simulateDatabaseFailure = false;

        void add(Exam exam) {
            store.put(exam.getId(), exam);
        }

        void setSimulateDatabaseFailure(boolean fail) {
            this.simulateDatabaseFailure = fail;
        }

        @Override
        public Optional<Exam> findById(Long id) throws java.sql.SQLException {
            if (simulateDatabaseFailure) {
                throw new java.sql.SQLException("Simulated database failure: connection refused");
            }
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Exam> findAllActive() throws java.sql.SQLException {
            if (simulateDatabaseFailure) {
                throw new java.sql.SQLException("Simulated database failure: connection refused");
            }
            return store.values().stream().filter(Exam::isActive).toList();
        }

        @Override
        public List<Exam> findAll() throws java.sql.SQLException {
            if (simulateDatabaseFailure) {
                throw new java.sql.SQLException("Simulated database failure: connection refused");
            }
            return new ArrayList<>(store.values());
        }
    }

    private static class StubQuestionDAO extends QuestionDAO {
        private final List<Question> questions = new ArrayList<>();

        void add(Question q) {
            questions.add(q);
        }

        @Override
        public List<Question> findByExamId(Long examId) {
            return questions.stream().filter(q -> Objects.equals(q.getExamId(), examId)).toList();
        }

        @Override
        public int countByExamId(Long examId) {
            return (int) questions.stream().filter(q -> Objects.equals(q.getExamId(), examId)).count();
        }
    }
}
