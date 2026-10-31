package com.onlineexam.service;

import com.onlineexam.model.Exam;
import com.onlineexam.model.ExamResult;
import com.onlineexam.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EvaluationService}.
 * Evaluates candidate responses against question answer keys without requiring a live database.
 */
public class EvaluationServiceTest {

    private EvaluationService evaluationService;
    private Exam sampleExam;
    private List<Question> sampleQuestions;

    @BeforeEach
    void setUp() {
        evaluationService = new EvaluationService();
        sampleExam = new Exam(1L, "Java Concurrency", "Assessment", 15, 10, 6, true, null, null);

        Question q1 = new Question(1L, 1L, "Q1", "A", "B", "C", "D", Question.Option.A, 2, "Exp 1", null);
        Question q2 = new Question(2L, 1L, "Q2", "A", "B", "C", "D", Question.Option.B, 3, "Exp 2", null);
        Question q3 = new Question(3L, 1L, "Q3", "A", "B", "C", "D", Question.Option.C, 5, "Exp 3", null);

        sampleQuestions = List.of(q1, q2, q3); // Total marks = 2 + 3 + 5 = 10
    }

    @Test
    @DisplayName("All correct answers yield 100% score and passed status")
    void testAllCorrectAnswers() {
        Map<Long, Question.Option> answers = Map.of(
                1L, Question.Option.A,
                2L, Question.Option.B,
                3L, Question.Option.C
        );

        ExamResult result = evaluationService.evaluate(100L, 10L, sampleExam, sampleQuestions, answers, false);

        assertEquals(3, result.totalQuestions());
        assertEquals(3, result.correctCount());
        assertEquals(0, result.incorrectCount());
        assertEquals(0, result.unansweredCount());
        assertEquals(10, result.earnedMarks());
        assertEquals(10, result.totalPossibleMarks());
        assertEquals(100.0, result.percentage());
        assertTrue(result.passed());
        assertFalse(result.autoSubmitted());
    }

    @Test
    @DisplayName("Partial correct, incorrect, and unanswered questions are calculated accurately")
    void testMixedAnswers() {
        Map<Long, Question.Option> answers = new HashMap<>();
        answers.put(1L, Question.Option.A); // Correct (2 marks)
        answers.put(2L, Question.Option.D); // Incorrect (0 marks)
        // Q3 unanswered (0 marks)

        ExamResult result = evaluationService.evaluate(101L, 10L, sampleExam, sampleQuestions, answers, true);

        assertEquals(3, result.totalQuestions());
        assertEquals(1, result.correctCount());
        assertEquals(1, result.incorrectCount());
        assertEquals(1, result.unansweredCount());
        assertEquals(2, result.earnedMarks());
        assertEquals(10, result.totalPossibleMarks());
        assertEquals(20.0, result.percentage());
        assertFalse(result.passed(), "Score of 2 is below passing mark of 6");
        assertTrue(result.autoSubmitted());
    }

    @Test
    @DisplayName("All unanswered questions yield 0 score, 0 percentage, and fail status")
    void testAllUnanswered() {
        ExamResult result = evaluationService.evaluate(102L, 10L, sampleExam, sampleQuestions, Map.of(), false);

        assertEquals(3, result.totalQuestions());
        assertEquals(0, result.correctCount());
        assertEquals(0, result.incorrectCount());
        assertEquals(3, result.unansweredCount());
        assertEquals(0, result.earnedMarks());
        assertEquals(0.0, result.percentage());
        assertFalse(result.passed());
    }

    @Test
    @DisplayName("Exact passing score boundary marks the candidate as passed")
    void testExactPassingScoreBoundary() {
        // Passing marks is 6; Q2 (3) + Q3 (5) = 8 marks (passed); Q1 (2) + Q2 (3) = 5 (failed); Q1(2)+Q3(5)=7 (passed)
        // Let's create an exam where passing is 5
        Exam exactExam = new Exam(2L, "Exact", "Test", 10, 10, 5, true, null, null);
        Map<Long, Question.Option> answers = Map.of(
                1L, Question.Option.A, // 2 marks
                2L, Question.Option.B  // 3 marks -> total 5 marks
        );

        ExamResult result = evaluationService.evaluate(103L, 10L, exactExam, sampleQuestions, answers, false);

        assertEquals(5, result.earnedMarks());
        assertEquals(50.0, result.percentage());
        assertTrue(result.passed(), "5 marks should pass with passing score of 5");
    }

    @Test
    @DisplayName("Invalid inputs throw IllegalArgumentException")
    void testInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () ->
                evaluationService.evaluate(1L, 1L, null, sampleQuestions, Map.of(), false));

        assertThrows(IllegalArgumentException.class, () ->
                evaluationService.evaluate(1L, 1L, sampleExam, null, Map.of(), false));

        assertThrows(IllegalArgumentException.class, () ->
                evaluationService.evaluate(1L, 1L, sampleExam, List.of(), Map.of(), false));
    }
}
