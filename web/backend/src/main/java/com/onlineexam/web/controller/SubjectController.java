package com.onlineexam.web.controller;

import com.onlineexam.dao.SubjectDAO;
import com.onlineexam.model.Subject;
import com.onlineexam.model.User;
import com.onlineexam.service.AuthService;
import com.onlineexam.service.SubjectService;
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
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;
    private final SubjectDAO subjectDAO;
    private final AuthService authService;

    public SubjectController(SubjectService subjectService, SubjectDAO subjectDAO, AuthService authService) {
        this.subjectService = subjectService;
        this.subjectDAO = subjectDAO;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<List<SubjectResponse>> getSubjects(@AuthenticationPrincipal UserPrincipal principal) throws SQLException {
        boolean isAdmin = principal != null && principal.isAdmin();
        List<Subject> subjects = isAdmin ? subjectDAO.findAll() : subjectDAO.findAllPublished();

        List<SubjectResponse> responses = new ArrayList<>();
        for (Subject s : subjects) {
            int topicCount = subjectDAO.countTopics(s.getId());
            int examCount = subjectDAO.countExams(s.getId());
            responses.add(new SubjectResponse(s.getId(), s.getName(), s.getDescription(), s.isPublished(), topicCount, examCount));
        }
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSubjectById(@PathVariable Long id) throws SQLException {
        Optional<Subject> subjectOpt = subjectService.getSubjectById(id);
        if (subjectOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Subject not found"));
        }
        Subject s = subjectOpt.get();
        int topicCount = subjectDAO.countTopics(s.getId());
        int examCount = subjectDAO.countExams(s.getId());
        return ResponseEntity.ok(new SubjectResponse(s.getId(), s.getName(), s.getDescription(), s.isPublished(), topicCount, examCount));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSubject(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestBody CreateSubjectRequest request) {
        try {
            User admin = authService.getUserById(principal.id()).orElseThrow();
            Subject s = new Subject(request.name(), request.description(), request.getEffectiveIsPublished());
            Subject created = subjectService.createSubject(admin, s);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSubject(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long id,
                                           @RequestBody CreateSubjectRequest request) {
        try {
            User admin = authService.getUserById(principal.id()).orElseThrow();
            Subject s = new Subject(request.name(), request.description(), request.getEffectiveIsPublished());
            s.setId(id);
            boolean updated = subjectService.updateSubject(admin, s);
            if (!updated) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Subject not found"));
            }
            return ResponseEntity.ok(s);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation Error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSubject(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long id,
                                           @RequestParam(required = false, defaultValue = "false") boolean force) {
        try {
            User admin = authService.getUserById(principal.id()).orElseThrow();
            boolean deleted = subjectService.deleteSubject(admin, id, force);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Subject not found"));
            }
            return ResponseEntity.ok(Map.of("message", "Subject deleted successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot Delete Subject", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
