package com.onlineexam.service;

import com.onlineexam.config.DatabaseConnection;
import com.onlineexam.dao.ExamDAO;
import com.onlineexam.dao.StudyMaterialDAO;
import com.onlineexam.dao.SubjectDAO;
import com.onlineexam.dao.TopicDAO;
import com.onlineexam.model.Subject;
import com.onlineexam.model.Topic;
import com.onlineexam.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service managing academic subjects with role-based authorization.
 *
 * <p>Deletion strategy:
 * <ul>
 *   <li>A subject that has student exam <em>attempts</em> (rows in student_exams) can NEVER be
 *       force-deleted because the cascade would destroy irreplaceable student history.</li>
 *   <li>A subject that has topics / exams but NO student history <em>may</em> be
 *       force-deleted by an administrator after explicit confirmation.  The deletion runs in a
 *       single JDBC transaction so it is atomic – any failure rolls back completely.</li>
 *   <li>A subject with no dependents can be deleted normally.</li>
 * </ul>
 */
public class SubjectService {

    private static final Logger LOGGER = Logger.getLogger(SubjectService.class.getName());
    private final SubjectDAO subjectDAO;

    public SubjectService() {
        this(new SubjectDAO());
    }

    public SubjectService(SubjectDAO subjectDAO) {
        if (subjectDAO == null) throw new IllegalArgumentException("SubjectDAO cannot be null.");
        this.subjectDAO = subjectDAO;
    }

    public List<Subject> getPublishedSubjects() throws SQLException {
        return subjectDAO.findAllPublished();
    }

    public List<Subject> getAllSubjects(User user) throws SQLException {
        assertAdmin(user);
        return subjectDAO.findAll();
    }

    public Optional<Subject> getSubjectById(Long id) throws SQLException {
        return subjectDAO.findById(id);
    }

    public Subject createSubject(User admin, Subject subject) throws SQLException {
        assertAdmin(admin);
        if (subject == null) throw new IllegalArgumentException("Subject cannot be null.");
        subject.setCreatedBy(admin.getId());

        Optional<Subject> existing = subjectDAO.findByName(subject.getName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A subject with the name '" + subject.getName() + "' already exists.");
        }

        return subjectDAO.create(subject);
    }

    public boolean updateSubject(User admin, Subject subject) throws SQLException {
        assertAdmin(admin);
        if (subject == null || subject.getId() == null) {
            throw new IllegalArgumentException("Subject and ID cannot be null.");
        }

        Optional<Subject> existing = subjectDAO.findByName(subject.getName());
        if (existing.isPresent() && !existing.get().getId().equals(subject.getId())) {
            throw new IllegalArgumentException("Another subject with the name '" + subject.getName() + "' already exists.");
        }

        return subjectDAO.update(subject);
    }

    /**
     * Deletes a subject (simple path – no force).
     * Throws {@link IllegalStateException} if any dependents exist.
     */
    public boolean deleteSubject(User admin, Long id) throws SQLException {
        return deleteSubject(admin, id, false);
    }

    /**
     * Deletes a subject with an optional force flag.
     *
     * <p><strong>Rules enforced regardless of force flag:</strong>
     * <ol>
     *   <li>Student exam history (student_exams) is NEVER deleted automatically – blocks deletion.</li>
     * </ol>
     *
     * <p><strong>When {@code force=false}:</strong>
     * Blocks if any topics or exams exist; returns {@code IllegalStateException}.
     *
     * <p><strong>When {@code force=true} (no student history):</strong>
     * Runs inside a single JDBC transaction:
     * <ol>
     *   <li>Delete study_materials for each topic (explicit, same connection).</li>
     *   <li>Delete topics (same connection – CASCADE would handle materials but we're explicit).</li>
     *   <li>Detach exams from subject (SET subject_id=NULL) – preserves exams that have no attempts.</li>
     *   <li>Delete the subject row (same connection).</li>
     *   <li>Commit if all steps succeed; rollback on any error.</li>
     * </ol>
     *
     * @throws IllegalStateException if deletion is blocked by student history or dependents.
     * @throws SQLException          on database errors (triggers rollback).
     */
    public boolean deleteSubject(User admin, Long id, boolean force) throws SQLException {
        assertAdmin(admin);
        if (id == null) return false;

        int topicsCount   = subjectDAO.countTopics(id);
        int examsCount    = subjectDAO.countExams(id);
        int attemptsCount = subjectDAO.countStudentAttemptsBySubject(id);

        // HARD BLOCK: student history must never be silently destroyed
        if (attemptsCount > 0) {
            throw new IllegalStateException(
                "Cannot delete subject: It has " + attemptsCount +
                " student exam attempt(s) on record. " +
                "Student examination history must not be deleted. " +
                "To remove this subject, first retire (deactivate) its exams and archive the history.");
        }

        // Without force: block if any structural dependents exist
        if (!force && (topicsCount > 0 || examsCount > 0)) {
            throw new IllegalStateException(
                "Cannot delete subject: It has " + topicsCount +
                " related topic(s) and " + examsCount +
                " examination(s). Remove or reassign them first, or use force deletion after reviewing the dependent content.");
        }

        // Simple path – no dependents, no force needed
        if (topicsCount == 0 && examsCount == 0) {
            return subjectDAO.delete(id);
        }

        // Force cascade path – all operations run inside a single JDBC transaction
        StudyMaterialDAO materialDAO = new StudyMaterialDAO();
        TopicDAO topicDAO = new TopicDAO();
        ExamDAO examDAO = new ExamDAO();

        Connection conn = DatabaseConnection.getConnection();
        try {
            conn.setAutoCommit(false);

            // Step 1: Detach exams from this subject or its topics (SET subject_id=NULL, topic_id=NULL).
            //         Must be done first while topics still reference subject_id in the database.
            //         Exams have no attempts (verified above) and are preserved.
            int examsDetached = examDAO.detachFromSubject(conn, id);
            LOGGER.info("Force-delete: detached " + examsDetached + " exam(s) from subject " + id);

            // Step 2: Delete study materials for each topic using the same connection
            List<Topic> topics = subjectDAO.findTopicsBySubject(conn, id);
            for (Topic t : topics) {
                int materialsDeleted = materialDAO.deleteByTopicId(conn, t.getId());
                LOGGER.info("Force-delete: removed " + materialsDeleted + " material(s) from topic " + t.getId());
            }

            // Step 3: Delete topics using the same connection
            int topicsDeleted = topicDAO.deleteBySubjectId(conn, id);
            LOGGER.info("Force-delete: removed " + topicsDeleted + " topic(s) from subject " + id);

            // Step 4: Delete the subject row using the same transactional connection
            boolean deleted = subjectDAO.delete(conn, id);
            LOGGER.info("Force-delete: subject " + id + " deleted=" + deleted);

            conn.commit();
            return deleted;

        } catch (Exception ex) {
            try { conn.rollback(); } catch (SQLException rollbackEx) {
                LOGGER.severe("Rollback failed after deletion error: " + rollbackEx.getMessage());
            }
            LOGGER.severe("Force-delete of subject " + id + " failed and was rolled back: " + ex.getMessage());
            throw (ex instanceof SQLException) ? (SQLException) ex : new SQLException("Force delete failed: " + ex.getMessage(), ex);
        } finally {
            try { conn.setAutoCommit(true); conn.close(); } catch (SQLException closeEx) {
                LOGGER.warning("Failed to restore connection state: " + closeEx.getMessage());
            }
        }
    }

    private void assertAdmin(User user) {
        if (user == null || user.getRole() != User.Role.ADMIN) {
            throw new SecurityException("Access denied: Administrator privileges required.");
        }
    }
}
