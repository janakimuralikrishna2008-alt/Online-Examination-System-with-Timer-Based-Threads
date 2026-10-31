package com.onlineexam.service;

import com.onlineexam.dao.StudyMaterialDAO;
import com.onlineexam.model.StudyMaterial;
import com.onlineexam.model.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing learning materials, safe attachments, and student study progress.
 */
public class LearningService {

    private static final Logger LOGGER = Logger.getLogger(LearningService.class.getName());
    private static final long MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "txt", "md", "doc", "docx", "png", "jpg", "jpeg", "zip"
    );

    private final StudyMaterialDAO studyMaterialDAO;
    private final Path storageDirectory;

    public LearningService() {
        this(new StudyMaterialDAO(), Paths.get(System.getProperty("user.dir"), "storage", "materials"));
    }

    public LearningService(StudyMaterialDAO studyMaterialDAO) {
        this(studyMaterialDAO, Paths.get(System.getProperty("user.dir"), "storage", "materials"));
    }

    public LearningService(StudyMaterialDAO studyMaterialDAO, Path storageDirectory) {
        if (studyMaterialDAO == null) throw new IllegalArgumentException("StudyMaterialDAO cannot be null.");
        this.studyMaterialDAO = studyMaterialDAO;
        this.storageDirectory = storageDirectory;
        ensureStorageDirectoryExists();
    }

    private void ensureStorageDirectoryExists() {
        try {
            if (!Files.exists(storageDirectory)) {
                Files.createDirectories(storageDirectory);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not initialize materials storage directory: " + storageDirectory, e);
        }
    }

    public List<StudyMaterial> getPublishedMaterials(Long topicId) throws SQLException {
        return studyMaterialDAO.findPublishedByTopicId(topicId);
    }

    public List<StudyMaterial> getAllMaterials(User user, Long topicId) throws SQLException {
        assertAdmin(user);
        return studyMaterialDAO.findByTopicId(topicId);
    }

    public Optional<StudyMaterial> getMaterialById(Long id) throws SQLException {
        return studyMaterialDAO.findById(id);
    }

    public StudyMaterial createMaterial(User admin, StudyMaterial material) throws SQLException {
        assertAdmin(admin);
        if (material == null) throw new IllegalArgumentException("Material cannot be null.");
        material.setCreatedBy(admin.getId());
        return studyMaterialDAO.create(material);
    }

    public boolean updateMaterial(User admin, StudyMaterial material) throws SQLException {
        assertAdmin(admin);
        if (material == null || material.getId() == null) {
            throw new IllegalArgumentException("Material and ID cannot be null.");
        }
        return studyMaterialDAO.update(material);
    }

    public boolean deleteMaterial(User admin, Long id) throws SQLException {
        assertAdmin(admin);
        if (id == null) return false;
        return studyMaterialDAO.delete(id);
    }

    // --- Student Progress Operations ---

    public boolean markMaterialStudied(Long studentId, Long materialId) throws SQLException {
        if (studentId == null || materialId == null) return false;
        return studyMaterialDAO.markCompleted(studentId, materialId);
    }

    public boolean setMaterialProgress(Long studentId, Long materialId, boolean completed) throws SQLException {
        if (studentId == null || materialId == null) return false;
        return studyMaterialDAO.setCompleted(studentId, materialId, completed);
    }

    public int getCompletedMaterialsCount(Long studentId) throws SQLException {
        if (studentId == null) return 0;
        return studyMaterialDAO.countCompletedByStudentId(studentId);
    }

    public boolean isMaterialStudied(Long studentId, Long materialId) throws SQLException {
        if (studentId == null || materialId == null) return false;
        return studyMaterialDAO.isCompleted(studentId, materialId);
    }

    public Set<Long> getCompletedMaterialIds(Long studentId, Long topicId) throws SQLException {
        return studyMaterialDAO.getCompletedMaterialIds(studentId, topicId);
    }

    /**
     * Calculates completion percentage for a topic (0 to 100).
     */
    public double getTopicProgressPercentage(Long studentId, Long topicId) throws SQLException {
        if (studentId == null || topicId == null) return 0.0;
        List<StudyMaterial> materials = studyMaterialDAO.findPublishedByTopicId(topicId);
        if (materials.isEmpty()) return 100.0; // No materials means fully ready

        Set<Long> completed = studyMaterialDAO.getCompletedMaterialIds(studentId, topicId);
        return ((double) completed.size() / materials.size()) * 100.0;
    }

    // --- Safe File Attachment Validation & Storage ---

    /**
     * Validates and saves an attachment file safely preventing path traversal and unsafe code execution.
     *
     * @param sourceFile source file chosen by administrator
     * @return relative stored file path
     * @throws IOException if file reading or copying fails
     * @throws IllegalArgumentException if file validation fails
     */
    public String saveAttachmentSafely(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            throw new IllegalArgumentException("Selected file does not exist or is invalid.");
        }

        if (sourceFile.length() > MAX_ATTACHMENT_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum permitted limit (10 MB).");
        }

        String fileName = sourceFile.getName();
        String extension = getFileExtension(fileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type '." + extension + "' is not permitted. Allowed: " + ALLOWED_EXTENSIONS);
        }

        // Generate safe unique filename
        String safeBaseName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String uniqueFileName = UUID.randomUUID().toString().substring(0, 8) + "_" + safeBaseName;

        Path targetPath = storageDirectory.resolve(uniqueFileName).normalize();
        if (!targetPath.startsWith(storageDirectory)) {
            throw new SecurityException("Potential path traversal detected in file storage.");
        }

        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Attachment stored safely at: " + targetPath);

        // Return relative path for portability
        return "storage/materials/" + uniqueFileName;
    }

    private String getFileExtension(String name) {
        int idx = name.lastIndexOf('.');
        return (idx > 0 && idx < name.length() - 1) ? name.substring(idx + 1) : "";
    }

    private void assertAdmin(User user) {
        if (user == null || user.getRole() != User.Role.ADMIN) {
            throw new SecurityException("Access denied: Administrator privileges required.");
        }
    }
}
