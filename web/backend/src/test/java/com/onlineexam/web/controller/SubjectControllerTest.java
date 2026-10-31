package com.onlineexam.web.controller;

import com.onlineexam.dao.SubjectDAO;
import com.onlineexam.dao.UserDAO;
import com.onlineexam.model.Subject;
import com.onlineexam.model.User;
import com.onlineexam.service.AuthService;
import com.onlineexam.service.SubjectService;
import com.onlineexam.web.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SubjectControllerTest {

    private SubjectController subjectController;
    private StubSubjectDAO stubSubjectDAO;
    private AuthService authService;
    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        stubSubjectDAO = new StubSubjectDAO();
        SubjectService subjectService = new SubjectService(stubSubjectDAO);
        UserDAO userDAO = new UserDAO();
        authService = new AuthService(userDAO);

        adminPrincipal = new UserPrincipal(1L, "admin", "ADMIN");
        subjectController = new SubjectController(subjectService, stubSubjectDAO, authService);
    }

    @Test
    @DisplayName("DELETE /api/subjects/{id} without force when dependents exist returns 400")
    void testDeleteSubjectWithDependentsNoForceReturns400() {
        Subject s = new Subject("Discrete Math", "Theory", true);
        stubSubjectDAO.create(s);
        stubSubjectDAO.topicCount = 2;

        ResponseEntity<?> response = subjectController.deleteSubject(adminPrincipal, s.getId(), false);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("related topic(s)"));
    }

    @Test
    @DisplayName("DELETE /api/subjects/{id}?force=true when student history exists returns 400 (hard block)")
    void testDeleteSubjectWithStudentHistoryReturns400EvenWithForce() {
        Subject s = new Subject("Data Structures", "Algorithms", true);
        stubSubjectDAO.create(s);
        stubSubjectDAO.attemptCount = 3;

        ResponseEntity<?> response = subjectController.deleteSubject(adminPrincipal, s.getId(), true);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("student exam attempt(s)"));
    }

    @Test
    @DisplayName("DELETE /api/subjects/{id} on clean subject succeeds and returns 200")
    void testDeleteCleanSubjectSucceeds() {
        Subject s = new Subject("Web Architecture", "Frontend & Backend", true);
        stubSubjectDAO.create(s);
        stubSubjectDAO.topicCount = 0;
        stubSubjectDAO.examCount = 0;
        stubSubjectDAO.attemptCount = 0;

        ResponseEntity<?> response = subjectController.deleteSubject(adminPrincipal, s.getId(), false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Subject deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/subjects/{id} on non-existent subject returns 404")
    void testDeleteNonExistentSubjectReturns404() {
        ResponseEntity<?> response = subjectController.deleteSubject(adminPrincipal, 999999L, false);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // --- Stub Subject DAO ---
    private static class StubSubjectDAO extends SubjectDAO {
        private final Map<Long, Subject> store = new HashMap<>();
        int topicCount = 0;
        int examCount = 0;
        int attemptCount = 0;

        @Override public Optional<Subject> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public Optional<Subject> findByName(String name) {
            return store.values().stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
        }
        @Override public List<Subject> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<Subject> findAllPublished() {
            return store.values().stream().filter(Subject::isPublished).toList();
        }
        @Override public Subject create(Subject subject) {
            long newId = store.size() + 1L;
            subject.setId(newId);
            store.put(newId, subject);
            return subject;
        }
        @Override public boolean delete(Long id) { return store.remove(id) != null; }
        @Override public int countTopics(Long subjectId) { return topicCount; }
        @Override public int countExams(Long subjectId) { return examCount; }
        @Override public int countStudentAttemptsBySubject(Long subjectId) { return attemptCount; }
    }
}

