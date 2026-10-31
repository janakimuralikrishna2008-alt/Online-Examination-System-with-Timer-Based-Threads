package com.onlineexam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Subject} entity model.
 */
public class SubjectModelTest {

    @Test
    @DisplayName("Subject creation and validation with valid properties")
    void testValidSubject() {
        Subject s = new Subject("Mathematics", "Calculus and Algebra", true);
        assertDoesNotThrow(s::validate);
        assertEquals("Mathematics", s.getName());
        assertEquals("Calculus and Algebra", s.getDescription());
        assertTrue(s.isPublished());
    }

    @Test
    @DisplayName("Subject validation throws on empty or whitespace name")
    void testInvalidSubjectName() {
        Subject empty = new Subject("", "Desc", true);
        assertThrows(IllegalArgumentException.class, empty::validate);

        Subject nullName = new Subject(null, "Desc", true);
        assertThrows(IllegalArgumentException.class, nullName::validate);

        Subject blank = new Subject("   ", "Desc", true);
        assertThrows(IllegalArgumentException.class, blank::validate);
    }

    @Test
    @DisplayName("Subject equals and hashCode based on ID")
    void testSubjectEqualsAndHashCode() {
        Subject s1 = new Subject(1L, "Physics", "Mechanics", true, null, null, null);
        Subject s2 = new Subject(1L, "Physics II", "Modern Physics", false, null, null, null);
        Subject s3 = new Subject(2L, "Chemistry", "Organic", true, null, null, null);

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotEquals(s1, s3);
    }
}
