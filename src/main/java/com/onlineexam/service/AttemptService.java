package com.onlineexam.service;

import com.onlineexam.dao.StudentAnswerDAO;
import com.onlineexam.dao.StudentExamDAO;
import com.onlineexam.model.*;
import com.onlineexam.timer.ExamTimerListener;
import com.onlineexam.timer.ExamTimerService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Business service managing candidate examination attempts, answer recording,
 * and safe transaction-backed submission.
 */
public class AttemptService {

    private static final Logger LOGGER = Logger.getLogger(AttemptService.class.getName());

    // Fine-grained lock registry per attempt ID to prevent race conditions during submission
    private static final Map<Long, Object> ATTEMPT_LOCKS = new ConcurrentHashMap<>();

    private final StudentExamDAO studentExamDAO;
    private final StudentAnswerDAO studentAnswerDAO;
    private final ExamService examService;
    private final EvaluationService evaluationService;

    public AttemptService() {
        this(new StudentExamDAO(), new StudentAnswerDAO(), new ExamService(), new EvaluationService());
    }

    public AttemptService(StudentExamDAO studentExamDAO,
                          StudentAnswerDAO studentAnswerDAO,
                          ExamService examService,
                          EvaluationService evaluationService) {
        if (studentExamDAO == null || studentAnswerDAO == null || examService == null || evaluationService == null) {
            throw new IllegalArgumentException("Dependencies cannot be null.");
        }
        this.studentExamDAO = studentExamDAO;
        this.studentAnswerDAO = studentAnswerDAO;
        this.examService = examService;
        this.evaluationService = evaluationService;
    }

    /**
     * Starts a new examination attempt for a student or resumes an existing in-progress attempt.
     *
     * @param studentId the candidate's ID
     * @param examId the target exam ID
     * @return active {@link StudentExam} session
     * @throws IllegalStateException if the student already completed this exam
     */
    public StudentExam startAttempt(Long studentId, Long examId) throws SQLException {
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("Invalid student ID: " + studentId);
        }

        // Validate exam eligibility (active and has questions)
        Exam exam = examService.assertExamEligibleForAttempt(examId);

        // Check if candidate already has an attempt on file
        Optional<StudentExam> existingOpt = studentExamDAO.findByStudentAndExam(studentId, examId);
        if (existingOpt.isPresent()) {
            StudentExam existing = existingOpt.get();
            if (existing.isCompleted()) {
                throw new IllegalStateException("Student has already completed this exam attempt (Status: " +
                        existing.getStatus() + "). Retakes are not permitted.");
            }
            if (existing.isInProgress()) {
                // Authoritative deadline verification prevents deadline reset
                if (existing.getStartTime() != null) {
                    long elapsedSeconds = java.time.Duration.between(existing.getStartTime(), LocalDateTime.now()).getSeconds();
                    long durationSeconds = exam.getDurationSeconds();
                    long remainingAuthoritative = durationSeconds - elapsedSeconds;

                    if (remainingAuthoritative <= 0) {
                        LOGGER.warning("Attempt " + existing.getId() + " deadline expired while candidate was offline. Auto-submitting...");
                        submitAttempt(existing.getId(), Collections.emptyMap(), true);
                        throw new IllegalStateException("The examination deadline has expired. Your attempt has been automatically submitted.");
                    } else {
                        int adjustedRemaining = (int) Math.min(existing.getTimeRemainingSeconds(), remainingAuthoritative);
                        existing.setTimeRemainingSeconds(adjustedRemaining);
                        try {
                            studentExamDAO.updateTimer(existing.getId(), adjustedRemaining);
                        } catch (Exception ignored) {}
                    }
                }
                LOGGER.info("Resuming active exam attempt " + existing.getId() + " for student " + studentId);
                return existing;
            }
        }

        // Initialize new in-progress attempt
        StudentExam newAttempt = new StudentExam();
        newAttempt.setStudentId(studentId);
        newAttempt.setExamId(examId);
        newAttempt.setStartTime(LocalDateTime.now());
        newAttempt.setTimeRemainingSeconds(exam.getDurationSeconds());
        newAttempt.setStatus(StudentExam.ExamStatus.IN_PROGRESS);
        newAttempt.setTotalScore(0);

        StudentExam created = studentExamDAO.create(newAttempt);
        LOGGER.info(String.format("Started new exam attempt %d for student %d on exam %d",
                created.getId(), studentId, examId));
        return created;
    }

    /**
     * Retrieves an attempt session by ID.
     */
    public Optional<StudentExam> getAttempt(Long attemptId) throws SQLException {
        if (attemptId == null || attemptId <= 0) {
            return Optional.empty();
        }
        return studentExamDAO.findById(attemptId);
    }

    /**
     * Retrieves all exam attempts for a specific candidate.
     */
    public List<StudentExam> getStudentAttempts(Long studentId) throws SQLException {
        if (studentId == null || studentId <= 0) return Collections.emptyList();
        return studentExamDAO.findByStudentId(studentId);
    }

    /**
     * Retrieves all exam attempts across the entire system for administrative reporting.
     */
    public List<StudentExam> getAllAttempts() throws SQLException {
        return studentExamDAO.findAll();
    }

    /**
     * Saves or updates a student's answer for a specific question during an active exam attempt.
     * Rejects updates if the attempt has already been finalized.
     *
     * @param attemptId exam attempt ID
     * @param questionId question ID
     * @param selectedOption choice (A, B, C, D) or null to clear answer
     * @throws IllegalStateException if the attempt is already completed
     */
    public void saveAnswer(Long attemptId, Long questionId, Question.Option selectedOption) throws SQLException {
        if (attemptId == null || questionId == null) {
            throw new IllegalArgumentException("Attempt ID and Question ID are required.");
        }

        StudentExam attempt = studentExamDAO.findById(attemptId).orElseThrow(() ->
                new IllegalArgumentException("Exam attempt not found: " + attemptId));

        if (attempt.isCompleted()) {
            throw new IllegalStateException("Cannot modify answers: attempt " + attemptId + " is already finalized.");
        }

        StudentAnswer answer = new StudentAnswer();
        answer.setStudentExamId(attemptId);
        answer.setQuestionId(questionId);
        answer.setSelectedOption(selectedOption);
        answer.setCorrect(false); // Unchecked until final evaluation

        studentAnswerDAO.saveOrUpdate(answer);
    }

    /**
     * Retrieves all saved answers for an attempt as a map of question ID to chosen Option.
     */
    public Map<Long, Question.Option> getSavedAnswers(Long attemptId) throws SQLException {
        if (attemptId == null || attemptId <= 0) {
            return Collections.emptyMap();
        }

        List<StudentAnswer> list = studentAnswerDAO.findByStudentExamId(attemptId);
        Map<Long, Question.Option> map = new HashMap<>();
        for (StudentAnswer a : list) {
            if (a.getQuestionId() != null) {
                map.put(a.getQuestionId(), a.getSelectedOption());
            }
        }
        return map;
    }

    /**
     * Retrieves all saved student answer records for an attempt.
     */
    public List<StudentAnswer> getAttemptAnswers(Long attemptId) throws SQLException {
        if (attemptId == null || attemptId <= 0) {
            return Collections.emptyList();
        }
        return studentAnswerDAO.findByStudentExamId(attemptId);
    }

    /**
     * Finalizes and scores an exam attempt transactionally.
     * Guarded against duplicate calls from manual submission and timeout auto-submission.
     *
     * @param attemptId unique exam attempt ID
     * @param latestResponses map of latest user responses (can be empty, will merge with DB)
     * @param isAutoSubmitted true if triggered by countdown timer expiry, false if student clicked submit
     * @return immutable {@link ExamResult} with scoring breakdown
     */
    public ExamResult submitAttempt(Long attemptId,
                                    Map<Long, Question.Option> latestResponses,
                                    boolean isAutoSubmitted) throws SQLException {
        if (attemptId == null || attemptId <= 0) {
            throw new IllegalArgumentException("Invalid attempt ID: " + attemptId);
        }

        Object lock = ATTEMPT_LOCKS.computeIfAbsent(attemptId, k -> new Object());
        synchronized (lock) {
            try {
                StudentExam attempt = studentExamDAO.findById(attemptId).orElseThrow(() ->
                        new IllegalArgumentException("Exam attempt not found: " + attemptId));

                // Guard against duplicate submission
                if (attempt.isCompleted()) {
                    throw new IllegalStateException("Attempt " + attemptId + " has already been submitted (" +
                            attempt.getStatus() + "). Cannot submit twice.");
                }

                Exam exam = examService.getExamById(attempt.getExamId()).orElseThrow(() ->
                        new IllegalArgumentException("Exam definition not found for attempt: " + attemptId));

                List<Question> questions = examService.getExamQuestions(exam.getId());
                if (questions.isEmpty()) {
                    throw new IllegalStateException("Exam " + exam.getId() + " has no questions to evaluate.");
                }

                // Merge DB answers with latest in-memory responses
                Map<Long, Question.Option> mergedAnswers = getSavedAnswers(attemptId);
                if (latestResponses != null) {
                    mergedAnswers.putAll(latestResponses);
                }

                // Server-side strict evaluation
                ExamResult result = evaluationService.evaluate(
                        attemptId,
                        attempt.getStudentId(),
                        exam,
                        questions,
                        mergedAnswers,
                        isAutoSubmitted
                );

                StudentExam.ExamStatus finalStatus = isAutoSubmitted ?
                        StudentExam.ExamStatus.AUTO_SUBMITTED : StudentExam.ExamStatus.SUBMITTED;

                // Transactional persistence: batch upsert answers, update total score and final status
                try {
                    studentAnswerDAO.submitExamSessionTransactional(
                            attemptId,
                            result.evaluatedAnswers(),
                            questions,
                            finalStatus
                    );
                    studentExamDAO.updateScoreAndStatus(attemptId, result.earnedMarks(), finalStatus);
                    studentExamDAO.updateTimer(attemptId, 0);
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Transactional submission failed for attempt " + attemptId, e);
                    throw e;
                }

                LOGGER.info(String.format("Successfully submitted attempt %d. Score: %d/%d (Passed: %b)",
                        attemptId, result.earnedMarks(), result.totalPossibleMarks(), result.passed()));

                return result;

            } finally {
                ATTEMPT_LOCKS.remove(attemptId);
            }
        }
    }

    /**
     * Factory method linking an active attempt to an {@link ExamTimerService} with automated timeout submission.
     */
    public ExamTimerService createAndStartTimer(Long attemptId, ExamTimerListener listener) throws SQLException {
        StudentExam attempt = getAttempt(attemptId).orElseThrow(() ->
                new IllegalArgumentException("Attempt not found: " + attemptId));

        if (attempt.isCompleted()) {
            throw new IllegalStateException("Attempt " + attemptId + " is already completed; cannot start timer.");
        }

        long duration = Math.max(1, attempt.getTimeRemainingSeconds());

        // Background auto-submission task when timer expires
        Runnable autoSubmitTask = () -> {
            try {
                LOGGER.info("Auto-submitting attempt " + attemptId + " via ExamTimerService callback...");
                submitAttempt(attemptId, Collections.emptyMap(), true);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Auto-submission failed for attempt " + attemptId, e);
            }
        };

        ExamTimerService timer = new ExamTimerService(attemptId, duration, studentExamDAO, autoSubmitTask);
        if (listener != null) {
            timer.addListener(listener);
        }
        timer.start();
        return timer;
    }
}
