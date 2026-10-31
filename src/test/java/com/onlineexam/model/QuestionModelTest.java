package com.onlineexam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Question} entity and option lookup logic.
 */
public class QuestionModelTest {

    @Test
    @DisplayName("Valid multiple choice question passes validation")
    void testValidQuestion() {
        Question q = new Question(1L, "What is JDBC?", "Database Driver", "Web Framework", "Build Tool", "IDE",
                Question.Option.A, 2, "JDBC connects Java to databases.");
        assertDoesNotThrow(q::validate);
        assertEquals("Database Driver", q.getOptionText(Question.Option.A));
        assertTrue(q.isCorrect(Question.Option.A));
        assertFalse(q.isCorrect(Question.Option.B));
    }

    @Test
    @DisplayName("Missing question text throws IllegalArgumentException")
    void testBlankQuestionText() {
        Question q = new Question(1L, "  ", "A", "B", "C", "D", Question.Option.A, 1, null);
        Exception ex = assertThrows(IllegalArgumentException.class, q::validate);
        assertTrue(ex.getMessage().contains("Question text cannot be empty"));
    }

    @Test
    @DisplayName("Missing one option throws IllegalArgumentException")
    void testMissingOption() {
        Question q = new Question(1L, "Question?", "A", "", "C", "D", Question.Option.A, 1, null);
        Exception ex = assertThrows(IllegalArgumentException.class, q::validate);
        assertTrue(ex.getMessage().contains("All four answer options"));
    }

    @Test
    @DisplayName("Option.fromString parses valid options and returns null for invalid")
    void testOptionParsing() {
        assertEquals(Question.Option.A, Question.Option.fromString("a"));
        assertEquals(Question.Option.B, Question.Option.fromString("B"));
        assertNull(Question.Option.fromString("E"));
        assertNull(Question.Option.fromString(null));
    }
}
