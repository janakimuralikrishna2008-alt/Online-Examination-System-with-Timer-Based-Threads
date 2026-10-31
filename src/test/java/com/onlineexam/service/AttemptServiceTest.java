package com.onlineexam.service;

import com.onlineexam.dao.ExamDAO;
import com.onlineexam.dao.QuestionDAO;
import com.onlineexam.dao.StudentAnswerDAO;
import com.onlineexam.dao.StudentExamDAO;
import com.onlineexam.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AttemptService} testing attempt lifecycle, answer persistence,
 * duplicate submission guards, and transaction failure handling.
 */
public class AttemptServiceTest {

    private AttemptService attemptService;
    private StubStudentExamDAO stubStudentExamDAO;
    private StubStudentAnswerDAO stubStudentAnswerDAO;
    private StubExamDAO stubExamDAO;
    private StubQuestionDAO stubQuestionDAO;
    private ExamService examService;
    private EvaluationService evaluationService;

    private Exam testExam;
    private List<Question> testQuestions;

    @BeforeEach
    void setUp() {
        stubStudentExamDAO = new StubStudentExamDAO();
        stubStudentAnswerDAO = new StubStudentAnswerDAO();
        stubExamDAO = new StubExamDAO();
        stubQuestionDAO = new StubQuestionDAO();

        examService = new ExamService(stubExamDAO, stubQuestionDAO);
        evaluationService = new EvaluationService();
        attemptService = new AttemptService(stubStudentExamDAO, stubStudentAnswerDAO, examService, evaluationService);

        testExam = new Exam(1L, "Core Java Assessment", "Desc", 15, 10, 5, true, null, null);
        stubExamDAO.add(testExam);

        Question q1 = new Question(1L, 1L, "Question 1?", "A", "B", "C", "D", Question.Option.A, 5, null, null);
        Question q2 = new Question(2L, 1L, "Question 2?", "A", "B", "C", "D", Question.Option.B, 5, null, null);
        testQuestions = List.of(q1, q2);
        stubQuestionDAO.add(q1);
        stubQuestionDAO.add(q2);
    }

    @Test
    @DisplayName("startAttempt creates new in-progress session with correct student and duration")
    void testStartNewAttempt() throws SQLException {
        StudentExam attempt = attemptService.startAttempt(100L, 1L);

        assertNotNull(attempt);
        assertEquals(100L, attempt.getStudentId());
        assertEquals(1L, attempt.getExamId());
        assertEquals(StudentExam.ExamStatus.IN_PROGRESS, attempt.getStatus());
        assertEquals(900, attempt.getTimeRemainingSeconds()); // 15 mins * 60 = 900s
    }

    @Test
    @DisplayName("startAttempt resumes existing IN_PROGRESS attempt without duplicate row creation")
    void testResumeExistingAttempt() throws SQLException {
        StudentExam first = attemptService.startAttempt(100L, 1L);
        StudentExam resumed = attemptService.startAttempt(100L, 1L);

        assertEquals(first.getId(), resumed.getId());
        assertEquals(StudentExam.ExamStatus.IN_PROGRESS, resumed.getStatus());
    }

    @Test
    @DisplayName("startAttempt rejects candidate retakes if attempt is already completed")
    void testRejectsRetakeWhenCompleted() throws SQLException {
        StudentExam attempt = attemptService.startAttempt(100L, 1L);
        attempt.setStatus(StudentExam.ExamStatus.SUBMITTED);
        stubStudentExamDAO.update(attempt);

        Exception ex = assertThrows(IllegalStateException.class, () ->
                attemptService.startAttempt(100L, 1L));
        assertTrue(ex.getMessage().contains("already completed"));
    }

    @Test
    @DisplayName("startAttempt auto-submits and rejects resume if authoritative deadline has expired")
    void testResumeExpiredAttemptTriggersAutoSubmitAndRejects() throws SQLException {
        StudentExam attempt = attemptService.startAttempt(100L, 1L);
        // Simulate candidate started 20 minutes ago on a 15-minute exam
        attempt.setStartTime(LocalDateTime.now().minusMinutes(20));
        stubStudentExamDAO.update(attempt);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                attemptService.startAttempt(100L, 1L));
        assertTrue(ex.getMessage().contains("examination deadline has expired"));

        StudentExam stored = stubStudentExamDAO.findById(attempt.getId()).orElseThrow();
        assertEquals(StudentExam.ExamStatus.AUTO_SUBMITTED, stored.getStatus());
    }

    @Test
    @DisplayName("getAttemptAnswers returns stored answers for historical review")
    void testGetAttemptAnswersReturnsStoredAnswers() throws SQLException {
        StudentExam attempt = attemptService.startAttempt(106L, 1L);
        attemptService.saveAnswer(attempt.getId(), 1L, Question.Option.A);

        List<StudentAnswer> answers = attemptService.getAttemptAnswers(attempt.getId());
        assertNotNull(answers);
        assertEquals(1, answers.size());
        assertEquals(Question.Option.A, answers.get(0).getSelectedOption());
    }

    @Test
    @DisplayName("saveAnswer persists candidate choices and rejects edits after submission")
    void testSaveAnswerAndPreventEditsAfterSubmission() throws SQLException {
        StudentExam attempt = attemptService.startAttempt(101L, 1L);

        // Active attempt allows saving answers
        assertDoesNotThrow(() -> attemptService.saveAnswer(attempt.getId(), 1L, Question.Option.A));
        Map<Long, Question.Option> saved = attemptService.getSavedAnswers(attempt.getId());
        assertEquals(Question.Option.A, saved.get(1L));

        // Submit attempt
        attemptService.submitAttempt(attempt.getId(), Map.of(), false);

        // Further modifications are strictly rejected
        Exception ex = assertThrows(IllegalStateException.class, () ->
                attemptService.saveAnswer(attempt.getId(), 2L, Question.Option.B));
        assertTrue(ex.getMessage().contains("already finalized"));
    }

    @Test
    @DisplayName("Manual submission evaluates answers, updates score, and sets SUBMITTED status")
    void testManualSubmission() throws SQLException {
        StudentExam attempt = attemptService.startAttempt(102L, 1L);
        attemptService.saveAnswer(attempt.getId(), 1L, Question.Option.A); // 5 marks

        Map<Long, Question.Option> latest = Map.of(2L, Question.Option.B); // 5 marks
        ExamResult result = attemptService.submitAttempt(attempt.getId(), latest, false);

        assertEquals(10, result.earnedMarks());
        assertEquals(100.0, result.percentage());
        assertTrue(result.passed());
        assertFalse(result.autoSubmitted());

        StudentExam stored = stubStudentExamDAO.findById(attempt.getId()).orElseThrow();
        assertEquals(StudentExam.ExamStatus.SUBMITTED, stored.getStatus());
        assertEquals(10, stored.getTotalScore());
    }

    @Test
    @DisplayName("Automatic timeout submission sets AUTO_SUBMITTED status")
    void testAutomaticTimeoutSubmission() throws SQLException {
        StudentExam attempt = attemptService.startAttempt(103L, 1L);
        ExamResult result = attemptService.submitAttempt(attempt.getId(), Map.of(), true);

        assertTrue(result.autoSubmitted());
        StudentExam stored = stubStudentExamDAO.findById(attempt.getId()).orElseThrow();
        assertEquals(StudentExam.ExamStatus.AUTO_SUBMITTED, stored.getStatus());
    }

    @Test
    @DisplayName("Duplicate submission guard strictly prevents multiple submissions")
    void testDuplicateSubmissionPrevention() throws SQLException {
        StudentExam attempt = attemptService.startAttempt(104L, 1L);

        // First submission succeeds
        assertDoesNotThrow(() -> attemptService.submitAttempt(attempt.getId(), Map.of(), false));

        // Second submission attempt is rejected
        Exception ex = assertThrows(IllegalStateException.class, () ->
                attemptService.submitAttempt(attempt.getId(), Map.of(), true));
        assertTrue(ex.getMessage().contains("already been submitted"));
    }

    @Test
    @DisplayName("Transaction failure prevents state corruption and propagates exception")
    void testTransactionFailureRollsBack() throws SQLException {
        StudentExam attempt = attemptService.startAttempt(105L, 1L);
        stubStudentAnswerDAO.setFailOnSubmit(true);

        assertThrows(SQLException.class, () ->
                attemptService.submitAttempt(attempt.getId(), Map.of(), false));

        // Attempt should remain IN_PROGRESS since transaction failed
        StudentExam stored = stubStudentExamDAO.findById(attempt.getId()).orElseThrow();
        assertEquals(StudentExam.ExamStatus.IN_PROGRESS, stored.getStatus());
    }

    // In-memory test stubs
    private static class StubStudentExamDAO extends StudentExamDAO {
        private final Map<Long, StudentExam> attempts = new HashMap<>();
        private long idGen = 1000;

        @Override
        public Optional<StudentExam> findById(Long id) {
            return Optional.ofNullable(attempts.get(id));
        }

        @Override
        public Optional<StudentExam> findByStudentAndExam(Long studentId, Long examId) {
            return attempts.values().stream()
                    .filter(a -> Objects.equals(a.getStudentId(), studentId) && Objects.equals(a.getExamId(), examId))
                    .findFirst();
        }

        @Override
        public StudentExam create(StudentExam studentExam) {
            studentExam.setId(++idGen);
            attempts.put(studentExam.getId(), studentExam);
            return studentExam;
        }

        public void update(StudentExam attempt) {
            attempts.put(attempt.getId(), attempt);
        }

        @Override
        public boolean updateScoreAndStatus(Long id, int totalScore, StudentExam.ExamStatus status) {
            StudentExam a = attempts.get(id);
            if (a != null) {
                a.setTotalScore(totalScore);
                a.setStatus(status);
                return true;
            }
            return false;
        }

        @Override
        public boolean updateStatus(Long id, StudentExam.ExamStatus status) {
            StudentExam a = attempts.get(id);
            if (a != null) {
                a.setStatus(status);
                return true;
            }
            return false;
        }

        @Override
        public boolean updateTimer(Long id, int remaining) {
            StudentExam a = attempts.get(id);
            if (a != null) {
                a.setTimeRemainingSeconds(remaining);
                return true;
            }
            return false;
        }
    }

    private static class StubStudentAnswerDAO extends StudentAnswerDAO {
        private final List<StudentAnswer> answers = new ArrayList<>();
        private boolean failOnSubmit = false;

        public void setFailOnSubmit(boolean fail) {
            this.failOnSubmit = fail;
        }

        @Override
        public boolean saveOrUpdate(StudentAnswer answer) {
            answers.removeIf(a -> Objects.equals(a.getStudentExamId(), answer.getStudentExamId()) &&
                                  Objects.equals(a.getQuestionId(), answer.getQuestionId()));
            answers.add(answer);
            return true;
        }

        @Override
        public List<StudentAnswer> findByStudentExamId(Long examId) {
            return answers.stream().filter(a -> Objects.equals(a.getStudentExamId(), examId)).toList();
        }

        @Override
        public int submitExamSessionTransactional(Long studentExamId,
                                                  List<StudentAnswer> newAnswers,
                                                  List<Question> questions,
                                                  StudentExam.ExamStatus finalStatus) throws SQLException {
            if (failOnSubmit) {
                throw new SQLException("Simulated transaction commit failure in database.");
            }
            // Upsert answers
            for (StudentAnswer a : newAnswers) {
                saveOrUpdate(a);
            }
            return 10;
        }
    }

    private static class StubExamDAO extends ExamDAO {
        private final Map<Long, Exam> exams = new HashMap<>();

        void add(Exam exam) {
            exams.put(exam.getId(), exam);
        }

        @Override
        public Optional<Exam> findById(Long id) {
            return Optional.ofNullable(exams.get(id));
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
