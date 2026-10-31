package com.onlineexam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Exam} entity and validation rules.
 */
public class ExamModelTest {

    @Test
    @DisplayName("Valid exam entity passes validation")
    void testValidExam() {
        Exam exam = new Exam("Java Core", "Introductory exam", 30, 50, 20, true);
        assertDoesNotThrow(exam::validate);
        assertEquals(1800, exam.getDurationSeconds());
        assertTrue(exam.isActive());
    }

    @Test
    @DisplayName("Blank exam title throws IllegalArgumentException")
    void testBlankTitle() {
        Exam exam = new Exam("   ", "Description", 15, 100, 40, true);
        Exception ex = assertThrows(IllegalArgumentException.class, exam::validate);
        assertTrue(ex.getMessage().contains("Exam title cannot be empty"));
    }

    @Test
    @DisplayName("Zero or negative duration throws IllegalArgumentException")
    void testInvalidDuration() {
        Exam exam = new Exam("Title", "Description", 0, 100, 40, true);
        Exception ex = assertThrows(IllegalArgumentException.class, exam::validate);
        assertTrue(ex.getMessage().contains("Duration must be greater than 0"));
    }

    @Test
    @DisplayName("Passing marks exceeding total marks throws IllegalArgumentException")
    void testPassingMarksExceedTotal() {
        Exam exam = new Exam("Title", "Description", 15, 50, 60, true);
        Exception ex = assertThrows(IllegalArgumentException.class, exam::validate);
        assertTrue(ex.getMessage().contains("Passing marks must be between 0 and total marks"));
    }
}
