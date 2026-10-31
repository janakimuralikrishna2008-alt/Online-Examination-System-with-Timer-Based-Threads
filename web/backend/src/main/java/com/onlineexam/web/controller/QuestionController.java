package com.onlineexam.web.controller;

import com.onlineexam.dao.QuestionDAO;
import com.onlineexam.model.Question;
import com.onlineexam.model.User;
import com.onlineexam.service.AuthService;
import com.onlineexam.web.dto.ExamDto.CreateQuestionRequest;
import com.onlineexam.web.dto.ExamDto.QuestionResponse;
import com.onlineexam.web.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.*;

@RestController
@RequestMapping("/api/questions")
@PreAuthorize("hasRole('ADMIN')")
public class QuestionController {

    private final QuestionDAO questionDAO;
    private final AuthService authService;

    public QuestionController(QuestionDAO questionDAO, AuthService authService) {
        this.questionDAO = questionDAO;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<QuestionResponse>> getQuestions(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) Long topicId) throws SQLException {
        List<Question> questions;
        if (examId != null) {
            questions = questionDAO.findByExamId(examId);
        } else if (topicId != null) {
            questions = questionDAO.findByTopicId(topicId);
        } else {
            questions = questionDAO.findAll();
        }

        List<QuestionResponse> responses = questions.stream().map(q -> new QuestionResponse(
                q.getId(), q.getExamId(), q.getTopicId(), q.getQuestionText(),
                q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                q.getCorrectOption() != null ? q.getCorrectOption().name() : null,
                q.getMarks(), q.getExplanation(), q.isPublished()
        )).toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/exam/{examId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByExam(
            @PathVariable Long examId) throws SQLException {
        return getQuestions(examId, null);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestionById(@PathVariable Long id) throws SQLException {
        Optional<Question> qOpt = questionDAO.findById(id);
        if (qOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Question not found"));
        }
        Question q = qOpt.get();
        return ResponseEntity.ok(new QuestionResponse(
                q.getId(), q.getExamId(), q.getTopicId(), q.getQuestionText(),
                q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(),
                q.getCorrectOption() != null ? q.getCorrectOption().name() : null,
                q.getMarks(), q.getExplanation(), q.isPublished()
        ));
    }

    @PostMapping
    public ResponseEntity<?> createQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreateQuestionRequest request) {
        try {
            Question q = new Question();
            q.setExamId(request.examId());
            q.setTopicId(request.topicId());
            q.setQuestionText(request.questionText());
            q.setOptionA(request.optionA());
            q.setOptionB(request.optionB());
            q.setOptionC(request.optionC());
            q.setOptionD(request.optionD());
            q.setCorrectOption(Question.Option.fromString(request.correctOption()));
            q.setMarks(request.getEffectiveMarks());
            q.setExplanation(request.explanation());
            q.setPublished(request.getEffectiveIsPublished());

            Question created = questionDAO.create(q);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuestion(
            @PathVariable Long id,
            @RequestBody CreateQuestionRequest request) {
        try {
            Question q = new Question();
            q.setId(id);
            q.setExamId(request.examId());
            q.setTopicId(request.topicId());
            q.setQuestionText(request.questionText());
            q.setOptionA(request.optionA());
            q.setOptionB(request.optionB());
            q.setOptionC(request.optionC());
            q.setOptionD(request.optionD());
            q.setCorrectOption(Question.Option.fromString(request.correctOption()));
            q.setMarks(request.getEffectiveMarks());
            q.setExplanation(request.explanation());
            q.setPublished(request.getEffectiveIsPublished());

            boolean updated = questionDAO.update(q);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Question not found"));
            }
            return ResponseEntity.ok(q);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        try {
            boolean deleted = questionDAO.delete(id);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Question not found"));
            }
            return ResponseEntity.ok(Map.of("message", "Question deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
