package com.onlineexam.web.controller;

import com.onlineexam.dao.StudentExamDAO;
import com.onlineexam.model.StudentExam;
import com.onlineexam.web.dto.AiDto.*;
import com.onlineexam.web.security.UserPrincipal;
import com.onlineexam.web.service.ai.AiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final StudentExamDAO studentExamDAO;

    public AiController(AiService aiService, StudentExamDAO studentExamDAO) {
        this.aiService = aiService;
        this.studentExamDAO = studentExamDAO;
    }

    @PostMapping("/study-assistant")
    public ResponseEntity<?> askStudyAssistant(@RequestBody Map<String, Object> body) {
        try {
            Long topicId = 1L;
            if (body.get("topicId") != null) {
                topicId = Long.valueOf(body.get("topicId").toString());
            }

            String question = (String) body.get("question");
            if (question == null || question.isBlank()) {
                question = (String) body.get("message");
            }
            if (question == null || question.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Question or message cannot be empty."));
            }

            AiStudyResponse response = aiService.askStudyAssistant(topicId, question);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("reply", response.answer());
            res.put("answer", response.answer());
            res.put("topicName", response.topicName());
            res.put("sourceMaterials", response.sourceMaterials());
            res.put("sourceBased", response.sourceBased());
            res.put("provider", response.providerStatus());
            res.put("providerStatus", response.providerStatus());
            res.put("model", "gemini-1.5-flash");

            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "AI Service Error", "message", e.getMessage()));
        }
    }

    @PostMapping("/draft-question")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> draftQuestion(@RequestBody Map<String, Object> body) {
        try {
            Long topicId = null;
            if (body.get("topicId") != null) {
                topicId = Long.valueOf(body.get("topicId").toString());
            }
            String difficulty = (String) body.getOrDefault("difficulty", "MEDIUM");
            String promptNotes = (String) body.get("promptNotes");
            if (promptNotes == null) {
                promptNotes = (String) body.get("topic");
            }

            AiDraftQuestionResponse response = aiService.draftQuestion(topicId, difficulty, promptNotes);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Draft Generation Error", "message", e.getMessage()));
        }
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<AiRecommendationDto>> getRecommendations(
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        if (principal == null) return ResponseEntity.status(401).build();
        List<AiRecommendationDto> list = aiService.getPersonalizedRecommendations(principal.id());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/performance-insights")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPerformanceInsights() throws SQLException {
        AiPerformanceInsightsResponse response = aiService.getAggregateInsights();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/performance-insights/{attemptId}")
    public ResponseEntity<?> getAttemptInsights(
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

        String insightText = String.format(
                "You scored %d points on this attempt (Status: %s). " +
                "Review the detailed explanation for each question in the breakdown below to reinforce your knowledge.",
                attempt.getTotalScore(), attempt.getStatus().name()
        );

        return ResponseEntity.ok(Map.of(
                "attemptId", attemptId,
                "insights", insightText,
                "status", attempt.getStatus().name()
        ));
    }
}
