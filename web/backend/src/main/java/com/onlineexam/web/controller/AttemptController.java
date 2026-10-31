package com.onlineexam.web.controller;

import com.onlineexam.dao.*;
import com.onlineexam.model.*;
import com.onlineexam.service.AttemptService;
import com.onlineexam.service.EvaluationService;
import com.onlineexam.web.dto.AttemptDto.*;
import com.onlineexam.web.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import com.onlineexam.web.service.ServerExamTimerManager;

@RestController
@RequestMapping("/api/attempts")
public class AttemptController {

    private final AttemptService attemptService;
    private final EvaluationService evaluationService;
    private final StudentExamDAO studentExamDAO;
    private final ExamDAO examDAO;
    private final QuestionDAO questionDAO;
    private final SubjectDAO subjectDAO;
    private final ServerExamTimerManager serverExamTimerManager;

    public AttemptController(AttemptService attemptService, EvaluationService evaluationService,
                             StudentExamDAO studentExamDAO, ExamDAO examDAO, QuestionDAO questionDAO,
                             SubjectDAO subjectDAO, ServerExamTimerManager serverExamTimerManager) {
        this.attemptService = attemptService;
        this.evaluationService = evaluationService;
        this.studentExamDAO = studentExamDAO;
        this.examDAO = examDAO;
        this.questionDAO = questionDAO;
        this.subjectDAO = subjectDAO;
        this.serverExamTimerManager = serverExamTimerManager;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startAttempt(
            @RequestParam(required = false) Long examId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            Long studentId = principal.id();

            Long targetExamId = examId;
            if (targetExamId == null && body != null && body.get("examId") != null) {
                targetExamId = Long.valueOf(body.get("examId").toString());
            }

            if (targetExamId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", "examId is required."));
            }

            StudentExam attempt = attemptService.startAttempt(studentId, targetExamId);
            Exam exam = examDAO.findById(targetExamId).orElseThrow(() -> new IllegalStateException("Exam not found"));

            // Register session in multi-threaded server timer manager
            serverExamTimerManager.registerSessionTimer(attempt.getId(), exam.getDurationMinutes(), attempt.getStartTime());

            LocalDateTime deadline = attempt.getStartTime().plusMinutes(exam.getDurationMinutes());
            long remainingSeconds = serverExamTimerManager.getRemainingSeconds(attempt, exam);

            String subjectName = (exam.getSubjectId() != null)
                    ? subjectDAO.findById(exam.getSubjectId()).map(Subject::getName).orElse("General Studies")
                    : "General Studies";
            String examType = (exam.getAssessmentType() != null) ? exam.getAssessmentType().name() : "FORMAL_EXAM";

            // Load questions without answer key
            List<Question> questions = questionDAO.findByExamId(targetExamId);
            Map<Long, Question.Option> savedAnswers = attemptService.getSavedAnswers(attempt.getId());

            List<QuestionTakingDto> questionDtos = questions.stream().map(q -> {
                Question.Option selectedOpt = savedAnswers.get(q.getId());
                String selected = (selectedOpt != null) ? selectedOpt.name() : null;
                return new QuestionTakingDto(
                        q.getId(), q.getId(), q.getQuestionText(),
                        q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                        q.getMarks(), selected
                );
            }).toList();

            StartAttemptResponse response = new StartAttemptResponse(
                    attempt.getId(), exam.getId(), exam.getTitle(),
                    examType, subjectName,
                    exam.getDurationMinutes(), remainingSeconds,
                    attempt.getStartTime(), attempt.getStartTime(),
                    deadline, deadline, questionDtos
            );

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Attempt Rejected", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server Error", "message", e.getMessage()));
        }
    }

    @PostMapping("/{attemptId}/answers")
    public ResponseEntity<?> saveAnswer(
            @PathVariable Long attemptId,
            @RequestBody SaveAnswerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            // Verify ownership
            Optional<StudentExam> attOpt = studentExamDAO.findById(attemptId);
            if (attOpt.isEmpty() || !Objects.equals(attOpt.get().getStudentId(), principal.id())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Forbidden", "message", "You cannot submit answers for another student's attempt."));
            }

            StudentExam attempt = attOpt.get();
            Optional<Exam> examOpt = examDAO.findById(attempt.getExamId());
            if (examOpt.isPresent() && serverExamTimerManager.isExpired(attempt, examOpt.get())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Attempt Expired",
                        "message", "Examination deadline has expired. No further answer changes are permitted."
                ));
            }

            Question.Option opt = Question.Option.fromString(request.selectedOption());
            attemptService.saveAnswer(attemptId, request.questionId(), opt);
            return ResponseEntity.ok(Map.of("success", true, "questionId", request.questionId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Save Rejected", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server Error", "message", e.getMessage()));
        }
    }

    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<?> submitAttempt(
            @PathVariable Long attemptId,
            @RequestBody(required = false) SubmitAttemptRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            Optional<StudentExam> attOpt = studentExamDAO.findById(attemptId);
            if (attOpt.isEmpty() || !Objects.equals(attOpt.get().getStudentId(), principal.id())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Forbidden", "message", "You cannot submit another candidate's exam."));
            }

            StudentExam attempt = attOpt.get();
            Optional<Exam> examOpt = examDAO.findById(attempt.getExamId());
            Exam exam = examOpt.orElseThrow(() -> new IllegalStateException("Exam definition not found"));

            // Cancel any pending auto-submit background thread
            serverExamTimerManager.cancelSessionTimer(attemptId);

            // Guard against duplicate submission: return existing scorecard idempotently
            if (attempt.isCompleted()) {
                Map<Long, Question.Option> saved = attemptService.getSavedAnswers(attempt.getId());
                List<Question> questions = questionDAO.findByExamId(exam.getId());
                boolean wasAuto = (attempt.getStatus() == StudentExam.ExamStatus.AUTO_SUBMITTED);
                ExamResult existingResult = evaluationService.evaluate(
                        attempt.getId(), attempt.getStudentId(), exam, questions, saved, wasAuto
                );
                return ResponseEntity.ok(buildScorecardResponse(attempt, existingResult));
            }

            // Determine if auto-submitted (client flag or server-side deadline passed)
            boolean autoSubmitted = (request != null && request.autoSubmitted())
                    || serverExamTimerManager.isExpired(attempt, exam);

            ExamResult result = attemptService.submitAttempt(attemptId, Collections.emptyMap(), autoSubmitted);
            StudentExam completed = studentExamDAO.findById(attemptId).orElse(attempt);

            ScorecardResponse response = buildScorecardResponse(completed, result);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Submission Rejected", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server Error", "message", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<AttemptHistorySummaryDto>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<StudentExam> list = studentExamDAO.findByStudentId(principal.id());
        List<AttemptHistorySummaryDto> dtos = new ArrayList<>();

        for (StudentExam se : list) {
            Optional<Exam> examOpt = examDAO.findById(se.getExamId());
            String title = examOpt.map(Exam::getTitle).orElse("Exam #" + se.getExamId());
            String type = examOpt.map(e -> e.getAssessmentType().name()).orElse("FORMAL_EXAM");
            String subjectName = "General Studies";
            if (examOpt.isPresent() && examOpt.get().getSubjectId() != null) {
                try {
                    subjectName = subjectDAO.findById(examOpt.get().getSubjectId()).map(Subject::getName).orElse("General Studies");
                } catch (Exception ignored) {}
            }
            int totalMarks = examOpt.map(Exam::getTotalMarks).orElse(0);
            int passingMarks = examOpt.map(Exam::getPassingMarks).orElse(0);
            double pct = totalMarks > 0 ? ((double) se.getTotalScore() / totalMarks) * 100.0 : 0.0;
            boolean passed = se.getTotalScore() >= passingMarks;
            LocalDateTime completedTime = se.getEndTime() != null ? se.getEndTime() : se.getSubmittedAt();

            dtos.add(new AttemptHistorySummaryDto(
                    se.getId(), se.getId(), se.getExamId(), title, subjectName,
                    type, type,
                    se.getTotalScore(), se.getTotalScore(), totalMarks, Math.round(pct * 10.0) / 10.0,
                    passed, passed, se.getStatus().name(),
                    se.getStartTime(), se.getStartTime(), completedTime, completedTime
            ));
        }

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{attemptId}/result")
    public ResponseEntity<?> getResult(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<StudentExam> attOpt = studentExamDAO.findById(attemptId);
        if (attOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Attempt not found"));
        }

        StudentExam attempt = attOpt.get();
        if (!principal.isAdmin() && !Objects.equals(attempt.getStudentId(), principal.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
        }

        Exam exam = examDAO.findById(attempt.getExamId()).orElseThrow();
        List<Question> questions = questionDAO.findByExamId(exam.getId());
        Map<Long, Question.Option> saved = attemptService.getSavedAnswers(attempt.getId());
        boolean autoSubmitted = (attempt.getStatus() == StudentExam.ExamStatus.AUTO_SUBMITTED);
        ExamResult result = evaluationService.evaluate(attempt.getId(), attempt.getStudentId(), exam, questions, saved, autoSubmitted);

        ScorecardResponse response = buildScorecardResponse(attempt, result);
        return ResponseEntity.ok(response);
    }

    private ScorecardResponse buildScorecardResponse(StudentExam attempt, ExamResult result) throws SQLException {
        Exam exam = examDAO.findById(attempt.getExamId()).orElseThrow();
        Map<Long, Question.Option> candidateAnswers = attemptService.getSavedAnswers(attempt.getId());
        List<Question> questions = questionDAO.findByExamId(exam.getId());

        String subjectName = (exam.getSubjectId() != null)
                ? subjectDAO.findById(exam.getSubjectId()).map(Subject::getName).orElse("General Studies")
                : "General Studies";
        String examType = (exam.getAssessmentType() != null) ? exam.getAssessmentType().name() : "FORMAL_EXAM";

        List<QuestionReviewDto> reviewList = new ArrayList<>();
        for (Question q : questions) {
            Question.Option selectedOpt = candidateAnswers.get(q.getId());
            String selected = (selectedOpt != null) ? selectedOpt.name() : null;
            boolean correct = (selectedOpt != null && selectedOpt == q.getCorrectOption());
            int earned = correct ? q.getMarks() : 0;
            String correctOpt = (q.getCorrectOption() != null) ? q.getCorrectOption().name() : null;

            reviewList.add(new QuestionReviewDto(
                    q.getId(), q.getQuestionText(),
                    q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                    selected, correctOpt, correct,
                    earned, earned,
                    q.getMarks(), q.getMarks(),
                    q.getExplanation()
            ));
        }

        boolean autoSubmitted = (attempt.getStatus() == StudentExam.ExamStatus.AUTO_SUBMITTED);
        LocalDateTime completedTime = attempt.getEndTime() != null ? attempt.getEndTime() : attempt.getSubmittedAt();

        return new ScorecardResponse(
                attempt.getId(), exam.getId(), exam.getTitle(),
                subjectName, examType,
                result.earnedMarks(), result.earnedMarks(),
                result.totalPossibleMarks(), result.totalPossibleMarks(),
                result.percentage(),
                result.passed(), result.passed(),
                exam.getPassingMarks(),
                autoSubmitted, attempt.getStatus().name(),
                attempt.getStartTime(), attempt.getStartTime(),
                completedTime, completedTime,
                reviewList, reviewList
        );
    }
}
