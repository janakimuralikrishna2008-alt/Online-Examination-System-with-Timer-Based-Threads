package com.onlineexam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StudentExam} session lifecycle and timer countdown arithmetic.
 */
public class StudentExamModelTest {

    @Test
    @DisplayName("Countdown decrement reduces time remaining and auto-submits at zero")
    void testCountdownDecrementAndAutoSubmit() {
        StudentExam session = new StudentExam(10L, 20L, 10);
        session.setStatus(StudentExam.ExamStatus.IN_PROGRESS);

        session.decrementTime(4);
        assertEquals(6, session.getTimeRemainingSeconds());
        assertTrue(session.isInProgress());

        session.decrementTime(10); // Decrement more than remaining
        assertEquals(0, session.getTimeRemainingSeconds());
        assertEquals(StudentExam.ExamStatus.AUTO_SUBMITTED, session.getStatus());
        assertTrue(session.isCompleted());
    }

    @Test
    @DisplayName("Time formatting generates correct MM:SS and HH:MM:SS strings")
    void testFormattedTimeRemaining() {
        StudentExam shortSession = new StudentExam(1L, 1L, 125); // 2m 5s
        assertEquals("02:05", shortSession.getFormattedTimeRemaining());

        StudentExam longSession = new StudentExam(1L, 1L, 3665); // 1h 1m 5s
        assertEquals("01:01:05", longSession.getFormattedTimeRemaining());
    }

    @Test
    @DisplayName("Status parsing handles case insensitivity and fallback")
    void testStatusParsing() {
        assertEquals(StudentExam.ExamStatus.IN_PROGRESS, StudentExam.ExamStatus.fromString("in_progress"));
        assertEquals(StudentExam.ExamStatus.SUBMITTED, StudentExam.ExamStatus.fromString("SUBMITTED"));
        assertEquals(StudentExam.ExamStatus.NOT_STARTED, StudentExam.ExamStatus.fromString("UNKNOWN"));
    }
}
