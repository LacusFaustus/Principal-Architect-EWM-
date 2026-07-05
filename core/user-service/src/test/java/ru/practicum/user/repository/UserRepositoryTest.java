package ru.practicum.user.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.user.model.User;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void save_ShouldSaveUser() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("Test User", saved.getName());
        assertEquals("test@example.com", saved.getEmail());
    }

    @Test
    void findById_ShouldReturnUser() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        User saved = userRepository.save(user);

        Optional<User> found = userRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("Test User", found.get().getName());
    }

    @Test
    void findByEmail_ShouldReturnUser() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals("Test User", found.get().getName());
    }

    @Test
    void existsById_ShouldReturnTrue() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        User saved = userRepository.save(user);

        boolean exists = userRepository.existsById(saved.getId());

        assertTrue(exists);
    }

    @Test
    void findAll_ShouldReturnPage() {
        User user1 = new User();
        user1.setName("User 1");
        user1.setEmail("user1@example.com");
        userRepository.save(user1);

        User user2 = new User();
        user2.setName("User 2");
        user2.setEmail("user2@example.com");
        userRepository.save(user2);

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = userRepository.findAll(pageable);

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findByIdIn_ShouldReturnUsers() {
        User user1 = new User();
        user1.setName("User 1");
        user1.setEmail("user1@example.com");
        User saved1 = userRepository.save(user1);

        User user2 = new User();
        user2.setName("User 2");
        user2.setEmail("user2@example.com");
        User saved2 = userRepository.save(user2);

        List<Long> ids = List.of(saved1.getId(), saved2.getId());
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = userRepository.findByIdIn(ids, pageable);

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void deleteById_ShouldDelete() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        User saved = userRepository.save(user);

        userRepository.deleteById(saved.getId());

        Optional<User> found = userRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }
}