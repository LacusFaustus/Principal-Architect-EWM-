package ru.practicum.user.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testEqualsAndHashCode() {
        User user1 = new User();
        user1.setId(1L);
        user1.setName("Test User");
        user1.setEmail("test@example.com");

        User user2 = new User();
        user2.setId(1L);
        user2.setName("Different Name");
        user2.setEmail("different@example.com");

        User user3 = new User();
        user3.setId(2L);
        user3.setName("Test User");
        user3.setEmail("test@example.com");

        assertEquals(user1, user2);
        assertNotEquals(user1, user3);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

    @Test
    void testToString() {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        String toString = user.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("Test User"));
        assertTrue(toString.contains("test@example.com"));
    }
}