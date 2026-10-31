package com.onlineexam.web.controller;

import com.onlineexam.dao.ExamDAO;
import com.onlineexam.dao.QuestionDAO;
import com.onlineexam.dao.StudentExamDAO;
import com.onlineexam.dao.SubjectDAO;
import com.onlineexam.dao.TopicDAO;
import com.onlineexam.model.*;
import com.onlineexam.service.AuthService;
import com.onlineexam.service.ExamService;
import com.onlineexam.web.dto.ExamDto.*;
import com.onlineexam.web.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.*;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;
    private final ExamDAO examDAO;
    private final QuestionDAO questionDAO;
    private final SubjectDAO subjectDAO;
    private final TopicDAO topicDAO;
    private final StudentExamDAO studentExamDAO;
    private final AuthService authService;

    public ExamController(ExamService examService, ExamDAO examDAO, QuestionDAO questionDAO,
                          SubjectDAO subjectDAO, TopicDAO topicDAO, StudentExamDAO studentExamDAO,
                          AuthService authService) {
        this.examService = examService;
        this.examDAO = examDAO;
        this.questionDAO = questionDAO;
        this.subjectDAO = subjectDAO;
        this.topicDAO = topicDAO;
        this.studentExamDAO = studentExamDAO;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<ExamResponse>> getExams(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        boolean isAdmin = principal != null && principal.isAdmin();
        Long studentId = (principal != null && !principal.isAdmin()) ? principal.id() : null;

        List<Exam> exams;
        if (isAdmin) {
            exams = examDAO.findAll();
        } else {
            exams = examService.getAvailableExams();
        }

        // Apply filters
        if (subjectId != null) {
            exams = exams.stream().filter(e -> Objects.equals(e.getSubjectId(), subjectId)).toList();
        }
        if (topicId != null) {
            exams = exams.stream().filter(e -> Objects.equals(e.getTopicId(), topicId)).toList();
        }
        if (type != null && !type.isBlank()) {
            exams = exams.stream().filter(e -> e.getAssessmentType().name().equalsIgnoreCase(type)).toList();
        }

        List<ExamResponse> responses = new ArrayList<>();
        for (Exam e : exams) {
            String subjectName = (e.getSubjectId() != null)
                    ? subjectDAO.findById(e.getSubjectId()).map(Subject::getName).orElse(null) : null;
            String topicName = (e.getTopicId() != null)
                    ? topicDAO.findById(e.getTopicId()).map(Topic::getName).orElse(null) : null;
            int qCount = questionDAO.countByExamId(e.getId());

            String attemptStatus = "NOT_STARTED";
            if (studentId != null) {
                Optional<StudentExam> attOpt = studentExamDAO.findByStudentAndExam(studentId, e.getId());
                if (attOpt.isPresent()) {
                    StudentExam att = attOpt.get();
                    if (att.isInProgress()) {
                        attemptStatus = "IN_PROGRESS";
                    } else if (att.isCompleted()) {
                        attemptStatus = (att.getTotalScore() >= e.getPassingMarks()) ? "PASSED" : "FAILED";
                    }
                }
            }

            responses.add(new ExamResponse(
                    e.getId(), e.getTitle(), e.getDescription(), e.getDurationMinutes(),
                    e.getTotalMarks(), e.getPassingMarks(), e.isActive(),
                    e.getSubjectId(), subjectName, e.getTopicId(), topicName,
                    e.getAssessmentType().name(), qCount, attemptStatus
            ));
        }

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getExamById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        Optional<Exam> examOpt = examDAO.findById(id);
        if (examOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Exam not found"));
        }
        Exam e = examOpt.get();
        String subjectName = (e.getSubjectId() != null)
                ? subjectDAO.findById(e.getSubjectId()).map(Subject::getName).orElse(null) : null;
        String topicName = (e.getTopicId() != null)
                ? topicDAO.findById(e.getTopicId()).map(Topic::getName).orElse(null) : null;
        int qCount = questionDAO.countByExamId(e.getId());

        Long studentId = (principal != null && !principal.isAdmin()) ? principal.id() : null;
        String attemptStatus = "NOT_STARTED";
        if (studentId != null) {
            Optional<StudentExam> attOpt = studentExamDAO.findByStudentAndExam(studentId, e.getId());
            if (attOpt.isPresent()) {
                StudentExam att = attOpt.get();
                if (att.isInProgress()) {
                    attemptStatus = "IN_PROGRESS";
                } else if (att.isCompleted()) {
                    attemptStatus = (att.getTotalScore() >= e.getPassingMarks()) ? "PASSED" : "FAILED";
                }
            }
        }

        return ResponseEntity.ok(new ExamResponse(
                e.getId(), e.getTitle(), e.getDescription(), e.getDurationMinutes(),
                e.getTotalMarks(), e.getPassingMarks(), e.isActive(),
                e.getSubjectId(), subjectName, e.getTopicId(), topicName,
                e.getAssessmentType().name(), qCount, attemptStatus
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createExam(
            @RequestBody CreateExamRequest request) {
        try {
            Exam.AssessmentType type = request.getEffectiveAssessmentType().equalsIgnoreCase("PRACTICE_QUIZ")
                    ? Exam.AssessmentType.PRACTICE_QUIZ : Exam.AssessmentType.FORMAL_EXAM;

            Exam exam = new Exam();
            exam.setTitle(request.title());
            exam.setDescription(request.description());
            exam.setDurationMinutes(request.getEffectiveDurationMinutes());
            exam.setTotalMarks(request.getEffectiveTotalMarks());
            exam.setPassingMarks(request.getEffectivePassingMarks());
            exam.setActive(request.getEffectiveIsActive());
            exam.setSubjectId(request.subjectId());
            exam.setTopicId(request.topicId());
            exam.setAssessmentType(type);

            Exam created = examService.createExam(exam);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateExam(
            @PathVariable Long id,
            @RequestBody CreateExamRequest request) {
        try {
            Exam.AssessmentType type = request.getEffectiveAssessmentType().equalsIgnoreCase("PRACTICE_QUIZ")
                    ? Exam.AssessmentType.PRACTICE_QUIZ : Exam.AssessmentType.FORMAL_EXAM;

            Exam exam = new Exam();
            exam.setId(id);
            exam.setTitle(request.title());
            exam.setDescription(request.description());
            exam.setDurationMinutes(request.getEffectiveDurationMinutes());
            exam.setTotalMarks(request.getEffectiveTotalMarks());
            exam.setPassingMarks(request.getEffectivePassingMarks());
            exam.setActive(request.getEffectiveIsActive());
            exam.setSubjectId(request.subjectId());
            exam.setTopicId(request.topicId());
            exam.setAssessmentType(type);

            boolean updated = examService.updateExam(exam);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Exam not found"));
            }
            return ResponseEntity.ok(exam);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteExam(@PathVariable Long id) {
        try {
            boolean deleted = examService.deleteExam(id);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Exam not found"));
            }
            return ResponseEntity.ok(Map.of("message", "Exam deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

}
