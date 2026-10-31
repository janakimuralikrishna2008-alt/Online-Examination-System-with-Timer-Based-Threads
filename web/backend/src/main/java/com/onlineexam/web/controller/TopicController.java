package com.onlineexam.web.controller;

import com.onlineexam.dao.SubjectDAO;
import com.onlineexam.dao.TopicDAO;
import com.onlineexam.model.Subject;
import com.onlineexam.model.Topic;
import com.onlineexam.model.User;
import com.onlineexam.service.AuthService;
import com.onlineexam.service.LearningService;
import com.onlineexam.service.TopicService;
import com.onlineexam.web.dto.SubjectDto.*;
import com.onlineexam.web.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.*;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;
    private final TopicDAO topicDAO;
    private final SubjectDAO subjectDAO;
    private final LearningService learningService;
    private final AuthService authService;
    private final com.onlineexam.dao.QuestionDAO questionDAO;

    public TopicController(TopicService topicService, TopicDAO topicDAO, SubjectDAO subjectDAO,
                           LearningService learningService, AuthService authService,
                           com.onlineexam.dao.QuestionDAO questionDAO) {
        this.topicService = topicService;
        this.topicDAO = topicDAO;
        this.subjectDAO = subjectDAO;
        this.learningService = learningService;
        this.authService = authService;
        this.questionDAO = questionDAO;
    }


    @GetMapping
    public ResponseEntity<List<TopicResponse>> getTopics(
            @RequestParam(required = false) Long subjectId,
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        boolean isAdmin = principal != null && principal.isAdmin();
        Long studentId = (principal != null && !principal.isAdmin()) ? principal.id() : null;

        List<Topic> topics;
        if (subjectId != null) {
            topics = isAdmin ? topicDAO.findBySubjectId(subjectId) : topicDAO.findPublishedBySubjectId(subjectId);
        } else {
            topics = isAdmin ? topicDAO.findAll() : topicDAO.findAll().stream().filter(Topic::isPublished).toList();
        }

        List<TopicResponse> responses = new ArrayList<>();
        for (Topic t : topics) {
            String subjectName = subjectDAO.findById(t.getSubjectId()).map(Subject::getName).orElse("Unknown Subject");
            double progress = (studentId != null) ? learningService.getTopicProgressPercentage(studentId, t.getId()) : 0.0;
            int materialCount = topicDAO.countMaterials(t.getId());
            int questionCount = questionDAO.findByTopicId(t.getId()).size();
            responses.add(new TopicResponse(
                    t.getId(), t.getSubjectId(), subjectName, t.getName(), t.getDescription(),
                    t.getDisplayOrder(), t.isPublished(), progress, materialCount, questionCount
            ));
        }
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<List<TopicResponse>> getTopicsBySubject(
            @PathVariable Long subjectId,
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        return getTopics(subjectId, principal);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTopicById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        Optional<Topic> topicOpt = topicService.getTopicById(id);
        if (topicOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Topic not found"));
        }
        Topic t = topicOpt.get();
        String subjectName = subjectDAO.findById(t.getSubjectId()).map(Subject::getName).orElse("Unknown Subject");
        Long studentId = (principal != null && !principal.isAdmin()) ? principal.id() : null;
        double progress = (studentId != null) ? learningService.getTopicProgressPercentage(studentId, t.getId()) : 0.0;
        int materialCount = topicDAO.countMaterials(t.getId());
        int questionCount = questionDAO.findByTopicId(t.getId()).size();


        return ResponseEntity.ok(new TopicResponse(
                t.getId(), t.getSubjectId(), subjectName, t.getName(), t.getDescription(),
                t.getDisplayOrder(), t.isPublished(), progress, materialCount, questionCount
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createTopic(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestBody CreateTopicRequest request) {
        try {
            User admin = authService.getUserById(principal.id()).orElseThrow();
            Topic t = new Topic(request.subjectId(), request.name(), request.description(),
                    request.getEffectiveDisplayOrder(), request.getEffectiveIsPublished());
            Topic created = topicService.createTopic(admin, t);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTopic(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id,
                                         @RequestBody CreateTopicRequest request) {
        try {
            User admin = authService.getUserById(principal.id()).orElseThrow();
            Topic t = new Topic(id, request.subjectId(), request.name(), request.description(),
                    request.getEffectiveDisplayOrder(), request.getEffectiveIsPublished(), null, null);
            boolean updated = topicService.updateTopic(admin, t);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Topic not found"));
            }
            return ResponseEntity.ok(t);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTopic(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id) {
        try {
            User admin = authService.getUserById(principal.id()).orElseThrow();
            boolean deleted = topicService.deleteTopic(admin, id);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Topic not found"));
            }
            return ResponseEntity.ok(Map.of("message", "Topic deleted successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot Delete Topic", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
