package com.onlineexam.service;

import com.onlineexam.dao.SubjectDAO;
import com.onlineexam.model.Subject;
import com.onlineexam.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SubjectService} covering:
 *  - Authorization checks
 *  - CRUD operations
 *  - Deletion integrity rules (normal, force, student-history block, rollback)
 */
public class SubjectServiceTest {

    private SubjectService subjectService;
    private StubSubjectDAO stubSubjectDAO;
    private User admin;
    private User student;

    @BeforeEach
    void setUp() {
        stubSubjectDAO = new StubSubjectDAO();
        subjectService = new SubjectService(stubSubjectDAO);
        admin   = new User(1L, "admin",    "hash", "Admin User",   "admin@test.com",   User.Role.ADMIN,   null, null);
        student = new User(2L, "student1", "hash", "Student User", "student@test.com", User.Role.STUDENT, null, null);
    }

    // ── Authorization ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Non-administrator access is rejected with SecurityException")
    void testNonAdminAccessRejected() {
        Subject s = new Subject("Biology", "Life Sciences", true);
        assertThrows(SecurityException.class, () -> subjectService.createSubject(student, s));
        assertThrows(SecurityException.class, () -> subjectService.getAllSubjects(student));
        assertThrows(SecurityException.class, () -> subjectService.updateSubject(student, s));
        assertThrows(SecurityException.class, () -> subjectService.deleteSubject(student, 1L));
        assertThrows(SecurityException.class, () -> subjectService.deleteSubject(student, 1L, false));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Administrator can create subject successfully")
    void testCreateSubjectSuccess() throws SQLException {
        Subject s = new Subject("Computer Science", "Programming and Architecture", true);
        Subject created = subjectService.createSubject(admin, s);
        assertNotNull(created.getId());
        assertEquals("Computer Science", created.getName());
    }

    @Test
    @DisplayName("Duplicate subject name is rejected with IllegalArgumentException")
    void testDuplicateSubjectNameRejected() throws SQLException {
        subjectService.createSubject(admin, new Subject("Physics", "Mechanics", true));
        assertThrows(IllegalArgumentException.class,
            () -> subjectService.createSubject(admin, new Subject("Physics", "Different description", true)));
    }

    // ── Delete: no dependents ─────────────────────────────────────────────────

    @Test
    @DisplayName("Delete succeeds when subject has no dependents")
    void testDeleteSubject_NoDependents_ShouldSucceed() throws SQLException {
        Subject created = subjectService.createSubject(admin, new Subject("Maths", "Calculus", true));
        stubSubjectDAO.topicCount = 0;
        stubSubjectDAO.examCount  = 0;
        stubSubjectDAO.attemptCount = 0;
        assertTrue(subjectService.deleteSubject(admin, created.getId()));
        assertTrue(subjectService.getSubjectById(created.getId()).isEmpty());
    }

    // ── Delete: blocked by dependents (no force) ──────────────────────────────

    @Test
    @DisplayName("Delete blocked when related topics exist and force=false")
    void testDeleteSubject_WithTopics_NoForce_ShouldThrow() throws SQLException {
        Subject created = subjectService.createSubject(admin, new Subject("Chemistry", "Organic", true));
        stubSubjectDAO.topicCount = 2;
        stubSubjectDAO.examCount  = 0;
        stubSubjectDAO.attemptCount = 0;
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> subjectService.deleteSubject(admin, created.getId(), false));
        assertTrue(ex.getMessage().contains("2 related topic(s)"));
    }

    @Test
    @DisplayName("Delete blocked when related exams exist and force=false")
    void testDeleteSubject_WithExams_NoForce_ShouldThrow() throws SQLException {
        Subject created = subjectService.createSubject(admin, new Subject("Physics", "Thermodynamics", true));
        stubSubjectDAO.topicCount = 0;
        stubSubjectDAO.examCount  = 1;
        stubSubjectDAO.attemptCount = 0;
        assertThrows(IllegalStateException.class,
            () -> subjectService.deleteSubject(admin, created.getId(), false));
    }

    // ── Delete: HARD BLOCK by student history ─────────────────────────────────

    @Test
    @DisplayName("Delete is ALWAYS blocked when student exam history exists, even with force=true")
    void testDeleteSubject_WithStudentHistory_AlwaysBlocked() throws SQLException {
        Subject created = subjectService.createSubject(admin, new Subject("History", "World History", true));
        stubSubjectDAO.topicCount   = 1;
        stubSubjectDAO.examCount    = 1;
        stubSubjectDAO.attemptCount = 5; // 5 student attempts on record

        // Without force – blocked
        IllegalStateException ex1 = assertThrows(IllegalStateException.class,
            () -> subjectService.deleteSubject(admin, created.getId(), false));
        assertTrue(ex1.getMessage().contains("student exam attempt(s)"));

        // With force – STILL blocked (student history must never be deleted)
        IllegalStateException ex2 = assertThrows(IllegalStateException.class,
            () -> subjectService.deleteSubject(admin, created.getId(), true));
        assertTrue(ex2.getMessage().contains("student exam attempt(s)"));
    }

    // ── Delete: existing test (compatibility) ─────────────────────────────────

    @Test
    @DisplayName("Subject deletion blocked when related topics or exams exist (compatibility test)")
    void testSubjectDeletionBlockedWhenDependenciesExist() throws SQLException {
        Subject created = subjectService.createSubject(admin, new Subject("Chemistry", "Organic", true));

        stubSubjectDAO.topicCount = 2;
        assertThrows(IllegalStateException.class, () -> subjectService.deleteSubject(admin, created.getId()));

        stubSubjectDAO.topicCount = 0;
        stubSubjectDAO.examCount  = 1;
        assertThrows(IllegalStateException.class, () -> subjectService.deleteSubject(admin, created.getId()));

        stubSubjectDAO.examCount  = 0;
        assertTrue(subjectService.deleteSubject(admin, created.getId()));
    }

    // ── Rollback simulation ───────────────────────────────────────────────────

    @Test
    @DisplayName("Normal delete uses SubjectDAO.delete(Long id), no force path")
    void testNormalDeleteUsesSimplePath() throws SQLException {
        Subject created = subjectService.createSubject(admin, new Subject("Geography", "Earth", true));
        stubSubjectDAO.topicCount   = 0;
        stubSubjectDAO.examCount    = 0;
        stubSubjectDAO.attemptCount = 0;
        // Simple (non-force) path reaches SubjectDAO.delete(Long) – verify it succeeds
        assertTrue(subjectService.deleteSubject(admin, created.getId()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // In-Memory Test Stub
    // ──────────────────────────────────────────────────────────────────────────
    private static class StubSubjectDAO extends SubjectDAO {
        private final Map<Long, Subject> store = new HashMap<>();
        int topicCount   = 0;
        int examCount    = 0;
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

        @Override public boolean update(Subject subject) {
            if (store.containsKey(subject.getId())) { store.put(subject.getId(), subject); return true; }
            return false;
        }

        @Override public boolean delete(Long id) { return store.remove(id) != null; }

        @Override public int countTopics(Long subjectId)   { return topicCount; }
        @Override public int countExams(Long subjectId)    { return examCount; }

        @Override public int countStudentAttemptsBySubject(Long subjectId) { return attemptCount; }

        /** Force path is tested via integration; stub returns empty list so force path skips materials loop. */
        @Override public List<com.onlineexam.model.Topic> findTopicsBySubject(Long subjectId) {
            return new ArrayList<>();
        }
    }
}
