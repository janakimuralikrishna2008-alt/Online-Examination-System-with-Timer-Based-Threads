package com.onlineexam.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Topic} entity model.
 */
public class TopicModelTest {

    @Test
    @DisplayName("Topic creation and validation with valid properties")
    void testValidTopic() {
        Topic t = new Topic(1L, "Linear Algebra", "Matrices and Vectors", 1, true);
        assertDoesNotThrow(t::validate);
        assertEquals(1L, t.getSubjectId());
        assertEquals("Linear Algebra", t.getName());
        assertEquals(1, t.getDisplayOrder());
        assertTrue(t.isPublished());
    }

    @Test
    @DisplayName("Topic validation throws on missing subject or name")
    void testInvalidTopic() {
        Topic noSubject = new Topic(null, "Vectors", "Desc", 1, true);
        assertThrows(IllegalArgumentException.class, noSubject::validate);

        Topic zeroSubject = new Topic(0L, "Vectors", "Desc", 1, true);
        assertThrows(IllegalArgumentException.class, zeroSubject::validate);

        Topic emptyName = new Topic(1L, "", "Desc", 1, true);
        assertThrows(IllegalArgumentException.class, emptyName::validate);

        Topic negativeOrder = new Topic(1L, "Vectors", "Desc", -1, true);
        assertThrows(IllegalArgumentException.class, negativeOrder::validate);
    }
}
