package com.onlineexam.service;

import com.onlineexam.dao.StudyMaterialDAO;
import com.onlineexam.model.StudyMaterial;
import com.onlineexam.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LearningService} covering study materials, student progress tracking,
 * authorization controls, and secure file attachment validation.
 */
public class LearningServiceTest {

    private LearningService learningService;
    private StubStudyMaterialDAO stubDAO;
    private Path tempStorageDir;
    private User admin;
    private User student;

    @BeforeEach
    void setUp() throws IOException {
        stubDAO = new StubStudyMaterialDAO();
        tempStorageDir = Files.createTempDirectory("online_exam_test_materials_");
        learningService = new LearningService(stubDAO, tempStorageDir);

        admin = new User(1L, "admin", "hash", "Admin User", "admin@test.com", User.Role.ADMIN, null, null);
        student = new User(2L, "student1", "hash", "Student User", "student@test.com", User.Role.STUDENT, null, null);
    }

    @AfterEach
    void tearDown() {
        if (tempStorageDir != null && Files.exists(tempStorageDir)) {
            try (var stream = Files.walk(tempStorageDir)) {
                stream.sorted(Comparator.reverseOrder())
                      .map(Path::toFile)
                      .forEach(File::delete);
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    @DisplayName("Non-administrator access to study material management is rejected")
    void testNonAdminAccessRejected() {
        StudyMaterial material = new StudyMaterial(10L, "Sample Guide", "Notes", 1, true);
        assertThrows(SecurityException.class, () -> learningService.createMaterial(student, material));
        assertThrows(SecurityException.class, () -> learningService.getAllMaterials(student, 10L));
        assertThrows(SecurityException.class, () -> learningService.updateMaterial(student, material));
        assertThrows(SecurityException.class, () -> learningService.deleteMaterial(student, 1L));
    }

    @Test
    @DisplayName("Administrator can create, update, and delete study materials successfully")
    void testAdminCreateUpdateDeleteMaterial() throws SQLException {
        StudyMaterial m = new StudyMaterial(10L, "Java Concurrency", "Thread pools & Executors", 1, true);
        StudyMaterial created = learningService.createMaterial(admin, m);

        assertNotNull(created.getId());
        assertEquals("Java Concurrency", created.getTitle());
        assertEquals(admin.getId(), created.getCreatedBy());

        created.setTitle("Advanced Java Concurrency");
        assertTrue(learningService.updateMaterial(admin, created));

        Optional<StudyMaterial> fetched = learningService.getMaterialById(created.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Advanced Java Concurrency", fetched.get().getTitle());

        assertTrue(learningService.deleteMaterial(admin, created.getId()));
        assertTrue(learningService.getMaterialById(created.getId()).isEmpty());
    }

    @Test
    @DisplayName("Student progress tracking accurately marks completion and computes percentages")
    void testStudentProgressTracking() throws SQLException {
        StudyMaterial m1 = new StudyMaterial(10L, "Topic Overview", "Content 1", 1, true);
        StudyMaterial m2 = new StudyMaterial(10L, "Deep Dive", "Content 2", 2, true);
        learningService.createMaterial(admin, m1);
        learningService.createMaterial(admin, m2);

        // Initially no progress
        assertEquals(0.0, learningService.getTopicProgressPercentage(student.getId(), 10L), 0.001);
        assertFalse(learningService.isMaterialStudied(student.getId(), m1.getId()));

        // Mark m1 as studied
        assertTrue(learningService.markMaterialStudied(student.getId(), m1.getId()));
        assertTrue(learningService.isMaterialStudied(student.getId(), m1.getId()));
        assertFalse(learningService.isMaterialStudied(student.getId(), m2.getId()));
        assertEquals(50.0, learningService.getTopicProgressPercentage(student.getId(), 10L), 0.001);

        // Mark m2 as studied
        assertTrue(learningService.markMaterialStudied(student.getId(), m2.getId()));
        assertTrue(learningService.isMaterialStudied(student.getId(), m2.getId()));
        assertEquals(100.0, learningService.getTopicProgressPercentage(student.getId(), 10L), 0.001);

        // Topic with no materials returns 100%
        assertEquals(100.0, learningService.getTopicProgressPercentage(student.getId(), 999L), 0.001);
    }

    @Test
    @DisplayName("Attachment upload rejects invalid or non-existent files")
    void testAttachmentValidationInvalidFile() {
        assertThrows(IllegalArgumentException.class, () -> learningService.saveAttachmentSafely(null));
        assertThrows(IllegalArgumentException.class, () -> learningService.saveAttachmentSafely(new File("non_existent_file_xyz.txt")));
    }

    @Test
    @DisplayName("Attachment upload rejects disallowed file types (.exe, .sh, .bat)")
    void testAttachmentValidationDisallowedExtension() throws IOException {
        Path tempFile = Files.createTempFile("malicious_script", ".exe");
        try {
            Files.writeString(tempFile, "malicious binary");
            assertThrows(IllegalArgumentException.class, () -> learningService.saveAttachmentSafely(tempFile.toFile()));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("Attachment upload securely stores allowed files (.pdf, .txt, .png)")
    void testAttachmentValidationAllowedExtension() throws IOException {
        Path tempDoc = Files.createTempFile("study_notes", ".pdf");
        try {
            Files.writeString(tempDoc, "PDF Header and Document Content");
            String storedPath = learningService.saveAttachmentSafely(tempDoc.toFile());

            assertNotNull(storedPath);
            assertTrue(storedPath.startsWith("storage/materials/"));
            assertTrue(storedPath.endsWith(".pdf"));

            // Verify file actually exists in storage dir
            String fileName = Paths.get(storedPath).getFileName().toString();
            Path destFile = tempStorageDir.resolve(fileName);
            assertTrue(Files.exists(destFile));
            assertEquals("PDF Header and Document Content", Files.readString(destFile));
        } finally {
            Files.deleteIfExists(tempDoc);
        }
    }

    // --- In-Memory Test Stub for StudyMaterialDAO ---
    private static class StubStudyMaterialDAO extends StudyMaterialDAO {
        private final Map<Long, StudyMaterial> store = new HashMap<>();
        private final Set<String> progressStore = new HashSet<>(); // "studentId:materialId"

        @Override
        public Optional<StudyMaterial> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<StudyMaterial> findByTopicId(Long topicId) {
            return store.values().stream()
                    .filter(m -> Objects.equals(m.getTopicId(), topicId))
                    .sorted(Comparator.comparingInt(StudyMaterial::getDisplayOrder))
                    .toList();
        }

        @Override
        public List<StudyMaterial> findPublishedByTopicId(Long topicId) {
            return store.values().stream()
                    .filter(m -> Objects.equals(m.getTopicId(), topicId) && m.isPublished())
                    .sorted(Comparator.comparingInt(StudyMaterial::getDisplayOrder))
                    .toList();
        }

        @Override
        public StudyMaterial create(StudyMaterial material) {
            long newId = store.size() + 1L;
            material.setId(newId);
            store.put(newId, material);
            return material;
        }

        @Override
        public boolean update(StudyMaterial material) {
            if (store.containsKey(material.getId())) {
                store.put(material.getId(), material);
                return true;
            }
            return false;
        }

        @Override
        public boolean delete(Long id) {
            return store.remove(id) != null;
        }

        @Override
        public boolean markCompleted(Long studentId, Long materialId) {
            progressStore.add(studentId + ":" + materialId);
            return true;
        }

        @Override
        public boolean isCompleted(Long studentId, Long materialId) {
            return progressStore.contains(studentId + ":" + materialId);
        }

        @Override
        public Set<Long> getCompletedMaterialIds(Long studentId, Long topicId) {
            Set<Long> completed = new HashSet<>();
            for (StudyMaterial m : findPublishedByTopicId(topicId)) {
                if (progressStore.contains(studentId + ":" + m.getId())) {
                    completed.add(m.getId());
                }
            }
            return completed;
        }
    }
}
