package com.onlineexam.web.service.ai;

import com.onlineexam.dao.*;
import com.onlineexam.model.*;
import com.onlineexam.web.dto.AiDto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AiServiceTest {

    private AiService aiService;
    private StubStudyMaterialDAO stubStudyMaterialDAO;
    private StubTopicDAO stubTopicDAO;
    private StubSubjectDAO stubSubjectDAO;
    private StubStudentExamDAO stubStudentExamDAO;
    private StubExamDAO stubExamDAO;
    private StubQuestionDAO stubQuestionDAO;

    @BeforeEach
    void setUp() {
        stubStudyMaterialDAO = new StubStudyMaterialDAO();
        stubTopicDAO = new StubTopicDAO();
        stubSubjectDAO = new StubSubjectDAO();
        stubStudentExamDAO = new StubStudentExamDAO();
        stubExamDAO = new StubExamDAO();
        stubQuestionDAO = new StubQuestionDAO();

        aiService = new AiService(
                "", // no online key (test offline fallback)
                "OFFLINE",
                "gemini-1.5-flash",
                stubStudyMaterialDAO,
                stubTopicDAO,
                stubSubjectDAO,
                stubStudentExamDAO,
                stubExamDAO,
                stubQuestionDAO
        );

        Topic topic = new Topic(1L, 1L, "Java Concurrency", "Threads & Locks", 1, true, null, null);
        stubTopicDAO.store.put(1L, topic);

        StudyMaterial mat = new StudyMaterial(1L, 1L, "ScheduledExecutorService Guide",
                "Use ScheduledExecutorService for periodic drift-free timers.",
                null, null, null, 1, true, 1L, null, null);
        stubStudyMaterialDAO.store.put(1L, mat);
    }

    @Test
    @DisplayName("Offline Study Assistant synthesizes response based on topic study materials")
    void testAskStudyAssistantOfflineFallback() throws SQLException {
        AiStudyResponse response = aiService.askStudyAssistant(1L, "How do periodic timers work?");
        assertNotNull(response);
        assertEquals("Java Concurrency", response.topicName());
        assertTrue(response.answer().contains("ScheduledExecutorService Guide"));
        assertTrue(response.providerStatus().contains("Offline Knowledge Engine"));
    }

    @Test
    @DisplayName("Draft Question generates validated 4-option MCQ structure")
    void testDraftQuestion() throws SQLException {
        AiDraftQuestionResponse response = aiService.draftQuestion(1L, "MEDIUM", "Focus on thread pools");
        assertNotNull(response);
        assertNotNull(response.questionText());
        assertNotNull(response.optionA());
        assertNotNull(response.optionB());
        assertNotNull(response.optionC());
        assertNotNull(response.optionD());
        assertNotNull(response.correctOption());
        assertTrue(List.of("A", "B", "C", "D").contains(response.correctOption()));
        assertNotNull(response.explanation());
    }

    @Test
    @DisplayName("Personalized Recommendations identify low scoring topics")
    void testPersonalizedRecommendations() throws SQLException {
        // Mock a failed attempt for student 100 on topic 1
        Exam exam = new Exam();
        exam.setId(10L);
        exam.setTitle("Concurrency Test");
        exam.setDescription("Desc");
        exam.setDurationMinutes(10);
        exam.setTotalMarks(10);
        exam.setPassingMarks(8);
        exam.setActive(true);
        exam.setSubjectId(1L);
        exam.setTopicId(1L);
        exam.setAssessmentType(Exam.AssessmentType.FORMAL_EXAM);
        stubExamDAO.store.put(10L, exam);

        StudentExam attempt = new StudentExam(
                500L, 100L, 10L,
                java.time.LocalDateTime.now().minusMinutes(10),
                java.time.LocalDateTime.now(),
                0, 4, StudentExam.ExamStatus.SUBMITTED,
                java.time.LocalDateTime.now()
        );
        stubStudentExamDAO.store.put(500L, attempt);

        List<AiRecommendationDto> recs = aiService.getPersonalizedRecommendations(100L);
        assertFalse(recs.isEmpty());
        assertEquals(1L, recs.get(0).topicId());
        assertEquals("Critical", recs.get(0).priority());
        assertTrue(recs.get(0).reason().contains("40.0%"));
    }

    // --- Stubs ---
    private static class StubTopicDAO extends TopicDAO {
        final Map<Long, Topic> store = new HashMap<>();
        @Override public Optional<Topic> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Topic> findAll() { return new ArrayList<>(store.values()); }
    }

    private static class StubSubjectDAO extends SubjectDAO {
        @Override public Optional<Subject> findById(Long id) {
            Subject s = new Subject("Computer Science", "CS", true);
            s.setId(id);
            return Optional.of(s);
        }
    }


    private static class StubStudyMaterialDAO extends StudyMaterialDAO {
        final Map<Long, StudyMaterial> store = new HashMap<>();
        @Override public List<StudyMaterial> findPublishedByTopicId(Long topicId) {
            return store.values().stream().filter(m -> Objects.equals(m.getTopicId(), topicId)).toList();
        }
        @Override public List<StudyMaterial> findByTopicId(Long topicId) {
            return store.values().stream().filter(m -> Objects.equals(m.getTopicId(), topicId)).toList();
        }
    }

    private static class StubStudentExamDAO extends StudentExamDAO {
        final Map<Long, StudentExam> store = new HashMap<>();
        @Override public List<StudentExam> findByStudentId(Long studentId) {
            return store.values().stream().filter(se -> Objects.equals(se.getStudentId(), studentId)).toList();
        }
        @Override public List<StudentExam> findAll() { return new ArrayList<>(store.values()); }
    }

    private static class StubExamDAO extends ExamDAO {
        final Map<Long, Exam> store = new HashMap<>();
        @Override public Optional<Exam> findById(Long id) { return Optional.ofNullable(store.get(id)); }
    }

    private static class StubQuestionDAO extends QuestionDAO {}
}
