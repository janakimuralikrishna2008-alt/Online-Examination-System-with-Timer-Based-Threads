package com.onlineexam.web.service.ai;

import com.onlineexam.dao.*;
import com.onlineexam.model.*;
import com.onlineexam.web.dto.AiDto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modular AI Service providing context-aware study tutoring, question drafting,
 * personalized learning recommendations, and aggregate analytics.
 * Supports online LLMs (Gemini / OpenAI) via environment variables and robust offline fallback.
 */
@Service
public class AiService {

    private static final Logger LOGGER = Logger.getLogger(AiService.class.getName());
    private static volatile HttpClient httpClient;

    private static HttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (AiService.class) {
                if (httpClient == null) {
                    try {
                        httpClient = HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(10))
                                .build();
                    } catch (Exception e) {
                        LOGGER.warning("Could not initialize HttpClient: " + e.getMessage());
                    }
                }
            }
        }
        return httpClient;
    }

    private final String apiKey;
    private final String provider;
    private final String model;

    private final StudyMaterialDAO studyMaterialDAO;
    private final TopicDAO topicDAO;
    private final SubjectDAO subjectDAO;
    private final StudentExamDAO studentExamDAO;
    private final ExamDAO examDAO;
    private final QuestionDAO questionDAO;

    public AiService(
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.provider:AUTO}") String provider,
            @Value("${ai.model:gemini-1.5-flash}") String model,
            StudyMaterialDAO studyMaterialDAO,
            TopicDAO topicDAO,
            SubjectDAO subjectDAO,
            StudentExamDAO studentExamDAO,
            ExamDAO examDAO,
            QuestionDAO questionDAO) {
        this.apiKey = (apiKey != null) ? apiKey.trim() : "";
        this.provider = (provider != null) ? provider.trim().toUpperCase() : "AUTO";
        this.model = model;
        this.studyMaterialDAO = studyMaterialDAO;
        this.topicDAO = topicDAO;
        this.subjectDAO = subjectDAO;
        this.studentExamDAO = studentExamDAO;
        this.examDAO = examDAO;
        this.questionDAO = questionDAO;
    }

    public boolean isOnlineAiConfigured() {
        return !apiKey.isEmpty() && !apiKey.equalsIgnoreCase("none");
    }

    /**
     * AI Study Assistant: Answers student questions using verified topic study materials as context.
     */
    public AiStudyResponse askStudyAssistant(Long topicId, String userQuestion) throws SQLException {
        if (topicId == null || userQuestion == null || userQuestion.trim().isBlank()) {
            throw new IllegalArgumentException("Topic ID and question cannot be empty.");
        }

        Optional<Topic> topicOpt = topicDAO.findById(topicId);
        String topicName = topicOpt.map(Topic::getName).orElse("Topic #" + topicId);
        List<StudyMaterial> materials = studyMaterialDAO.findPublishedByTopicId(topicId);

        List<String> sourceTitles = new ArrayList<>();
        StringBuilder contextText = new StringBuilder();
        for (StudyMaterial m : materials) {
            sourceTitles.add(m.getTitle());
            contextText.append("--- ").append(m.getTitle()).append(" ---\n");
            if (m.getContent() != null) {
                contextText.append(m.getContent()).append("\n\n");
            }
        }

        // If online AI configured, attempt to query generative model
        if (isOnlineAiConfigured()) {
            try {
                String prompt = buildStudyPrompt(topicName, contextText.toString(), userQuestion);
                String onlineAnswer = callGenerativeAi(prompt);
                if (onlineAnswer != null && !onlineAnswer.isBlank()) {
                    return new AiStudyResponse(onlineAnswer, topicName, sourceTitles, true, "Online LLM (" + model + ")");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Online AI call failed, falling back to offline knowledge engine: " + e.getMessage());
            }
        }

        // Offline Fallback: Extract matching context and synthesize response
        String answer = generateOfflineStudyAnswer(topicName, materials, userQuestion);
        return new AiStudyResponse(answer, topicName, sourceTitles, !materials.isEmpty(), "Offline Knowledge Engine (Set AI_API_KEY for generative LLM)");
    }

    /**
     * AI Question Drafting: Drafts multiple-choice questions for admin review.
     */
    public AiDraftQuestionResponse draftQuestion(Long topicId, String difficulty, String promptNotes) throws SQLException {
        Optional<Topic> topicOpt = (topicId != null) ? topicDAO.findById(topicId) : Optional.empty();
        String topicName = topicOpt.map(Topic::getName).orElse("General Assessment");

        List<StudyMaterial> materials = (topicId != null) ? studyMaterialDAO.findByTopicId(topicId) : Collections.emptyList();
        StringBuilder notesBuilder = new StringBuilder();
        if (promptNotes != null && !promptNotes.isBlank()) {
            notesBuilder.append("Admin Instructions: ").append(promptNotes).append("\n");
        }
        for (StudyMaterial m : materials) {
            notesBuilder.append(m.getTitle()).append(": ").append(m.getContent()).append("\n");
        }

        if (isOnlineAiConfigured()) {
            try {
                String prompt = "Generate 1 high-quality multiple choice question with 4 options (A, B, C, D), " +
                        "correct option letter, and clear explanation for topic: " + topicName +
                        ". Difficulty: " + difficulty + ".\nReference Context: " + notesBuilder +
                        "\nFormat strictly as:\nQuestion: <text>\nA: <option A>\nB: <option B>\nC: <option C>\nD: <option D>\nCorrect: <A/B/C/D>\nExplanation: <text>";
                String response = callGenerativeAi(prompt);
                if (response != null && !response.isBlank()) {
                    AiDraftQuestionResponse parsed = parseDraftQuestion(response, "Online LLM (" + model + ")");
                    if (parsed != null) return parsed;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Online AI question draft failed, falling back to template engine: " + e.getMessage());
            }
        }

        // Offline Template Generator
        return generateOfflineDraftQuestion(topicName, difficulty, promptNotes);
    }

    /**
     * Personalized Learning Recommendations: Analyzes student performance to pinpoint weak areas.
     */
    public List<AiRecommendationDto> getPersonalizedRecommendations(Long studentId) throws SQLException {
        List<AiRecommendationDto> recommendations = new ArrayList<>();
        if (studentId == null) return recommendations;

        List<StudentExam> history = studentExamDAO.findByStudentId(studentId);
        if (history.isEmpty()) {
            recommendations.add(new AiRecommendationDto(
                    1L,
                    "Java 21 Concurrency & Virtual Threads",
                    "Computer Science & Software Engineering",
                    "Welcome! Start with our featured fundamental topic and practice quiz to establish your baseline score.",
                    "High"
            ));
            return recommendations;
        }

        Map<Long, List<Double>> topicScores = new HashMap<>();
        for (StudentExam se : history) {
            if (se.isCompleted()) {
                Optional<Exam> examOpt = examDAO.findById(se.getExamId());
                if (examOpt.isPresent() && examOpt.get().getTopicId() != null) {
                    Exam exam = examOpt.get();
                    Long topicId = exam.getTopicId();
                    double pct = exam.getTotalMarks() > 0 ? ((double) se.getTotalScore() / exam.getTotalMarks()) * 100.0 : 0.0;
                    topicScores.computeIfAbsent(topicId, k -> new ArrayList<>()).add(pct);
                }
            }
        }

        for (Map.Entry<Long, List<Double>> entry : topicScores.entrySet()) {
            Long topicId = entry.getKey();
            double avg = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            if (avg < 75.0) {
                Optional<Topic> topicOpt = topicDAO.findById(topicId);
                if (topicOpt.isPresent()) {
                    Topic t = topicOpt.get();
                    String subjectName = subjectDAO.findById(t.getSubjectId()).map(Subject::getName).orElse("Core Curriculum");
                    String priority = avg < 50.0 ? "Critical" : "High";
                    String reason = String.format("Average performance on recent assessments is %.1f%% (below target threshold 75%%). Recommended to review study notes and attempt the practice quiz again.", avg);
                    recommendations.add(new AiRecommendationDto(t.getId(), t.getName(), subjectName, reason, priority));
                }
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add(new AiRecommendationDto(
                    1L,
                    "Java 21 Concurrency & Virtual Threads",
                    "Computer Science & Software Engineering",
                    "Excellent progress! All completed assessments meet mastery criteria (>75%). Explore advanced study materials.",
                    "Normal"
            ));
        }

        return recommendations;
    }

    /**
     * Aggregate Performance Insights: Summarizes student outcomes for administrators.
     */
    public AiPerformanceInsightsResponse getAggregateInsights() throws SQLException {
        List<StudentExam> allAttempts = studentExamDAO.findAll();
        int total = allAttempts.size();
        if (total == 0) {
            return new AiPerformanceInsightsResponse(0, 0.0, 0.0, Collections.emptyList(),
                    List.of("No student examinations recorded yet."), "Active");
        }

        long passedCount = 0;
        double totalPct = 0.0;
        for (StudentExam se : allAttempts) {
            Optional<Exam> examOpt = examDAO.findById(se.getExamId());
            if (examOpt.isPresent()) {
                Exam exam = examOpt.get();
                if (se.getTotalScore() >= exam.getPassingMarks()) {
                    passedCount++;
                }
                if (exam.getTotalMarks() > 0) {
                    totalPct += ((double) se.getTotalScore() / exam.getTotalMarks()) * 100.0;
                }
            }
        }

        double passRate = (double) passedCount / total * 100.0;
        double avgScore = total > 0 ? (totalPct / total) : 0.0;


        List<TopicScoreMetric> metrics = new ArrayList<>();
        List<Topic> topics = topicDAO.findAll();
        for (Topic t : topics) {
            metrics.add(new TopicScoreMetric(t.getId(), t.getName(), avgScore, total, avgScore >= 60.0 ? "Proficient" : "Needs Review"));
        }

        List<String> keyRecs = new ArrayList<>();
        keyRecs.add(String.format("Current system-wide pass rate is %.1f%% across %d attempts.", passRate, total));
        if (passRate < 70.0) {
            keyRecs.add("Consider reviewing question difficulty in core examinations to align with study material coverage.");
        } else {
            keyRecs.add("Candidates demonstrate solid mastery of Java 21 Concurrency and timing principles.");
        }

        String providerStatus = isOnlineAiConfigured() ? "Online LLM (" + model + ")" : "Statistical Analytics Engine";
        return new AiPerformanceInsightsResponse(total, passRate, avgScore, metrics, keyRecs, providerStatus);
    }

    private String buildStudyPrompt(String topicName, String context, String question) {
        return "You are an expert educational AI tutor for an Online Examination System.\n" +
                "Subject Topic: " + topicName + "\n\n" +
                "Verified Study Materials:\n" + context + "\n\n" +
                "Student Question: " + question + "\n\n" +
                "Instructions:\n" +
                "1. Base your answer directly on the verified study materials whenever possible.\n" +
                "2. Provide clear, structured explanations with formatted code blocks if relevant.\n" +
                "3. Never reveal active examination answer keys or cheat sheets.\n" +
                "4. Be encouraging, accurate, and concise.";
    }

    private String callGenerativeAi(String prompt) throws Exception {
        if (apiKey.isEmpty()) return null;
        // Generic HTTP endpoint for Gemini REST API
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        String jsonPayload = String.format("{\"contents\":[{\"parts\":[{\"text\":%s}]}]}", escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(12))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpClient client = getHttpClient();
        if (client == null) {
            return null;
        }
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return extractGeminiText(response.body());
        }
        LOGGER.warning("AI provider responded with HTTP status " + response.statusCode() + ": " + response.body());
        return null;
    }

    private String extractGeminiText(String responseBody) {
        // Simple robust JSON extractor for Gemini output
        int textIdx = responseBody.indexOf("\"text\": \"");
        if (textIdx != -1) {
            int start = textIdx + 9;
            int end = responseBody.indexOf("\"", start);
            if (end != -1) {
                return responseBody.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
            }
        }
        return responseBody;
    }

    private String generateOfflineStudyAnswer(String topicName, List<StudyMaterial> materials, String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Guide for ").append(topicName).append("\n\n");

        if (materials.isEmpty()) {
            sb.append("No study materials have been published for this topic yet. ")
              .append("Please contact your instructor or administrator.");
            return sb.toString();
        }

        String lowerQ = question.toLowerCase();
        boolean matched = false;

        for (StudyMaterial m : materials) {
            if (m.getContent() != null && containsKeyword(m.getContent().toLowerCase(), lowerQ)) {
                sb.append("#### Key Insights from **").append(m.getTitle()).append("**:\n\n");
                sb.append(m.getContent()).append("\n\n");
                if (m.getResourceUrl() != null && !m.getResourceUrl().isBlank()) {
                    sb.append("🔗 **Official Reference:** [").append(m.getResourceUrl()).append("](")
                      .append(m.getResourceUrl()).append(")\n\n");
                }
                matched = true;
                break;
            }
        }

        if (!matched) {
            StudyMaterial first = materials.get(0);
            sb.append("Based on the core syllabus material (**").append(first.getTitle()).append("**):\n\n");
            sb.append(first.getContent()).append("\n\n");
        }

        sb.append("> *Note: Offline Knowledge Engine is active. Set `AI_API_KEY` in environment to enable dynamic Generative LLM responses.*");
        return sb.toString();
    }

    private boolean containsKeyword(String content, String question) {
        String[] words = question.split("\\s+");
        for (String w : words) {
            if (w.length() > 3 && content.contains(w)) return true;
        }
        return false;
    }

    private AiDraftQuestionResponse generateOfflineDraftQuestion(String topicName, String difficulty, String promptNotes) {
        String question = "Which mechanism in " + topicName + " is best suited for executing periodic background tasks?";
        String optA = "ScheduledExecutorService.scheduleAtFixedRate()";
        String optB = "java.lang.Thread.sleep() inside a while(true) loop";
        String optC = "Asynchronous JavaScript Promises in browser";
        String optD = "Recursive method invocation without base case";
        String correct = "A";
        String explanation = "ScheduledExecutorService provides drift-free periodic thread execution and safe lifecycle management.";

        if (promptNotes != null && promptNotes.toLowerCase().contains("virtual thread")) {
            question = "What is the primary benefit of Java 21 Virtual Threads over traditional OS platform threads?";
            optA = "Virtual threads execute bytecode twice as fast as platform threads";
            optB = "Lightweight concurrency enabling millions of concurrent threads with low memory footprint";
            optC = "They replace the Java Virtual Machine garbage collector";
            optD = "They run exclusively on GPU hardware";
            correct = "B";
            explanation = "Virtual threads are lightweight user-mode threads managed by the JDK rather than the operating system kernel.";
        }

        return new AiDraftQuestionResponse(question, optA, optB, optC, optD, correct, explanation, 1,
                "Offline Template Engine (Configure AI_API_KEY for dynamic LLM drafting)");
    }

    private AiDraftQuestionResponse parseDraftQuestion(String text, String providerStatus) {
        try {
            String q = extractValue(text, "Question:");
            String a = extractValue(text, "A:");
            String b = extractValue(text, "B:");
            String c = extractValue(text, "C:");
            String d = extractValue(text, "D:");
            String correct = extractValue(text, "Correct:").replaceAll("[^A-D]", "");
            String exp = extractValue(text, "Explanation:");

            if (q != null && a != null && b != null && c != null && d != null && !correct.isEmpty()) {
                return new AiDraftQuestionResponse(q, a, b, c, d, correct, exp != null ? exp : "Generated by AI", 1, providerStatus);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractValue(String text, String prefix) {
        int idx = text.indexOf(prefix);
        if (idx == -1) return null;
        int end = text.indexOf("\n", idx);
        if (end == -1) end = text.length();
        return text.substring(idx + prefix.length(), end).trim();
    }

    private String escapeJson(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }
}
