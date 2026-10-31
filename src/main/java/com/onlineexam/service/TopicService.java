package com.onlineexam.service;

import com.onlineexam.dao.TopicDAO;
import com.onlineexam.model.Topic;
import com.onlineexam.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Service managing learning topics with role-based authorization.
 */
public class TopicService {

    private static final Logger LOGGER = Logger.getLogger(TopicService.class.getName());
    private final TopicDAO topicDAO;

    public TopicService() {
        this(new TopicDAO());
    }

    public TopicService(TopicDAO topicDAO) {
        if (topicDAO == null) throw new IllegalArgumentException("TopicDAO cannot be null.");
        this.topicDAO = topicDAO;
    }

    public List<Topic> getPublishedTopics(Long subjectId) throws SQLException {
        return topicDAO.findPublishedBySubjectId(subjectId);
    }

    public List<Topic> getAllTopics(User user, Long subjectId) throws SQLException {
        assertAdmin(user);
        if (subjectId != null) {
            return topicDAO.findBySubjectId(subjectId);
        }
        return topicDAO.findAll();
    }

    public Optional<Topic> getTopicById(Long id) throws SQLException {
        return topicDAO.findById(id);
    }

    public Topic createTopic(User admin, Topic topic) throws SQLException {
        assertAdmin(admin);
        if (topic == null) throw new IllegalArgumentException("Topic cannot be null.");
        return topicDAO.create(topic);
    }

    public boolean updateTopic(User admin, Topic topic) throws SQLException {
        assertAdmin(admin);
        if (topic == null || topic.getId() == null) {
            throw new IllegalArgumentException("Topic and ID cannot be null.");
        }
        return topicDAO.update(topic);
    }

    public boolean deleteTopic(User admin, Long id) throws SQLException {
        assertAdmin(admin);
        if (id == null) return false;

        int matCount = topicDAO.countMaterials(id);
        int examCount = topicDAO.countExams(id);
        if (matCount > 0 || examCount > 0) {
            throw new IllegalStateException("Cannot delete topic: It has " + matCount +
                    " related study material(s) and " + examCount + " examination(s). Remove or reassign them first.");
        }

        return topicDAO.delete(id);
    }

    private void assertAdmin(User user) {
        if (user == null || user.getRole() != User.Role.ADMIN) {
            throw new SecurityException("Access denied: Administrator privileges required.");
        }
    }
}
