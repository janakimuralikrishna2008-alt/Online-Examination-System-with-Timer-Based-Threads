package com.onlineexam.web.controller;

import com.onlineexam.dao.TopicDAO;
import com.onlineexam.model.StudyMaterial;
import com.onlineexam.model.Topic;
import com.onlineexam.model.User;
import com.onlineexam.service.AuthService;
import com.onlineexam.service.LearningService;
import com.onlineexam.web.dto.StudyMaterialDto.*;
import com.onlineexam.web.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.*;

@RestController
@RequestMapping("/api/materials")
public class StudyMaterialController {

    private final LearningService learningService;
    private final TopicDAO topicDAO;
    private final AuthService authService;

    public StudyMaterialController(LearningService learningService, TopicDAO topicDAO, AuthService authService) {
        this.learningService = learningService;
        this.topicDAO = topicDAO;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<MaterialResponse>> getMaterials(
            @RequestParam Long topicId,
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        boolean isAdmin = principal != null && principal.isAdmin();
        Long studentId = (principal != null && !principal.isAdmin()) ? principal.id() : null;

        User user = (principal != null) ? authService.getUserById(principal.id()).orElse(null) : null;
        List<StudyMaterial> materials = isAdmin ? learningService.getAllMaterials(user, topicId)
                                                : learningService.getPublishedMaterials(topicId);

        String topicName = topicDAO.findById(topicId).map(Topic::getName).orElse("Topic #" + topicId);
        List<MaterialResponse> responses = new ArrayList<>();
        for (StudyMaterial m : materials) {
            boolean completed = (studentId != null) && learningService.isMaterialStudied(studentId, m.getId());
            responses.add(new MaterialResponse(
                    m.getId(), m.getTopicId(), topicName, m.getTitle(), m.getContent(),
                    m.getResourceUrl(), m.getAttachmentPath(), m.getAttachmentType(),
                    m.getDisplayOrder(), m.isPublished(), completed
            ));
        }
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/topic/{topicId}")
    public ResponseEntity<List<MaterialResponse>> getMaterialsByTopic(
            @PathVariable Long topicId,
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        return getMaterials(topicId, principal);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMaterialById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        Optional<StudyMaterial> matOpt = learningService.getMaterialById(id);
        if (matOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Study material not found"));
        }
        StudyMaterial m = matOpt.get();
        String topicName = topicDAO.findById(m.getTopicId()).map(Topic::getName).orElse("Topic #" + m.getTopicId());
        Long studentId = (principal != null && !principal.isAdmin()) ? principal.id() : null;
        boolean completed = (studentId != null) && learningService.isMaterialStudied(studentId, m.getId());

        return ResponseEntity.ok(new MaterialResponse(
                m.getId(), m.getTopicId(), topicName, m.getTitle(), m.getContent(),
                m.getResourceUrl(), m.getAttachmentPath(), m.getAttachmentType(),
                m.getDisplayOrder(), m.isPublished(), completed
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createMaterial(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreateMaterialRequest request) {
        try {
            User admin = authService.getUserById(principal.id()).orElseThrow();
            StudyMaterial m = new StudyMaterial(
                    request.topicId(), request.title(), request.content(),
                    request.resourceUrl(), request.attachmentPath(), request.getEffectiveAttachmentType(),
                    request.getEffectiveDisplayOrder(), request.getEffectiveIsPublished()
            );
            StudyMaterial created = learningService.createMaterial(admin, m);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateMaterial(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody CreateMaterialRequest request) {
        try {
            User admin = authService.getUserById(principal.id()).orElseThrow();
            StudyMaterial m = new StudyMaterial(
                    id, request.topicId(), request.title(), request.content(),
                    request.resourceUrl(), request.attachmentPath(), request.getEffectiveAttachmentType(),
                    request.getEffectiveDisplayOrder(), request.getEffectiveIsPublished(), null, null, null
            );
            boolean updated = learningService.updateMaterial(admin, m);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Study material not found"));
            }
            return ResponseEntity.ok(m);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteMaterial(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        try {
            User admin = authService.getUserById(principal.id()).orElseThrow();
            boolean deleted = learningService.deleteMaterial(admin, id);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Study material not found"));
            }
            return ResponseEntity.ok(Map.of("message", "Study material deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/progress")
    public ResponseEntity<?> markStudied(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean completed = true;
        if (body != null && body.containsKey("completed")) {
            completed = Boolean.parseBoolean(body.get("completed").toString());
        }
        boolean marked = learningService.setMaterialProgress(principal.id(), id, completed);
        return ResponseEntity.ok(Map.of("success", marked, "materialId", id, "completed", completed));
    }

    @GetMapping("/progress")
    public ResponseEntity<?> getTopicProgress(
            @RequestParam Long topicId,
            @AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        double pct = learningService.getTopicProgressPercentage(principal.id(), topicId);
        Set<Long> completedIds = learningService.getCompletedMaterialIds(principal.id(), topicId);
        List<StudyMaterial> published = learningService.getPublishedMaterials(topicId);
        return ResponseEntity.ok(new ProgressResponse(topicId, pct, completedIds.size(), published.size()));
    }
}
