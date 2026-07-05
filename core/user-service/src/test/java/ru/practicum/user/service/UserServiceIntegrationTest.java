package ru.practicum.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.saga.SagaOrchestrator;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.exception.ConflictException;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        topics = {"stats.user-actions.v1", "stats.events-similarity.v1"},
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private SagaOrchestrator sagaOrchestrator;

    @MockBean
    private AuthServiceClient authServiceClient;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_ShouldSucceed() {
        // Arrange
        NewUserRequest request = new NewUserRequest();
        request.setEmail("test@example.com");
        request.setName("Test User");

        doNothing().when(authServiceClient).createUserCredentials(anyLong(), any(), any());
        when(sagaOrchestrator.startSaga(any(), any())).thenReturn("saga-id");
        doNothing().when(sagaOrchestrator).completeSaga(any());

        // Act
        UserDto result = userService.createUser(request);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test User", result.getName());

        // Проверяем, что пользователь сохранен в БД
        User savedUser = userRepository.findById(result.getId()).orElse(null);
        assertNotNull(savedUser);
        assertEquals("test@example.com", savedUser.getEmail());
    }

    @Test
    void createUser_DuplicateEmail_ShouldThrowException() {
        // Arrange
        NewUserRequest request = new NewUserRequest();
        request.setEmail("test@example.com");
        request.setName("Test User");

        User existingUser = new User();
        existingUser.setEmail("test@example.com");
        existingUser.setName("Existing User");
        userRepository.save(existingUser);

        // Act & Assert
        assertThrows(ConflictException.class, () -> userService.createUser(request));
    }

    @Test
    void getUserById_ShouldReturnUser() {
        // Arrange
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        User saved = userRepository.save(user);

        // Act
        UserDto result = userService.getUserById(saved.getId());

        // Assert
        assertNotNull(result);
        assertEquals(saved.getId(), result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test User", result.getName());
    }

    @Test
    void deleteUser_ShouldDelete() {
        // Arrange
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        User saved = userRepository.save(user);

        doNothing().when(authServiceClient).deleteUserCredentials(saved.getId());

        // Act
        userService.deleteUser(saved.getId());

        // Assert
        assertFalse(userRepository.findById(saved.getId()).isPresent());
    }
}