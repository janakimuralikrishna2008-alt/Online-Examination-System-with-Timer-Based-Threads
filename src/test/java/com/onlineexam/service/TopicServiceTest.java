package com.onlineexam.service;

import com.onlineexam.dao.TopicDAO;
import com.onlineexam.model.Topic;
import com.onlineexam.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TopicService} verifying topic operations and authorization.
 */
public class TopicServiceTest {

    private TopicService topicService;
    private StubTopicDAO stubTopicDAO;
    private User admin;
    private User student;

    @BeforeEach
    void setUp() {
        stubTopicDAO = new StubTopicDAO();
        topicService = new TopicService(stubTopicDAO);
        admin = new User(1L, "admin", "hash", "Admin User", "admin@test.com", User.Role.ADMIN, null, null);
        student = new User(2L, "student1", "hash", "Student User", "student@test.com", User.Role.STUDENT, null, null);
    }

    @Test
    @DisplayName("Non-administrator access to topic modification is rejected with SecurityException")
    void testNonAdminAccessRejected() {
        Topic t = new Topic(1L, "Data Structures", "Trees and Graphs", 1, true);
        assertThrows(SecurityException.class, () -> topicService.createTopic(student, t));
        assertThrows(SecurityException.class, () -> topicService.getAllTopics(student, 1L));
        assertThrows(SecurityException.class, () -> topicService.updateTopic(student, t));
        assertThrows(SecurityException.class, () -> topicService.deleteTopic(student, 1L));
    }

    @Test
    @DisplayName("Administrator can create and retrieve topics successfully")
    void testCreateAndRetrieveTopics() throws SQLException {
        Topic t1 = new Topic(1L, "Concurrency", "Threads", 1, true);
        Topic t2 = new Topic(1L, "Streams API", "Lambdas", 2, true);

        Topic created1 = topicService.createTopic(admin, t1);
        Topic created2 = topicService.createTopic(admin, t2);

        assertNotNull(created1.getId());
        assertNotNull(created2.getId());

        List<Topic> published = topicService.getPublishedTopics(1L);
        assertEquals(2, published.size());
    }

    @Test
    @DisplayName("Topic deletion blocked when materials or exams exist")
    void testTopicDeletionBlockedWhenDependenciesExist() throws SQLException {
        Topic t = new Topic(1L, "Databases", "SQL", 1, true);
        Topic created = topicService.createTopic(admin, t);

        stubTopicDAO.matCount = 3;
        assertThrows(IllegalStateException.class, () -> topicService.deleteTopic(admin, created.getId()));

        stubTopicDAO.matCount = 0;
        stubTopicDAO.examCount = 1;
        assertThrows(IllegalStateException.class, () -> topicService.deleteTopic(admin, created.getId()));

        stubTopicDAO.examCount = 0;
        assertTrue(topicService.deleteTopic(admin, created.getId()));
    }

    // --- In-Memory Test Stub ---
    private static class StubTopicDAO extends TopicDAO {
        private final Map<Long, Topic> store = new HashMap<>();
        int matCount = 0;
        int examCount = 0;

        @Override
        public Optional<Topic> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Topic> findBySubjectId(Long subjectId) {
            return store.values().stream().filter(t -> t.getSubjectId().equals(subjectId)).toList();
        }

        @Override
        public List<Topic> findPublishedBySubjectId(Long subjectId) {
            return store.values().stream().filter(t -> t.getSubjectId().equals(subjectId) && t.isPublished()).toList();
        }

        @Override
        public List<Topic> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public Topic create(Topic topic) {
            long newId = store.size() + 10L;
            topic.setId(newId);
            store.put(newId, topic);
            return topic;
        }

        @Override
        public boolean update(Topic topic) {
            if (store.containsKey(topic.getId())) {
                store.put(topic.getId(), topic);
                return true;
            }
            return false;
        }

        @Override
        public boolean delete(Long id) {
            return store.remove(id) != null;
        }

        @Override
        public int countMaterials(Long topicId) {
            return matCount;
        }

        @Override
        public int countExams(Long topicId) {
            return examCount;
        }
    }
}
