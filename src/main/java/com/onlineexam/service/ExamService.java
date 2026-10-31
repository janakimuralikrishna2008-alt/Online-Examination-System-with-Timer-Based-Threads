package com.onlineexam.service;

import com.onlineexam.dao.ExamDAO;
import com.onlineexam.dao.QuestionDAO;
import com.onlineexam.model.Exam;
import com.onlineexam.model.Question;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Business service managing examinations and questions.
 * Coordinates {@link ExamDAO} and {@link QuestionDAO}.
 */
public class ExamService {

    private static final Logger LOGGER = Logger.getLogger(ExamService.class.getName());

    private final ExamDAO examDAO;
    private final QuestionDAO questionDAO;

    public ExamService() {
        this(new ExamDAO(), new QuestionDAO());
    }

    public ExamService(ExamDAO examDAO, QuestionDAO questionDAO) {
        if (examDAO == null || questionDAO == null) {
            throw new IllegalArgumentException("DAOs cannot be null.");
        }
        this.examDAO = examDAO;
        this.questionDAO = questionDAO;
    }

    /**
     * Retrieves all exams currently marked active for student attempts.
     */
    public List<Exam> getAvailableExams() throws SQLException {
        return examDAO.findAllActive();
    }

    /**
     * Retrieves all exams in the catalog (including inactive ones, for admin management).
     */
    public List<Exam> getAllExams() throws SQLException {
        return examDAO.findAll();
    }

    /**
     * Retrieves an exam by its primary key.
     */
    public Optional<Exam> getExamById(Long examId) throws SQLException {
        if (examId == null || examId <= 0) {
            return Optional.empty();
        }
        return examDAO.findById(examId);
    }

    /**
     * Retrieves questions configured for a specific exam.
     */
    public List<Question> getExamQuestions(Long examId) throws SQLException {
        if (examId == null || examId <= 0) {
            return List.of();
        }
        return questionDAO.findByExamId(examId);
    }

    public List<Exam> getExamsBySubject(Long subjectId) throws SQLException {
        return examDAO.findBySubjectId(subjectId);
    }

    public List<Exam> getExamsByTopic(Long topicId) throws SQLException {
        return examDAO.findActiveByTopicId(topicId);
    }

    public List<Exam> getPracticeQuizzesByTopic(Long topicId) throws SQLException {
        return examDAO.findPracticeQuizzesByTopicId(topicId);
    }

    public List<Exam> getFormalExamsByTopic(Long topicId) throws SQLException {
        return examDAO.findFormalExamsByTopicId(topicId);
    }

    public List<Question> getAllQuestions() throws SQLException {
        return questionDAO.findAll();
    }

    public List<Question> getQuestionsByTopic(Long topicId) throws SQLException {
        return questionDAO.findByTopicId(topicId);
    }

    public Optional<Question> getQuestionById(Long questionId) throws SQLException {
        return questionDAO.findById(questionId);
    }

    /**
     * Validates that an exam's configuration adheres to business constraints:
     * - Title not empty
     * - Duration > 0
     * - Total marks > 0
     * - Passing marks between 0 and total marks
     */
    public void validateExamConfiguration(Exam exam) {
        if (exam == null) {
            throw new IllegalArgumentException("Exam entity cannot be null.");
        }
        exam.validate();
    }

    /**
     * Checks whether an examination is currently eligible for student attempts.
     * An exam is eligible if:
     * 1. It exists in the database.
     * 2. It is marked active (is_active = true).
     * 3. It contains at least one configured question.
     *
     * @param examId the exam ID to verify
     * @return the verified {@link Exam} entity
     * @throws IllegalArgumentException if exam does not exist
     * @throws IllegalStateException if exam is inactive or has no questions
     */
    public Exam assertExamEligibleForAttempt(Long examId) throws SQLException {
        if (examId == null || examId <= 0) {
            throw new IllegalArgumentException("Invalid exam ID: " + examId);
        }

        Exam exam = examDAO.findById(examId).orElseThrow(() ->
                new IllegalArgumentException("Exam not found with ID: " + examId));

        if (!exam.isActive()) {
            throw new IllegalStateException("Exam '" + exam.getTitle() + "' (ID: " + examId + ") is currently inactive.");
        }

        int questionCount = questionDAO.countByExamId(examId);
        if (questionCount == 0) {
            // Also check list as fallback if count is not available
            List<Question> questions = questionDAO.findByExamId(examId);
            if (questions.isEmpty()) {
                throw new IllegalStateException("Exam '" + exam.getTitle() + "' (ID: " + examId + ") has no questions configured.");
            }
        }

        return exam;
    }

    /**
     * Creates a new examination in the catalog.
     */
    public Exam createExam(Exam exam) throws java.sql.SQLException {
        validateExamConfiguration(exam);
        return examDAO.create(exam);
    }

    /**
     * Updates an existing examination.
     */
    public boolean updateExam(Exam exam) throws java.sql.SQLException {
        validateExamConfiguration(exam);
        return examDAO.update(exam);
    }

    /**
     * Deletes an examination by ID.
     */
    public boolean deleteExam(Long examId) throws java.sql.SQLException {
        if (examId == null || examId <= 0) return false;
        return examDAO.delete(examId);
    }

    /**
     * Creates a new question for an exam.
     */
    public Question createQuestion(Question question) throws java.sql.SQLException {
        if (question == null) throw new IllegalArgumentException("Question cannot be null.");
        question.validate();
        return questionDAO.create(question);
    }

    /**
     * Updates an existing question.
     */
    public boolean updateQuestion(Question question) throws java.sql.SQLException {
        if (question == null) throw new IllegalArgumentException("Question cannot be null.");
        question.validate();
        return questionDAO.update(question);
    }

    /**
     * Deletes a question by ID.
     */
    public boolean deleteQuestion(Long questionId) throws java.sql.SQLException {
        if (questionId == null || questionId <= 0) return false;
        return questionDAO.delete(questionId);
    }
}
